/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.backupandrestore.cminterface;

import static com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMClient.RETRY_INDEFINITELY;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.CONFIGURATION_BRO_RESOURCE;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.CONFIGURATION_RESOURCE;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.getBrmConfigurationResource;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMBackupManagerJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMEricssonbrmJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMProgressReportJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.ProgressReportPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.ProgressReportPatchFactory;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.UpdateProgressReportPatch;
import com.ericsson.adp.mgmt.backupandrestore.exception.CMMediatorException;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;

/**
 * Configuration Management Mediator Service.
 */
@Service
@SuppressWarnings({"PMD.CyclomaticComplexity"})
public class CMMediatorService extends CMService{

    private static final double HUNDRED_PERCENT = 1.0;
    private static final Logger log = LogManager.getLogger(CMMediatorService.class);

    private BRMConfigurationUtil brmConfigurationUtil;
    private BackupManagerRepository backupManagerRepository;
    private ProgressReportPatchFactory progressReportPatchFactory;
    private KeyStoreService keyStoreService;

    private final AtomicReference<Optional<Action>> lastEnqueuedBRMProgressReport = new AtomicReference<>(Optional.empty());
    private final AtomicReference<Optional<Action>> lastEnqueuedBackupProgressReport = new AtomicReference<>(Optional.empty());
    private final AtomicBoolean isBackupManagerBaseCreated =  new AtomicBoolean();

    private final BiFunction<CMMMessage, Exception, Optional<CMMMessage>> remedy = (CMMMessage m, Exception e) -> {
        keyStoreService.regenerateKeyStoreForAlias(KeyStoreAlias.CMM_REST);
        getCMMClient().processMessage((CMMMessage) m);
        return Optional.of(m);
    };

    private final ExecutorService progressReportUpdateExecutor = Executors.newSingleThreadExecutor();

    /**
     * Constructs CM Mediator Service.
     * There is a dependency cycle between CMMediatorService and CMMMessageFactory, that is:
     *  CMMediatorService -> CMMMessageFactory -> BackupPatchFactory -> BackupManagerPatchFactory ->
     *  BackupManagerRepository -> BackupRepository -> CMMediatorService
     * and Spring does not allow this. To get around this issue, the @Lazy annotation
     * is used for the CMMMessageFactory. This will create a proxy for the CMMMessageFactory bean
     * when the CMMediatorService is instantiated. The actual CMMMessageFactory bean will
     * be fully created when it's first needed.
     * @param cmmClient CMMClient service
     * @param etagNotifIdBase common Etag and NotifId tracker
     * @param cmmMessageFactory Message factory Service
     */
    @Autowired
    public CMMediatorService(final CMMClient cmmClient,
                             final EtagNotifIdBase etagNotifIdBase,
            @Lazy final CMMMessageFactory cmmMessageFactory) {
        setCmmClient(cmmClient);
        setCMMMessageFactory(cmmMessageFactory);
        brmConfigurationUtil = new BRMConfigurationUtil(getCMMClient(), getCMMessageFactory(), etagNotifIdBase);
    }

    /**
     * Recreates BRM information in CM Mediator.
     * Retries reconnection if REST server is unavailable.
     * @param isStartup defines whether it is startup or not.
     */
    public void prepareCMMediator(final boolean isStartup) {
        isBackupManagerBaseCreated.set(false);
        if (isStartup) {
            executeIfCMIsEnabled(() -> {
                log.info("Preparing CM Mediator");
                pushSchemaAndConfiguration(true, isStartup);
            }, () -> {
                    throw new CMMediatorException("Failed to prepare CM for Orchestrator usage.");
                }
            );
        } else {
            executeIfCMIsEnabled(() -> {
                log.info("Preparing CM Mediator without startup");
                pushSchemaAndConfigurationIndividualBM(true, isStartup,
                        backupManagerRepository.getBackupManagers());
            }, () -> {
                    throw new CMMediatorException("Failed to prepare CM for Orchestrator usage.");
                }
            );

        }
    }

    /**
     * Initialize the CM Mediator services
     */
    public void initCMMediator() {
        waitUntilCMMisReady();
        executeIfCMIsEnabled(() -> {
            log.info("Initiating CM Mediator");
            pushSchemaAndConfiguration(true, true);
        }, (() -> pushSchemaAndConfiguration(true, true)));
    }

    private void waitUntilCMMisReady() {
        boolean toContinue = true;
        while (toContinue && getCMMClient().isFlagEnabled()) {
            try {
                getCMMClient().exists(CONFIGURATION_RESOURCE);
                toContinue = false;
            } catch (ResourceAccessException e) {
                log.info("Waiting for CM Mediator being up and running");
                sleep(1000, "Validate if CMM is ready interrupted");
            } catch (Exception e) {
                log.error("Error validating if CM Mediator is up and running");
                toContinue = false;
                throw e;
            }
        }
    }

    /**
     * Shuts down the progress report update execution thread
     */
    public void stopProcessingProgressReport() {
        progressReportUpdateExecutor.shutdownNow();
    }

    /**
     * Adds backupManager to CM.
     * @param backupManager to be added.
     */
    public void addBackupManager(final BackupManager backupManager) {
        executeIfCMIsEnabled(() -> {
            log.info("Adding backupManager {} to CM Mediator", backupManager.getBackupManagerId() );
            final CMMMessage message = getCMMessageFactory().getPatchToAddBackupManager(backupManager, RETRY_INDEFINITELY);
            if (isBRMBackupManagersEmpty() && !isBackupManagerBaseCreated.get()) {
                isBackupManagerBaseCreated.set(true);
                final HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json-patch+json");
                message.setHttpEntity(new HttpEntity<>(headers));
            }
            getCMMClient().processMessage(message);
        });
    }

    /**
     * Updates backupManager in CM.
     * @param backupManager to be updated.
     */
    public void updateBackupManager(final BackupManager backupManager) {
        executeIfCMIsEnabled(() -> {
            log.info("Updating backupManager in CM Mediator");
            final CMMMessage message = getCMMessageFactory().getMessageToUpdateBackupManager(backupManager,
                    remedy,
                    RETRY_INDEFINITELY);
            getCMMClient().processMessage(message);
        });
    }

    /**
     * Adds periodic event to CM.
     *
     * @param periodicEvent
     *            periodic event to be added
     */
    public void addPeriodicEvent(final PeriodicEvent periodicEvent) {
        executeIfCMIsEnabled(() -> {
            log.info("Adding periodic event to CM Mediator");
            final CMMMessage message = getCMMessageFactory().getMessageToAddPeriodicEvent(periodicEvent, RETRY_INDEFINITELY);
            getCMMClient().processMessage(message);
        });
    }

    /**
     * Updates periodic event in CM.
     *
     * @param periodicEvent
     *            periodic event to be updated
     */
    public void updatePeriodicEvent(final PeriodicEvent periodicEvent) {
        executeIfCMIsEnabled(() -> {
            log.info("Updating periodic event in CM Mediator");
            final CMMMessage message = getCMMessageFactory().getMessageToUpdatePeriodicEvent(periodicEvent,
                    remedy,
                    RETRY_INDEFINITELY);
            getCMMClient().processMessage(message);
        });
    }

    /**
     * Deletes periodic event in CM.
     *
     * @param periodicEvent
     *            periodic event to be deleted
     */
    public void deletePeriodicEvent(final PeriodicEvent periodicEvent) {
        executeIfCMIsEnabled(() -> {
            log.info("deleting periodic event in CM Mediator");
            final CMMMessage message = getCMMessageFactory().getMessageToDeletePeriodicEvent(periodicEvent, RETRY_INDEFINITELY);
            getCMMClient().processMessage(message);
        });
    }

    /**
     * Adds housekeeping to CM.
     *
     * @param housekeeping
     *            to be added
     */
    public void addHousekeeping(final Housekeeping housekeeping) {
        executeIfCMIsEnabled(() -> {
            log.info("Adding housekeeping config to CM Mediator");
            final CMMMessage message = getCMMessageFactory().getMessageToAddHousekeeping(housekeeping, RETRY_INDEFINITELY);
            getCMMClient().processMessage(message);
        });
    }

    /**
     * Updates housekeeping in CM.
     *
     * @param housekeeping
     *            to be added
     */
    public void updateHousekeeping(final Housekeeping housekeeping) {
        executeIfCMIsEnabled(() -> {
            log.info("Updating housekeeping in CM Mediator");
            final CMMMessage message = getCMMessageFactory().getMessageToUpdateHousekeeping(housekeeping,
                    remedy,
                    RETRY_INDEFINITELY);
            getCMMClient().processMessage(message);
        });
    }

    /**
     * Adds scheduler to CM.
     * @param scheduler to be added
     */
    public void addScheduler(final Scheduler scheduler) {
        executeIfCMIsEnabled(() -> {
            log.info("Adding scheduler config to CM Mediator");
            final CMMMessage message = getCMMessageFactory().getMessageToAddScheduler(scheduler, RETRY_INDEFINITELY);
            getCMMClient().processMessage(message);
        });
    }

    /**
     * Updates scheduler in CM.
     * @param scheduler to be added
     * TODO  Do we need to keep this retry pattern?
     */
    public void updateScheduler(final Scheduler scheduler) {
        if (getCMMClient().isReady()) {
            executeCMAndRetry(() -> {
                log.info("Updating scheduler config in CM Mediator");
                final CMMMessage message = getCMMessageFactory().getMessageToUpdateScheduler(scheduler,
                        remedy,
                        RETRY_INDEFINITELY);
                getCMMClient().processMessage(message);
            });
        }
    }

    /**
     * Adds backup to CM.
     * @param manager the manager to add the backup under
     * @param backup to be added.
     */
    public void addBackup(final BackupManager manager, final Backup backup) {
        executeIfCMIsEnabled(() -> {
            final CMMMessage message;
            log.info("Adding backup to CM Mediator - To Post: <{}>", isTheFirstbackup(manager, backup));
            if (isTheFirstbackup(manager, backup)) {
                message = getCMMessageFactory()
                        .getMessageToAddInitialBackup(manager, backup, RETRY_INDEFINITELY);
                final HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json-patch+json");
                message.setHttpEntity(new HttpEntity<>(headers));
                message.getConfigurationPatch();
            } else {
                message = getCMMessageFactory().getMessageToAddBackup(manager, backup, RETRY_INDEFINITELY);
            }
            getCMMClient().processMessage(message);
        });
    }

    /*
     * Validates if the repository is empty, or the first backup being created is the same that need to be added
     */
    private boolean isTheFirstbackup(final BackupManager brmManager, final Backup backupToAdd) {
        if (brmManager.getBackups(Ownership.READABLE).isEmpty()) {
            return true;
        }
        if (brmManager.getBackups(Ownership.READABLE).size() == 1) {
            final Optional<Backup> brmBackup = brmManager.getBackups(Ownership.READABLE).stream()
                    .filter(backup -> backup.getName()
                            .equals(backupToAdd.getName())).findFirst();
            if (brmBackup.isPresent()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates backup in CM.
     * @param manager the manager to update the backup under
     * @param backup to be updated.
     */
    public void updateBackup(final BackupManager manager, final Backup backup) {
        executeIfCMIsEnabled(() -> {
            log.info("Updating backup in CM Mediator");
            final CMMMessage message = getCMMessageFactory().getMessageToUpdateBackup(manager, backup,
                    remedy,
                    RETRY_INDEFINITELY);
            getCMMClient().processMessage(message);
        });
    }

    /**
     * Updates backup in CM.
     * @param manager the manager to update the backup under
     * @param backup to be updated.
     */
    public void updateBackupAndWait(final BackupManager manager, final Backup backup) {
        executeIfCMIsEnabled(() -> {
            log.info("Updating backup in CM Mediator");
            final CMMMessage message = getCMMessageFactory().getMessageToUpdateBackup(manager, backup,
                    remedy,
                    RETRY_INDEFINITELY);
            getCMMClient().processMessageAndWait(message);
        });
    }

    /**
     * Deletes a backup in CM.
     * @param backupManagerId owner of backup.
     * @param backupIndex index of backup to be deleted.
     */
    public void deleteBackup(final String backupManagerId, final int backupIndex) {
        executeIfCMIsEnabled(() -> {
            log.info("Deleting backup in CM Mediator");
            final CMMMessage message = getCMMessageFactory().getMessageToDeleteBackup(backupManagerId, backupIndex, RETRY_INDEFINITELY);
            getCMMClient().processMessage(message);
        });
    }

    /**
     * Deletes a backup in CM and wait to continue the operation
     * @param backupManagerId owner of backup.
     * @param backupIndex index of backup to be deleted.
     */
    public void deleteBackupAndWait(final String backupManagerId, final int backupIndex) {
        executeIfCMIsEnabled(() -> {
            log.info("Deleting backup in CM Mediator");
            final CMMMessage message = getCMMessageFactory().getMessageToDeleteBackup(backupManagerId, backupIndex, RETRY_INDEFINITELY);
            getCMMClient().processMessageAndWait(message);
        });
    }

    /**
     * Enqueues a progress report to progress report queue
     * that is internal to the single threaded executor.
     * @param action to be added.
     */
    public void enqueueProgressReport(final Action action) {
        final Runnable progressReportUpdateTask = () -> {
            try {
                final Action clone = action.getCopy().get();
                addProgressReport(clone);
                log.debug("Added action {} to the progress report queue.", clone);
            } catch (Exception e) {
                log.warn("Failed to update progress report in CM for action: <{}>", action, e);
            }
        };
        progressReportUpdateExecutor.submit(progressReportUpdateTask);
    }

    /**
     * Adds a progress report to CM.
     * @param action to be added.
     */
    public void addProgressReport(final Action action) {
        executeIfCMIsEnabled(() -> {
            final BackupManager manager = backupManagerRepository.getBackupManager(action.getBackupManagerId());
            // In the event this is a restore action on a vBRM, the action carries the vBRMs ID, so this progress report
            // will be pushed under the vBRM - as is desired. However, since we should only ever be running RESTORE actions
            // under a vBRM, we can use the ownership context here to assert that the manager does in fact own the backup
            // being operated on in other cases.
            final Ownership context = action.getName() == ActionType.RESTORE ? Ownership.READABLE : Ownership.OWNED;

            boolean emptyProgressReport = true;
            try {
                emptyProgressReport = isProgressReportNotExisting(action, manager, context);
            } catch (ResourceAccessException | HttpClientErrorException e) {
                log.warn("The empty progress report check failed as CMM is not available due to <{}>", e.getMessage());

                if (action.getProgressPercentage() != HUNDRED_PERCENT) {
                    log.warn("The progress report for action <{}> is not pushed to CM due to <{}>.",
                            action, e.getMessage());
                    return;
                }
                emptyProgressReport = indefinitelyCheckIfProgressReportIsNotExisting(action, manager, context);
            } catch (Exception e) {
                log.warn("The empty progress report check failed as CMM is not available due to {}", e.getMessage());
                if (action.getProgressPercentage() == HUNDRED_PERCENT) {
                    emptyProgressReport = indefinitelyCheckIfProgressReportIsNotExisting(action, manager, context);
                }
                throw e;
            }
            if (emptyProgressReport) {
                manager.backupManagerLevelProgressReportSetCreated();
                addProgressReport(action, manager, context);
            } else {
                updateProgressReport(action);
            }
        });
    }

    private void addProgressReport(final Action action, final BackupManager manager, final Ownership context) {
        log.debug("Adding progress report for action <{}:{}> to CM Mediator", action.getName(), action.getActionId());
        for (final ProgressReportPatch patch : progressReportPatchFactory.getPatchToAddProgressReport(action)) {
            brmConfigurationUtil.patch(patch);
        }
        if (action.isRestoreOrExport()) {
            manager.getBackup(action.getBackupName(), context).backupLevelProgressReportSetCreated();
        } else {
            manager.backupManagerLevelProgressReportSetCreated();
        }
    }

    private void updateProgressReport(final Action action) {
        if (isProgressReportAlreadyEnqueuedInCM(action)) {
            log.debug("Progress report update for action <{}> is already added to the CM queue. Adding a duplicate update is skipped.", action);
            return;
        }
        log.debug("Updating progress report for action <{}:{}> to CM Mediator", action.getName(), action.getActionId());
        for (final UpdateProgressReportPatch patch : progressReportPatchFactory.getPatchToUpdateProgressReport(action)) {
            brmConfigurationUtil.patch(patch);
        }
        setLastEnqueuedInCM(action);
    }

    private void setLastEnqueuedInCM(final Action action) {
        if (action.isRestoreOrExport()) {
            lastEnqueuedBackupProgressReport.set(Optional.of(action));
        } else {
            lastEnqueuedBRMProgressReport.set(Optional.of(action));
        }
    }

    private boolean isProgressReportAlreadyEnqueuedInCM(final Action action) {
        Optional<Action> lastEnqueued = Optional.empty();
        if (action.isRestoreOrExport()) {
            lastEnqueued = lastEnqueuedBackupProgressReport.get();
        } else {
            lastEnqueued = lastEnqueuedBRMProgressReport.get();
        }
        return lastEnqueued.isPresent() && (new BRMProgressReportJson(lastEnqueued.get())).equals(new BRMProgressReportJson(action));
    }

    private boolean indefinitelyCheckIfProgressReportIsNotExisting(final Action action, final BackupManager manager, final Ownership context) {
        int retryCount = 0;
        final int maxDelay = getCMMClient().getMaxDelay();
        while (true) {
            try {
                return isProgressReportNotExisting(action, manager, context);
            } catch (ResourceAccessException | HttpClientErrorException e) {
                retryCount++;
                log.warn("The validation for existing progrest report failed. Retrying in {} ms (attempt {})", maxDelay, retryCount);
                sleep(maxDelay, "The validation for existing progress report is interrupted.");
            } catch (Exception e) {
                throw e;
            }
        }
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity"})
    private boolean isProgressReportNotExisting(final Action action, final BackupManager manager, final Ownership context) {
        log.info("Checking progress report in CMM: {}", action.toString());
        final Optional<BRMBackupManagerJson> brmManager = getBRMBackupManagerJson(action);

        final Supplier<Boolean> isSchedulerEmpty = () ->
            Optional.ofNullable(brmManager.get().getScheduler())
            .map(scheduler -> Optional.ofNullable(scheduler.getProgressReports()))
            .orElse(Optional.empty()).isEmpty();
        final Supplier<Boolean> isManagerPRCreated = () -> !manager.isBackupManagerLevelProgressReportCreated();
        final Supplier<Boolean> isRestoreExport = () -> ! manager.getBackup(action.getBackupName(), context).
                isBackupLevelProgressReportCreated();

        if (brmManager.isEmpty()) {
            return true;
        }
        updateProgressReportFlags (manager, brmManager, context);
        if (action.isScheduledEvent()) {
            // Scheduler doesn't includes a flag to handle the progress-report in their definition,
            // so it will be using the brmConfigurationjson instead
            return validateIsRestoreOrExport(action, isRestoreExport, isSchedulerEmpty);
        }
        return validateIsRestoreOrExport(action, isRestoreExport, isManagerPRCreated);
    }

    private boolean validateIsRestoreOrExport(final Action action,
                                              final Supplier<Boolean> isRestoreOrExport,
                                              final Supplier<Boolean> notExportRestore) {
        if (action.isRestoreOrExport()) {
            return isRestoreOrExport.get();
        } else {
            return notExportRestore.get();
        }
    }

    /**
     * Once the BRMBackupManagerJson is read, it will update the flags with the configuration
     * @param manager backup Manager to be updated from config
     * @param brmManagerjson Optional json backup manager from configuration
     * @param context Ownership to filter backups.
     */
    public void updateProgressReportFlags(final BackupManager manager, final Optional<BRMBackupManagerJson> brmManagerjson,
                                          final Ownership context) {
        manager.backupManagerLevelProgressReportResetCreated(); // backupManager ProgressReport is false
        // reset all the backup flags to false
        Optional.ofNullable(manager.getBackups(context))
        .map(List::stream)
        .orElse(Stream.empty())
            .forEach(backup -> backup.backupLevelProgressReportResetCreated());
        if (!brmManagerjson.isEmpty()) {
            // validates if backup Manager progressReport was created of not
            if (!Optional.ofNullable(brmManagerjson.get())
                    .flatMap(brm -> Optional.ofNullable(brm.getProgressReports())).isEmpty()) {
                manager.backupManagerLevelProgressReportSetCreated(); // backupManager ProgressReport is set
            }
                // backupManager has backups, update the backups progress-report
            if (! Optional.ofNullable(brmManagerjson.get())
                    .flatMap(brm -> Optional.ofNullable(brm.getBackups())).isEmpty()) {
                brmManagerjson.get().getBackups()
                    .forEach(backup -> {
                    // if there any progressReport in the configuration
                        if (!Optional.ofNullable(backup.getProgressReports()).isEmpty()) {
                            final Optional<Backup> optBackup = Optional.ofNullable(
                                    manager.getBackups(context)
                                    .stream()
                                    .filter(mgrBackup -> mgrBackup.getBackupId().equalsIgnoreCase(backup.getBackupId()))
                                    .findFirst()
                                    .orElse(null)
                                );
                            // if the backup is still in the backup manager the flag is updated
                            if (!optBackup.isEmpty()) {
                                optBackup.get()
                                    .backupLevelProgressReportSetCreated();
                            }
                        }
                    });
            }
        }
    }

    private boolean isBRMBackupManagersEmpty() {
        final Optional<BRMEricssonbrmJson> brmEricConfiguration = brmConfigurationUtil.getEricssonBRMConfiguration();
        if (brmEricConfiguration.isPresent()) {
            return Optional.ofNullable(brmEricConfiguration.get())
                    .map(BRMEricssonbrmJson::getBRMConfiguration)
                    .flatMap(brmConfig -> Optional.ofNullable(brmConfig.getBrm()))
                    .flatMap(brm -> Optional.ofNullable(brm.getBackupManagers())).isEmpty();
        }
        return true;
    }

    private Optional<BRMBackupManagerJson> getBRMBackupManagerJson(final Action action) {
        final String backupId = action.getBackupManagerId();
        final Optional<BRMEricssonbrmJson> brmEricConfiguration = brmConfigurationUtil.getEricssonBRMConfiguration();
        if (brmEricConfiguration.isPresent()) {
            // Search in the BackupManagers the one listed in action
            return Optional.ofNullable(brmEricConfiguration.get())
                    .map(BRMEricssonbrmJson::getBRMConfiguration)
                    .flatMap(brmConfig -> Optional.ofNullable(brmConfig.getBrm()))
                    .flatMap(brm -> Optional.ofNullable(brm.getBackupManagers()))
                    .flatMap(bm -> bm.stream().filter(backupManager -> backupManager.getBackupManagerId().equals(backupId)).findAny());
        }
        return Optional.empty();
    }

    private void executeIfCMIsEnabled(final Runnable runnable) {
        executeIfCMIsEnabled(runnable, (() -> pushSchemaAndConfiguration(false, false)));
    }

    private void executeCMAndRetry(final Runnable runnable) {
        executeIfCMIsEnabled(runnable, (() -> pushSchemaAndConfiguration(true, false)));
    }

    private void executeIfCMIsEnabled(final Runnable runnable, final Runnable failureAction) {
        if (getCMMClient().isFlagEnabled()) {
            try {
                runnable.run();
            } catch (final Exception e) {
                final String exception = ExceptionUtils.getStackTrace(e);
                log.error("Failed to perform action on CM {}", hidePassword(exception));
                failureAction.run();
            }
        }
    }

    /**
     * Validates the Etag and notifId with the one used as base
     * @param etag tag to be validated with the base Etag
     * @param notifId to be validated
     * @return false if either the Etag or notifId are invalid, true any other way.
     */
    public boolean isValidEtagNotifId(final String etag, final Integer notifId) {
        return brmConfigurationUtil.isValidETag(etag, notifId);
    }

    public void setBrmConfigurationService(final BRMConfigurationUtil brmConfigurationService) {
        this.brmConfigurationUtil = brmConfigurationService;
    }

    /**
     * update the last baseEtag from CMMediator
     * @param etag eTag to be set as base
     * @param notifId to be set as base
     */
    public void updateEtagNotifId(final String etag, final Integer notifId) {
        brmConfigurationUtil.updateEtagNotifId(etag, notifId);
    }

    /**
     * Push schema and configuration to CMM.
     * Add NACM roles
     * Subscribe to CMM
     * @param retryFlag Boolean
     *        On true if an error occurs, an indefinite number of retries will be performed.
     *        On false if an error occurs, a single attempt to retry will be performed.
     * @param isStartup defines whether it is startup or not
     */
    protected void pushSchemaAndConfiguration(final boolean retryFlag, final boolean isStartup) {
        brmConfigurationUtil.pushSchemaAndConfiguration(retryFlag, isStartup);
    }

    public boolean isConfigurationinCMM() {
        return getCMMClient().exists(CONFIGURATION_BRO_RESOURCE);
    }

    /**
     * Push schema and configuration to CMM.
     * Add NACM roles
     * Subscribe to CMM
     * @param retryFlag Boolean
     *        On true if an error occurs, an indefinite number of retries will be performed.
     *        On false if an error occurs, a single attempt to retry will be performed.
     * @param isStartup defines whether it is startup or not
     * @param backupManagers List of backup managers
     */
    protected void pushSchemaAndConfigurationIndividualBM(final boolean retryFlag,
                                                          final boolean isStartup,
                                                          final List<BackupManager> backupManagers) {
        brmConfigurationUtil.pushSchemaAndConfiguration(retryFlag, isStartup, backupManagers);
    }

    @Autowired
    public void setProgressReportPatchFactory(final ProgressReportPatchFactory progressReportPatchFactory) {
        this.progressReportPatchFactory = progressReportPatchFactory;
    }

    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

    @Autowired
    public void setKeyStoreService(final KeyStoreService keyStoreService) {
        this.keyStoreService = keyStoreService;
    }

    /**
     * update the last baseEtag from CMMediator
     * @return Optional Etag value from BRMCOnfiguration
     */
    public Optional<String> updateEtagfromCMM() {
        final Optional<String> lastEtag = getLastEtagfromCMM();
        // If not able to retrieve the last etag, don't change it
        if (lastEtag.isPresent()) {
            brmConfigurationUtil.updateEtag(lastEtag.get());
        }
        return lastEtag;
    }

    /**
     * Get the etag from BRM Configuration
     * @return Optional String with the etag from CMM
     */
    public Optional<String> getLastEtagfromCMM() {
        return getCMMClient().getLastEtag(getBrmConfigurationResource());
    }

    /**
     * Converts the string configuration into a BRMEricssonbrmJson
     * @param configuration String with the configuration
     * @return object representing the configuration
     */
    public Optional<BRMEricssonbrmJson> getBRMConfiguration(final String configuration) {
        return brmConfigurationUtil.getBRMConfiguration(configuration);
    }
    /**
     * Sets the backup level progress report for
     * either RESTORE or EXPORT action that was last enqueued into the CM queue.
     * @param action a RESTORE or EXPORT action
     */
    protected void setLastEnqueuedBackupProgressReport(final Action action) {
        this.lastEnqueuedBackupProgressReport.set(Optional.of(action));
    }

    /**
     * Sets the backup manager level progress report for
     * either CREATE, IMPORT, or DELETE that was last enqueued into the CM queue.
     * @param action a CREATE, IMPORT, or DELETE action
     */
    protected void setLastEnqueuedBRMProgressReport(final Action action) {
        this.lastEnqueuedBRMProgressReport.set(Optional.of(action));
    }
}
