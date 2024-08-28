/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.action;

import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping.AUTO_DELETE;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping.HOUSE_KEEPING;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerConstants.AUTO_EXPORT_URI;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerConstants.SCHEDULED_BACKUP_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerConstants.SCHEDULER;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventConstants.DAYS;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventConstants.EVENT_ID;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventConstants.HOURS;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventConstants.MINUTES;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventConstants.PERIODIC_EVENT;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventConstants.START_TIME;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventConstants.STOP_TIME;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventConstants.WEEKS;
import static com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation.ADD;
import static com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation.REMOVE;
import static com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation.REPLACE;
import static java.util.stream.Collectors.groupingBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.ScheduledEventHandler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.EtagNotifIdBase;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMHousekeepingJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.MediatorRequest;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.MediatorRequestPatch;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.UpdateHousekeepingRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.PeriodicEventRequestOrResponse;
import com.ericsson.adp.mgmt.backupandrestore.util.ESAPIValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;

/**
 * Handles requests from CM Mediator
 */
@Service
@SuppressWarnings({"PMD.CyclomaticComplexity"})
public class MediatorNotificationHandler {

    private static final Logger log = LogManager.getLogger(MediatorNotificationHandler.class);

    private BackupManagerRepository backupManagerRepository;
    private ScheduledEventHandler eventHandler;
    private ActionService actionService;

    private SftpServerNotificationPatchHandler sftpServerPatchHandler;
    private ESAPIValidator esapiValidator;
    private IdValidator idValidator;
    private EtagNotifIdBase etagNotifIdBase;
    private CMMediatorService cmMediatorService;

    /**
     * Validates the notification from Mediator
     * @param request Mediator request
     */
    public void handleMediatorRequest(final MediatorRequest request) {
        if (hasChangedBy(request) && isValidETag(request)) { // If not changedBy YP or invalid it's ignored
            // Updates the last Etag and process this notification
            updateEtagAndNotifId(request);
            updateHousekeeping(getHousekeepingPatches(request));
            updateScheduler(getSchedulerPatches(request));
            addPeriodicEvent(getPeriodicEventPatches(request, ADD.getStringRepresentation()));
            removePeriodicEvent(getPeriodicEventPatches(request, REMOVE.getStringRepresentation()));
            updatePeriodicEventId(getUpdatePeriodicEventIdPatches(request));
            updatePeriodicEvent(getUpdatePeriodicEventPatches(request));
            sftpServerPatchHandler.addSftpServerPatches(sftpServerPatchHandler.getAddSftpServersPatches(request));
            sftpServerPatchHandler.removeSftpServerPatches(sftpServerPatchHandler.getRemoveSftpServersPatches(request));
            sftpServerPatchHandler.updateSftpServerNamePatches(sftpServerPatchHandler.getUpdateSftpServerNamesPatches(request));
            sftpServerPatchHandler.updateSftpServerPatches(sftpServerPatchHandler.getUpdateSftpServersPatches(request));
        } else {
            // try to get the latest version from Mediator on invalid request
            cmMediatorService.updateEtagfromCMM();
        }
    }

    private void updateHousekeeping(final Map<Integer, List<MediatorRequestPatch>> housekeepingPatches) {
        if (!housekeepingPatches.isEmpty()) {
            log.info("Received a notification from CM. Updating the housekeeping configuration with the new information {} ", housekeepingPatches);
            executeHouseKeepingFromMediator(housekeepingPatches);
        }
    }

    private void updateScheduler(final Map<Integer, List<MediatorRequestPatch>> schedulerPatches) {
        if (!schedulerPatches.isEmpty()) {
            log.info("Received a notification from CM. Updating the scheduler configuration with the new information {} ", schedulerPatches.values());
            for (final List<MediatorRequestPatch> schedulerPatch:schedulerPatches.values()) {
                updateSchedulerFromMediator(schedulerPatch);
            }
        }
    }

    private void addPeriodicEvent(final List<MediatorRequestPatch> addEventPatches) {
        if (!addEventPatches.isEmpty()) {
            log.info("Received a notification from CM. Adding a periodic event {} ", addEventPatches);
            addPeriodicEventFromMediator(addEventPatches);
        }
    }

    private void updatePeriodicEventId(final Map<Integer, List<MediatorRequestPatch>> updateEventIdPatches) {
        if (!updateEventIdPatches.isEmpty()) {
            log.info("Received a notification from CM. Updating the periodic event ID with the new information {} ", updateEventIdPatches.values());
            for (final List<MediatorRequestPatch> updateEventIdPatch:updateEventIdPatches.values()) {
                updatePeriodicEventIdFromMediator(updateEventIdPatch);
            }
        }
    }

    private void updatePeriodicEvent(final Map<Integer, List<MediatorRequestPatch>> updateEventPatches) {
        if (!updateEventPatches.isEmpty()) {
            log.info("Received a notification from CM. Updating the periodic event with the new information {} ", updateEventPatches.values());
            for (final List<MediatorRequestPatch> updateEventPatch:updateEventPatches.values()) {
                updatePeriodicEventFromMediator(updateEventPatch);
            }
        }
    }

    private void removePeriodicEvent(final List<MediatorRequestPatch> removeEventPatches) {
        if (!removeEventPatches.isEmpty()) {
            log.info("Received a notification from CM. Deleting the periodic event {} ", removeEventPatches);
            removePeriodicEventFromMediator(removeEventPatches);
        }
    }

    private void executeHouseKeepingFromMediator(final Map<Integer, List<MediatorRequestPatch>>  patchesPerBRM) {
        patchesPerBRM.forEach((k, v) -> {
            final BackupManager backupManager = backupManagerRepository.getBackupManager(k);
            final BRMHousekeepingJson housekeepingJson = getBRMHousekeepingJson(backupManager, v);
            actionService.executeAndWait(backupManager, getHousekeepingRequest(
                    housekeepingJson.getAutoDelete(), housekeepingJson.getMaxNumberBackups()));
        });
    }

    private void updateSchedulerFromMediator(final List<MediatorRequestPatch> patches) {
        final BackupManager backupManager = backupManagerRepository.getBackupManager(patches.get(0).getBackupManagerIndex());
        if (isValidSchedulerPatch(patches)) {
            updateScheduler(backupManager, patches);
        } else {
            final Scheduler scheduler = backupManager.getScheduler();
            scheduler.persist();
        }
    }

    private boolean isValidSchedulerPatch(final List<MediatorRequestPatch> patches) {
        boolean isBackupValid = true;
        boolean isURIValid = true;
        for (final MediatorRequestPatch requestPatch:patches) {
            final String updatedElement = requestPatch.getUpdatedElement();
            if (updatedElement.equalsIgnoreCase(SCHEDULED_BACKUP_NAME.toString())) {
                isBackupValid = validateBackupName(requestPatch);
            } else if (!requestPatch.getOp().equals(REMOVE.getStringRepresentation()) &&
                    updatedElement.equalsIgnoreCase(AUTO_EXPORT_URI.toString())) {
                isURIValid = validateAutoExportURI(requestPatch);
            }
        }
        return isBackupValid && isURIValid;
    }

    private boolean validateBackupName(final MediatorRequestPatch requestPatch) {
        boolean isBackupValid = true;
        final String backup = requestPatch.getValue().toString();
        isBackupValid = idValidator.isValidId(backup) && esapiValidator.isValidBackupName(backup);
        if (!isBackupValid) {
            log.warn("Invalid update - invalid backup name");
        }
        return isBackupValid;
    }

    private boolean validateAutoExportURI(final MediatorRequestPatch requestPatch) {
        boolean isURIValid = true;
        final String uri = requestPatch.getValue().toString();
        if (uri == null || uri.isEmpty()) {
            isURIValid = false;
            log.warn("Invalid update - No URI was provided");
        } else {
            isURIValid = esapiValidator.isValidURI(uri);
            if (!isURIValid) {
                log.warn("Invalid update - the URI format is invalid");
            }
        }
        return isURIValid;
    }

    private void updateScheduler(final BackupManager backupManager, final List<MediatorRequestPatch> patches) {
        final Scheduler scheduler = backupManager.getScheduler();
        for (final MediatorRequestPatch requestPatch:patches) {
            if (requestPatch.getOp().equals(REMOVE.getStringRepresentation())) {
                scheduler.clearProperty(requestPatch.getUpdatedElement());
            } else {
                scheduler.updateProperty(requestPatch.getUpdatedElement(), String.valueOf(requestPatch.getValue()));
            }
        }
        scheduler.updateNextScheduledTime();
        scheduler.persist();
    }

    private void updatePeriodicEventIdFromMediator(final List<MediatorRequestPatch> updateEventIdPatches) {
        final BackupManager backupManager = backupManagerRepository.getBackupManager(updateEventIdPatches.get(0).getBackupManagerIndex());
        final Map<Integer, List<MediatorRequestPatch>> patches = updateEventIdPatches
                .stream()
                .collect(groupingBy(MediatorRequestPatch::getPeriodicEventIndex));
        final Map<Integer, List<MediatorRequestPatch>> sortedMap = new TreeMap<>((a, b) -> Integer.compare(b, a));
        sortedMap.putAll(patches);

        sortedMap.forEach((periodicEventIndex, patch) -> {
            final PeriodicEvent event = backupManager.getScheduler().getPeriodicEvent(periodicEventIndex);
            eventHandler.updatePeriodicEventId(backupManager, event, patch.get(0).getValue().toString());
        });
    }

    private void updatePeriodicEventFromMediator(final List<MediatorRequestPatch> updateEventPatches) {
        final BackupManager backupManager = backupManagerRepository.getBackupManager(updateEventPatches.get(0).getBackupManagerIndex());
        final Map<Integer, List<MediatorRequestPatch>> patches = updateEventPatches
                .stream()
                .collect(groupingBy(MediatorRequestPatch::getPeriodicEventIndex));

        patches.forEach((periodicEventIndex, patch) -> {
            final PeriodicEventRequestOrResponse eventUpdateRequest = getCreatePeriodicEventRequest(patch);
            final PeriodicEvent event = backupManager.getScheduler().getPeriodicEvent(periodicEventIndex);
            eventHandler.updatePeriodicEvent(backupManager, event.getEventId(), eventUpdateRequest);
        });
    }

    private void addPeriodicEventFromMediator(final List<MediatorRequestPatch> addEventPatches) {
        for (final MediatorRequestPatch patch:addEventPatches) {
            final BackupManager backupManager = backupManagerRepository.getBackupManager(patch.getBackupManagerIndex());
            // Parse value
            final String value = String.valueOf(patch.getValue());
            // if this is an array, we need to prcess each element
            if (value.startsWith("[")) {
                final Pattern pattern = Pattern.compile("\\{[^}]*\\}");
                final Matcher matcher = pattern.matcher(value);
                while (matcher.find()) {
                    eventhandlerCreatePeriodicEvent (backupManager, matcher.group());
                }
            } else {
                eventhandlerCreatePeriodicEvent (backupManager,  value);
            }
        }
    }

    private void eventhandlerCreatePeriodicEvent(final BackupManager backupManager, final String value) {
        final String[] parts = value.substring(1, value.length() - 1).split(",");
        final Map<String, String> valueMap = new HashMap<>();
        for (final String part : parts) {
            final String[] empdata = part.split("=");
            final String field = empdata[0].trim();
            final String value1 = empdata[1].trim();

            valueMap.put(field, value1);
        }

        final PeriodicEventRequestOrResponse request = getCreatePeriodicEventRequest(valueMap);
        eventHandler.createPeriodicEvent(backupManager, request, false);
    }

    private void removePeriodicEventFromMediator(final List<MediatorRequestPatch> removeEventPatches) {
        for (final MediatorRequestPatch patch:removeEventPatches) {
            final BackupManager backupManager = backupManagerRepository.getBackupManager(patch.getBackupManagerIndex());
            if (patch.getPeriodicEventIndex() == -1) {
                backupManager.getScheduler().getPeriodicEvents().stream()
                    .forEach(event -> eventHandler.deletePeriodicEvent(backupManager, event, true));
            } else {
                eventHandler.deletePeriodicEvent(backupManager, backupManager.getScheduler().getPeriodicEvent(patch.getPeriodicEventIndex()), false);
            }
        }
    }

    private PeriodicEventRequestOrResponse getCreatePeriodicEventRequest(final Map<String, String> addEventValue) {
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setEventId(addEventValue.get(EVENT_ID.toString()));
        request.setHours(Integer.valueOf(addEventValue.get(HOURS.toString())));
        request.setMinutes(Integer.valueOf(addEventValue.get(MINUTES.toString())));
        request.setWeeks(Integer.valueOf(addEventValue.get(WEEKS.toString())));
        request.setDays(Integer.valueOf(addEventValue.get(DAYS.toString())));
        if (addEventValue.containsKey(START_TIME.toString())) {
            request.setStartTime(addEventValue.get(START_TIME.toString()));
        }
        if (addEventValue.containsKey(STOP_TIME.toString())) {
            request.setStopTime(addEventValue.get(STOP_TIME.toString()));
        }
        return request;
    }

    private PeriodicEventRequestOrResponse getCreatePeriodicEventRequest(final List<MediatorRequestPatch> patches) {
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        for (final MediatorRequestPatch requestPatch:patches) {
            if (requestPatch.getUpdatedElement().equalsIgnoreCase(HOURS.toString())) {
                request.setHours((Integer) requestPatch.getValue());
            } else if (requestPatch.getUpdatedElement().equalsIgnoreCase(MINUTES.toString())) {
                request.setMinutes((Integer) requestPatch.getValue());
            } else if (requestPatch.getUpdatedElement().equalsIgnoreCase(DAYS.toString())) {
                request.setDays((Integer) requestPatch.getValue());
            } else if (requestPatch.getUpdatedElement().equalsIgnoreCase(WEEKS.toString())) {
                request.setWeeks((Integer) requestPatch.getValue());
            } else if (requestPatch.getUpdatedElement().equalsIgnoreCase(START_TIME.toString())) {
                request.setStartTime(getValue(requestPatch));
            } else if (requestPatch.getUpdatedElement().equalsIgnoreCase(STOP_TIME.toString())) {
                request.setStopTime(getValue(requestPatch));
            } else if (requestPatch.getUpdatedElement().equalsIgnoreCase(EVENT_ID.toString())) {
                request.setEventId(getValue(requestPatch));
            }
        }
        return request;
    }

    private String getValue(final MediatorRequestPatch requestPatch) {
        return requestPatch.getValue() == null ? "" : String.valueOf(requestPatch.getValue());
    }

    private BRMHousekeepingJson getBRMHousekeepingJson(final BackupManager backupManager, final List<MediatorRequestPatch> patches) {
        final Housekeeping housekeeping = new Housekeeping(backupManager.getHousekeeping());

        for (final MediatorRequestPatch requestPatch:patches) {
            if (requestPatch.getUpdatedElement().equalsIgnoreCase(AUTO_DELETE.toString())) {
                housekeeping.setAutoDelete(String.valueOf(requestPatch.getValue()));
            } else {
                housekeeping.setMaxNumberBackups( (Integer) requestPatch.getValue());
            }
        }
        return new BRMHousekeepingJson(housekeeping);
    }

    private UpdateHousekeepingRequest getHousekeepingRequest(final String autoDeleteEnabled,
                                                             final int maximumManualBackupsNumberStore ) {
        final  UpdateHousekeepingRequest housekeepingRequest = new UpdateHousekeepingRequest();
        housekeepingRequest.setAction(ActionType.HOUSEKEEPING);
        housekeepingRequest.setAutoDelete(autoDeleteEnabled);
        housekeepingRequest.setMaximumManualBackupsNumberStored(maximumManualBackupsNumberStore);
        housekeepingRequest.setExecuteAstask(false);
        return housekeepingRequest;
    }

    private Map<Integer, List<MediatorRequestPatch>> getHousekeepingPatches(final MediatorRequest request) {
        return request.getPatch()
                .stream()
                .filter(patch -> patch.getPath().toLowerCase().contains(HOUSE_KEEPING.toString()))
                .filter(patch -> patch.getOp().toLowerCase().contains(REPLACE.getStringRepresentation()))
                .collect(groupingBy(MediatorRequestPatch::getBackupManagerIndex));
    }

    private Map<Integer, List<MediatorRequestPatch>> getSchedulerPatches(final MediatorRequest request) {
        return request.getPatch()
                .stream()
                .filter(patch -> patch.getPath().toLowerCase().contains(SCHEDULER.toString()))
                .filter(patch -> !patch.getPath().toLowerCase().contains(PERIODIC_EVENT.toString()))
                .filter(patch -> patch.getOp().toLowerCase().contains(REPLACE.getStringRepresentation()) ||
                        patch.getOp().toLowerCase().contains(ADD.getStringRepresentation()) ||
                        patch.getOp().toLowerCase().contains(REMOVE.getStringRepresentation()))
                .collect(groupingBy(MediatorRequestPatch::getBackupManagerIndex));
    }

    private Map<Integer, List<MediatorRequestPatch>> getUpdatePeriodicEventIdPatches(final MediatorRequest request) {
        return request.getPatch()
                .stream()
                .filter(patch -> patch.getPath().toLowerCase().matches(MediatorRequestPatch.PERIODIC_EVENT_PATH_REGEX_ID))
                .filter(patch -> patch.getOp().toLowerCase().contains(REPLACE.getStringRepresentation()))
                .collect(groupingBy(MediatorRequestPatch::getBackupManagerIndex));
    }

    private Map<Integer, List<MediatorRequestPatch>> getUpdatePeriodicEventPatches(final MediatorRequest request) {
        return request.getPatch()
                .stream()
                .filter(patch -> patch.getPath().toLowerCase().matches(MediatorRequestPatch.PERIODIC_EVENT_PATH_REGEX_SKIP_ONLY_ID))
                .filter(patch -> !patch.getUpdatedElement().toLowerCase().contains(PERIODIC_EVENT.toString()))
                .collect(groupingBy(MediatorRequestPatch::getBackupManagerIndex));
    }

    private List<MediatorRequestPatch> getPeriodicEventPatches(final MediatorRequest request, final String operation) {
        return request.getPatch()
                .stream()
                .filter(patch -> patch.getPath().toLowerCase().contains(PERIODIC_EVENT.toString()))
                .filter(patch -> patch.getOp().toLowerCase().contains(operation))
                .filter(patch -> patch.getUpdatedElement().toLowerCase().contains(PERIODIC_EVENT.toString()))
                .collect(Collectors.toList());
    }

    private boolean isValidETag(final MediatorRequest request) {
        final Integer notifId = request.getNotifId();
        final String etagbaseString = request.getBaseETag();
        boolean isValid = etagNotifIdBase.isValidMediatorRequest(etagbaseString, notifId);
        if (!isValid) {
            // BRO Updates from CMM and double check the values
            cmMediatorService.updateEtagfromCMM();
            isValid = etagNotifIdBase.isValidMediatorRequest(etagbaseString, notifId);
            if (!isValid) {
                log.warn("Potential slow update in CM Mediator: Validating using configEtag:{}",
                        request.getConfigETag());
                return etagNotifIdBase.isValidMediatorRequest(request.getConfigETag(), notifId);
            }
        }
        return isValid;
    }

    private void updateEtagAndNotifId (final MediatorRequest request) {
        // change the local ETag to the configETag value in the notification
        log.info("Updating {} with baseEtag: {} notifId: {}", etagNotifIdBase, request.getConfigETag(), request.getNotifId());
        etagNotifIdBase.updateEtag(request.getConfigETag());
        etagNotifIdBase.setNotifId(request.getNotifId());
    }

    private boolean hasChangedBy(final MediatorRequest request) {
        return request.getChangedBy() != null;
    }

    @Autowired
    public void setSftpServerPatchHandler(final SftpServerNotificationPatchHandler sftpServerPatchHandler) {
        this.sftpServerPatchHandler = sftpServerPatchHandler;
    }

    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

    @Autowired
    public void setEventHandler(final ScheduledEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Autowired
    public void setActionService(final ActionService actionService) {
        this.actionService = actionService;
    }

    @Autowired
    public void setEsapiValidator(final ESAPIValidator esapiValidator) {
        this.esapiValidator = esapiValidator;
    }

    @Autowired
    public void setIdValidator(final IdValidator idValidator) {
        this.idValidator = idValidator;
    }

    @Autowired
    public void setEtagNotifIdBase(final EtagNotifIdBase etagNotifIdBase) {
        this.etagNotifIdBase = etagNotifIdBase;
    }

    @Autowired
    public void setCmMediatorService(final CMMediatorService cmMediatorService) {
        this.cmMediatorService = cmMediatorService;
    }
}
