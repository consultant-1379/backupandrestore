/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup.manager;

import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager.DEFAULT_BACKUP_MANAGER_ID;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.DEFAULT_MAX_BACKUP;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.CalendarEventFileService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionRepository;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.agent.VBRMAutoCreate;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.BackupManagerFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.HousekeepingFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.PersistedBackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.VirtualInformationFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.ScheduledEventHandler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerInformation;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServer;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMEricssonbrmJson;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupManagerNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.exception.FilePersistenceException;
import com.ericsson.adp.mgmt.backupandrestore.job.ResetConfigJob;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProviderFactory;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;

/**
 * Controls access to backupManagers.
 */
@Service
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.TooManyFields"})
public class BackupManagerRepository {
    private static final String UNSPECIFIED_SCOPE = "";
    private static final Function<Integer, Integer> maxBackupNumber = n -> n > DEFAULT_MAX_BACKUP ? n : DEFAULT_MAX_BACKUP;
    private static Logger log = LogManager.getLogger(BackupManagerRepository.class);

    private final List<BackupManager> backupManagers = new ArrayList<>();
    private BackupManagerFileService backupManagerfileService;
    private HousekeepingFileService housekeepingFileService;
    private SchedulerFileService schedulerFileService;
    private SftpServerFileService sftpServerFileService;
    private VirtualInformationFileService virtualInformationFileService;
    private VBRMAutoCreate vbrmAutoCreate = VBRMAutoCreate.NONE;
    private boolean deleteVBRM;
    private PeriodicEventFileService periodicEventFileService;
    private CalendarEventFileService calendarEventFileService;
    private ScheduledEventHandler eventHandler;
    private CMMediatorService cmMediatorService;
    private ActionRepository actionRepository;
    private BackupRepository backupRepository;
    private IdValidator idValidator;
    private PersistProviderFactory providerFactory;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Backup manager Constructor
     * Lock the object operation until it is initialized
     */
    public BackupManagerRepository() {
        lock.lock();
    }

    /**
     * Initializes BackupManagers from persisted ones.
     * Is the backup Manager is already in memory it ignores the initialization
     */
    public void initializeBackupManagers() {
        log.debug("Initialize backup managers");
        final List<String> discardedvBRMs = new ArrayList<>();
        backupManagerfileService.getBackupManagers().stream()
            .filter(deleteVBRM ? persistedBRM -> isvBRMRequired(persistedBRM, discardedvBRMs) : persistedBRM -> true)
            .map(this::createBackupManager).forEach(backupManager -> {
                addTobackupManagers (backupManager);
                // Update any running actions. Don't send updates to CMM, since it isn't ready yet and we push our entire
                // configuration later. If this is a vBRM and the action is a restore it's possible the action refers to a
                // backup that the vBRM doesn't know about yet, so any attempt to write it's state to CMM would result in a
                // *fun* new way to get BRO stuck in a boot loop
                backupManager.getActions().forEach(action -> actionRepository.failActionIfRunning(action));
                // As above, write to persistence layer but not CMM, as that's done later. The reason we can't write these
                // to CMM is that we would need to write the backup to any vBRMs, which at this point might not know about
                // their parents backups, and as such that would result in a boot loop (as the CMService tries to get an
                // index for the backup from the vBRM and the vBRM throws a BackupNotFound exception)
                backupManager.getBackups(Ownership.OWNED).forEach(backup -> backupRepository.corruptBackupIfIncomplete(backup));
                // This does try to write to CMM, but that's fine, since the scheduler is definitely only going to write to
                // it's own node under a BRM, so BRO will only get stuck in boot loop if CMM disagrees with us on how many
                // BRMs there are, which was true before vBRMs were introduced
                eventHandler.schedulePeriodicEvents(backupManager.getScheduler());
                eventHandler.scheduleCalendarEvents(backupManager.getScheduler());
                updateOperationsTotalMetric(backupManager.getBackupManagerId());
            });

        if (deleteVBRM) {
            delete(discardedvBRMs);
        }
        // Now that all backup managers are in memory, we can sync the backup lists between child and parent, mark any
        // ongoing backups as corrupted, and persist them
        final List<String> missingResetVBRM = new ArrayList<>();
        for (final BackupManager backupManager: backupManagers) {
            // sync this BRMs backup list to any children it might have. As above, we don't worry about writing this to CMM
            backupManager.syncChildren();
            log.info("BackupManager:{}", backupManager.getBackupManagerId());
            if (!isResetBRM(backupManager.getBackupManagerId()) && isMissingResetBRM(backupManager) && !backupManager.isVirtual()) {
                missingResetVBRM.add(backupManager.getBackupManagerId());
            }
        }
        missingResetVBRM.forEach(id -> createBackupManager(id, id + ResetConfigJob.RESET_BRM_SUFFIX, new ArrayList<>()));
        log.debug("Backup manager repository is ready");
    }

    /**
     * Checks if the persisted vBRM needs to be initialized and added to the BRO config
     * If not, add the discarded vBRM to the list of vBRMs that need to be removed from the persistence layer
     * @param discardedvBRMS the list of discarded virtual backup managers
     * @return true if the vBRM needs to be initialized, false otherwise.
     */
    private boolean isvBRMRequired(final PersistedBackupManager persistedBRM, final List<String> discardedvBRMS) {
        final String backupManagerId = persistedBRM.getBackupManagerId();
        if (isResetBRM(backupManagerId) || vbrmAutoCreate == VBRMAutoCreate.ALL) {
            return true;
        }
        final VirtualInformation virtualInformation = readVirtualInformation(backupManagerId);
        final String parentId = virtualInformation.getParentId();
        if (parentId.isEmpty()) {
            return true;
        }
        if (vbrmAutoCreate == VBRMAutoCreate.DEFAULT && parentId.equals(DEFAULT_BACKUP_MANAGER_ID)) {
            return true;
        } else {
            discardedvBRMS.add(backupManagerId);
            return false;
        }
    }

    private void delete(final List<String> discardedVBRMIds) {
        discardedVBRMIds.forEach( backupManagerId -> {
            log.debug("Deleting left over files from unused virtual backup manager {}", backupManagerId);
            backupManagerfileService.deleteBackups(backupManagerId);
            backupManagerfileService.delete(backupManagerId);
        });
    }

    private VirtualInformation readVirtualInformation(final String backupManagerId) {
        return virtualInformationFileService.getVirtualInformation(backupManagerId)
                                                            .orElse(new VirtualInformation());
    }

    private boolean isMissingResetBRM(final BackupManager backupManager) {
        return backupManagers.stream().noneMatch(
            b -> b.getBackupManagerId().equals(backupManager.getBackupManagerId() + ResetConfigJob.RESET_BRM_SUFFIX)
        );
    }

    private boolean isResetBRM(final String backupManagerId) {
        return backupManagerId.endsWith(ResetConfigJob.RESET_BRM_SUFFIX);
    }

    /**
     * Constructs and runs the fail behaviour for a ResetConfigJob, to cleanup after a configuration reset that was interrupted
     * by a BRO restart
     * @param action - the action describing the reset
     * */
    public void failResetAction(final Action action) {
        if (!action.getName().equals(ActionType.RESTORE) || !action.getBackupManagerId().endsWith(ResetConfigJob.RESET_BRM_SUFFIX)) {
            throw new IllegalArgumentException("Cannot fail action as it is not a config reset");
        }
        log.info("Failing action {} as part of start up sequence", action.getActionId());
        final Optional<BackupManager> backupManager = getBackupManager(action.getBackupManagerId()).getParent();
        if (backupManager.isPresent()) {
            final BackupManager brm = backupManager.get();
            final ResetConfigJob job = new ResetConfigJob();
            job.setProvider(providerFactory.getPersistProvider());
            job.setBrmFileService(backupManagerfileService);
            job.setHandler(eventHandler);
            job.setHousekeepingFileService(housekeepingFileService);
            job.setSchedulerFileService(schedulerFileService);
            job.setBackupManager(brm);
            job.fail(); // Fail the job, which runs all necessary cleanup and reload functions
        }
    }

    /**
     * Gets all backupManagers.
     * @return list of backupManagers.
     */
    public List<BackupManager> getBackupManagers() {
        return new ArrayList<>(backupManagers);
    }

    /**
     * Get specific backupManager.
     * Exception if not backup manager is present
     * @param backupManagerId id to look for.
     * @return specific backupManager.
     */
    public BackupManager getBackupManager(final String backupManagerId) {
        return findBackupManager(backupManagerId).orElseThrow(() -> new BackupManagerNotFoundException(backupManagerId));
    }

    /**
     * Get specific backupManager.
     * Exception if not backup manager is present
     * @param backupManagerIndex positional index on backupmanager list
     * @return specific backupManager.
     */
    public BackupManager getBackupManager(final int backupManagerIndex) {
        return findBackupManager(backupManagerIndex).orElseThrow(() -> new BackupManagerNotFoundException(String.valueOf(backupManagerIndex)));
    }

    /**
     * Get the vBRM's that are children of a given BRM, if there are any
     * @param backupManagerId - the ID of the BRM who's children you want a stream of
     * @return a stream of the child vBRM's of the passed BRM
     * */
    public Stream<BackupManager> getChildren(final String backupManagerId) {
        return backupManagers.stream().filter(b -> b.isVirtual() && b.getVirtualInformation().getParentId().equals(backupManagerId));
    }

    /**
     * Creates new BackupManager with given id, if no current backup manager has that id.
     * @param backupManagerId id of new backupManager.
     */
    public void createBackupManager(final String backupManagerId) {
        createBackupManager(backupManagerId, true);
    }

    /**
     * Creates new BackupManager with given id, if no current backup manager has that id.
     * @param backupManagerId id of new backupManager.
     * @param notifyMediator true notify mediator the new BM
     */
    public void createBackupManager(final String backupManagerId, final boolean notifyMediator) {
        final String parsedId = parseBackupManagerId(backupManagerId);
        createBackupManager("", parsedId, new ArrayList<>(), notifyMediator);
        // Since we just created a BRM and not a vBRM, lets create it's config restore vBRM now
        createBackupManager(parsedId, parsedId + ResetConfigJob.RESET_BRM_SUFFIX, new ArrayList<>(), notifyMediator);
    }

    /**
     * Creates the DEFAULT BackupManager, if it doesn't exist
     */
    public void finishInitialization () {
        lock.unlock();
    }
    /**
     * Creates new (potentially virtual) BackupManager with given parent, id and agent list, if it doesn't exist
     * @param parentId - id of parent BRM to this vBRM, if virtual, otherwise an empty string
     * @param backupManagerId id of new backupManager.
     * @param agents - agents to register to this vBRM, if virtual, otherwise an empty list
     */
    public void createBackupManager(final String parentId, final String backupManagerId, final List<Agent> agents) {
        createBackupManager (parentId, backupManagerId, agents, true);
    }

    /**
     * Creates new (potentially virtual) BackupManager with given parent, id and agent list, if it doesn't exist
     * @param parentId - id of parent BRM to this vBRM, if virtual, otherwise an empty string
     * @param backupManagerId id of new backupManager.
     * @param agents - agents to register to this vBRM, if virtual, otherwise an empty list
     * @param notifyMediator true notify mediator the new BM
     */
    public void createBackupManager(final String parentId, final String backupManagerId, final List<Agent> agents, final boolean notifyMediator) {
        final String newBackupManagerId = parseBackupManagerId(backupManagerId);
        idValidator.validateId(newBackupManagerId);
        lock.lock();
        log.debug("Create backup manager: {}", backupManagerId);
        try {
            if (!isBackupManagerInMemory(newBackupManagerId)) {
                initializeBackupManager(
                        newBackupManagerId,
                        parentId,
                        agents.stream().map(Agent::getAgentId).collect(Collectors.toList()),
                        notifyMediator);
            }
            if (log.isDebugEnabled()) {
                final Map<Integer, String> positionIdMap = IntStream.range(0, backupManagers.size())
                        .boxed()
                        .collect(Collectors.toMap(
                            index -> index,
                            index -> (String) backupManagers.get(index).getBackupManagerId()
                            ));
                log.debug ("<{}> was created in backup managers <{}>", backupManagerId, positionIdMap);
            }
        } catch (final Exception excp) {
            log.error("Error creating backup manager: {}:", backupManagerId, excp);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets index of backupManager.
     * @param backupManagerId to look for.
     * @return index of backupManager.
     */
    public int getIndex(final String backupManagerId) {
        final int index = backupManagers
                .stream()
                .map(BackupManager::getBackupManagerId)
                .collect(Collectors.toList())
                .indexOf(backupManagerId);
        if (index == -1) {
            throw new BackupManagerNotFoundException(backupManagerId);
        }
        return index;
    }

    private void initializeBackupManager(final String backupManagerId,
                                         final String parentId,
                                         final List<String> agentIds,
                                         final boolean notifyMediator) {
        log.info("Setting backup manager <{}>", backupManagerId);
        final Housekeeping housekeeping = new Housekeeping(backupManagerId, this::persistHousekeeping);
        final BackupManager backupManager = new BackupManager(
                backupManagerId,
                housekeeping,
                createAndPersistNewScheduler(backupManagerId),
                this::persist,
                backupManagerfileService,
                this,
                new VirtualInformation(parentId, agentIds));
        addTobackupManagers(backupManager);
        // if this is a newly created vBRM, sync the parent BRMs children
        final Optional<BackupManager> backupmanagerparent = backupManager.getParent();
        if (backupmanagerparent.isPresent()) {
            backupmanagerparent.get().syncChildren();
        }
        backupManagerfileService.writeToFile(backupManager);
        virtualInformationFileService.writeToFile(backupManager);
        housekeepingFileService.writeToFile(housekeeping);
        if (notifyMediator) {
            addBackupManager(backupManager);
        }
        periodicEventFileService.createPeriodicEventsDirectory(backupManager);
        calendarEventFileService.createCalendarEventsDirectory(backupManager);
    }

    private Optional<BackupManager> findBackupManager(final String backupManagerId) {
        if (log.isDebugEnabled()) {
            final Map<Integer, String> positionIdMap = IntStream.range(0, backupManagers.size())
                    .boxed()
                    .collect(Collectors.toMap(
                        index -> index,
                        index -> (String) backupManagers.get(index).getBackupManagerId()
                        ));
            log.debug ("Searching <{}> in the current repository <{}>", backupManagerId, positionIdMap);
        }
        return backupManagers
                .stream()
                .filter(backupManager -> backupManager.getBackupManagerId().equals(backupManagerId))
                .findFirst();
    }

    private Optional<BackupManager> findBackupManager(final int backupManagerIndex) {
        return Optional.of(backupManagers.get(backupManagerIndex));
    }

    private boolean isBackupManagerInMemory(final String scope) {
        return findBackupManager(scope).isPresent();
    }

    private String parseBackupManagerId(final String backupManagerId) {
        if (UNSPECIFIED_SCOPE.equals(backupManagerId)) {
            return BackupManager.DEFAULT_BACKUP_MANAGER_ID;
        }
        return backupManagerId;
    }

    private BackupManager createBackupManager(final PersistedBackupManager persistedBackupManager) {
        final String backupManagerId = persistedBackupManager.getBackupManagerId();
        return new BackupManager(
                persistedBackupManager,
                actionRepository.getActions(backupManagerId),
                backupRepository.getBackups(backupManagerId),
                getPersistedSftpServers(backupManagerId),
                getHousekeepingInformation(backupManagerId),
                getOrCreateScheduler(backupManagerId),
                this::persist,
                backupManagerfileService,
                this,
                readVirtualInformation(backupManagerId)
                );
    }

    /**
     * Get the list of persisted SFTP Servers for a given BRM ID
     * @param backupManagerId the ID of the backup manager
     * @return a list of the BRM's persisted SFTP Server
     */
    public List<SftpServer> getPersistedSftpServers(final String backupManagerId) {
        return sftpServerFileService.getSftpServers(backupManagerId);
    }

    private void persist(final BackupManager backupManager) {
        backupManagerfileService.writeToFile(backupManager);
        virtualInformationFileService.writeToFile(backupManager);
        cmMediatorService.updateBackupManager(backupManager);
    }

    private void persistHousekeeping(final Housekeeping housekeeping) {
        housekeepingFileService.writeToFile(housekeeping);
        cmMediatorService.updateHousekeeping(housekeeping);
    }

    private void persistScheduler(final Scheduler scheduler) {
        schedulerFileService.writeToFile(scheduler);
        cmMediatorService.updateScheduler(scheduler);
    }

    /**
     * Persists the SftpServer
     * @param sftpServer the SFTP Server
     */
    public void persistSftpServer(final SftpServer sftpServer) {
        sftpServerFileService.writeToFile(sftpServer);
    }

    /**
     * Creates a default housekeeping from backup manager id
     * @param backupManagerId backup manager id
     * @return Housekeeping created
     */
    public Housekeeping createHousekeeping(final String backupManagerId) {
        Housekeeping housekeeping;
        final int numberOfBackups = backupRepository.getBackups(backupManagerId).size();
        housekeeping = new Housekeeping(maxBackupNumber.apply(numberOfBackups), AUTO_DELETE_ENABLED, backupManagerId, this::persistHousekeeping);
        return housekeeping;
    }

    /**
     * Used by ProbeFileManager to check if backupManager has finished initialising before
     * writing the health status of probes.
     */
    public void cycle () {
        if (!lock.isHeldByCurrentThread()) {
            lock.lock();
            lock.unlock();
        }
    }

    /**
     * Return a backup information and persist it from backup manager Id
     * @param backupManagerId Backup manager Id
     * @return Housekeeping object
     */
    public Housekeeping getHousekeepingInformation(final String backupManagerId) {
        Housekeeping housekeeping;
        final int numberOfBackups = backupRepository.getBackups(backupManagerId).size();

        try {
            final HousekeepingInformation persistedHousekeeping = housekeepingFileService
                    .getPersistedHousekeepingInformation(backupManagerId);

            housekeeping = new Housekeeping(persistedHousekeeping.getMaxNumberBackups(),
                    persistedHousekeeping.getAutoDelete(), backupManagerId, this::persistHousekeeping);

            if (numberOfBackups > housekeeping.getMaxNumberBackups()) {
                housekeeping = new Housekeeping(numberOfBackups, housekeeping.getAutoDelete(),
                        backupManagerId, this::persistHousekeeping);

                housekeeping.persist();
            }
            return housekeeping;

        } catch (final IndexOutOfBoundsException e) {
            housekeeping = new Housekeeping(maxBackupNumber.apply(numberOfBackups), AUTO_DELETE_ENABLED, backupManagerId, this::persistHousekeeping);
            housekeepingFileService.writeToFile(housekeeping);
            cmMediatorService.addHousekeeping(housekeeping);
            return housekeeping;
        }
    }

    /**
     * Adds backupManager to CM.
     * @param backupManager to be added.
     */
    public void addBackupManager(final BackupManager backupManager) {
        cmMediatorService.addBackupManager(backupManager);
    }

    private Scheduler getOrCreateScheduler(final String backupManagerId) {
        Scheduler scheduler;
        try {
            final SchedulerInformation schedulerInfo = schedulerFileService.getPersistedSchedulerInformation(backupManagerId);
            scheduler = new Scheduler(schedulerInfo, eventHandler.getPeriodicEvents(backupManagerId),
                    eventHandler.getCalendarEvents(backupManagerId),
                    backupManagerId, this::persistScheduler);
        } catch (final IndexOutOfBoundsException | FilePersistenceException | NullPointerException e) {
            scheduler = createAndPersistNewScheduler(backupManagerId);
            cmMediatorService.addScheduler(scheduler);
        }
        return scheduler;
    }

    private void addTobackupManagers(final BackupManager backupmanager) {
        final boolean existBackupManager = backupManagers.stream()
                .anyMatch(obj -> obj.equals(backupmanager));
        if (!existBackupManager) {
            log.debug("Adding backupManager {} to List", backupmanager.getBackupManagerId());
            backupManagers.add(backupmanager);
        }
    }

    /**
     * Converts the string configuration into a BRMEricssonbrmJson
     * @param configuration String with the configuration
     * @return object representing the configuration
     */
    public Optional<BRMEricssonbrmJson> getBRMConfiguration(final String configuration) {
        return cmMediatorService.getBRMConfiguration(configuration);
    }

    /**
     * Creates a scheduler and persist it
     * @param backupManagerId Backup manager Id
     * @return Scheduler object
     */
    public Scheduler createAndPersistNewScheduler(final String backupManagerId) {
        final Scheduler scheduler = new Scheduler(backupManagerId, this::persistScheduler);
        schedulerFileService.writeToFile(scheduler);
        return scheduler;
    }

    private void updateOperationsTotalMetric(final String backupManagerId) {
        actionRepository.getActions(backupManagerId)
                .forEach(Action::updateOperationsTotalMetric);
    }

    /**
     * Retrieves the lastEtag from CMM
     */
    public void getLastEtagfromCMM() {
        cmMediatorService.getLastEtagfromCMM();
    }

    // Setter for the logger (for testing purposes)
    public void setLogger(final Logger logger) {
        this.log = logger;
    }

    @Autowired
    public void setBackupManagerFileService(final BackupManagerFileService fileService) {
        backupManagerfileService = fileService;
    }

    @Autowired
    public void setHousekeepingFileService(final HousekeepingFileService housekeepingFileService) {
        this.housekeepingFileService = housekeepingFileService;
    }

    @Autowired
    public void setCmMediatorService(final CMMediatorService cmMediatorService) {
        this.cmMediatorService = cmMediatorService;
    }

    @Autowired
    public void setActionRepository(final ActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }

    @Autowired
    public void setBackupRepository(final BackupRepository backupRepository) {
        this.backupRepository = backupRepository;
    }

    @Autowired
    public void setIdValidator(final IdValidator idValidator) {
        this.idValidator = idValidator;
    }

    @Autowired
    public void setSchedulerFileService(final SchedulerFileService schedulerFileService) {
        this.schedulerFileService = schedulerFileService;
    }

    @Autowired
    public void setVirtualInformationFileService(final VirtualInformationFileService virtualInformationFileService) {
        this.virtualInformationFileService = virtualInformationFileService;
    }

    @Autowired
    public void setPeriodicEventFileService(final PeriodicEventFileService periodicEventFileService) {
        this.periodicEventFileService = periodicEventFileService;
    }

    @Autowired
    public void setCalendarEventFileService(final CalendarEventFileService calendarEventFileService) {
        this.calendarEventFileService = calendarEventFileService;
    }

    @Autowired
    public void setSftpServerFileService(final SftpServerFileService sftpServerFileService) {
        this.sftpServerFileService = sftpServerFileService;
    }

    @Autowired
    public void setScheduledEventHandler(final ScheduledEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Autowired
    public void setProviderFactory(final PersistProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    @Value("${vBRMAutoCreate:NONE}")
    public void setVbrmAutoCreate(final VBRMAutoCreate autoCreate) {
        this.vbrmAutoCreate = autoCreate;
    }

    @Value("${flag.deleteVBRM:false}")
    public void setDeleteVBRM(final boolean deleteVBRM) {
        this.deleteVBRM = deleteVBRM;
    }
}
