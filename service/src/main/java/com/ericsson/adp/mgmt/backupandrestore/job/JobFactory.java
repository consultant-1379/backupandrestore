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
package com.ericsson.adp.mgmt.backupandrestore.job;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V2_0;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionRepository;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionService;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentRepository;
import com.ericsson.adp.mgmt.backupandrestore.agent.discovery.AgentDiscoveryService;
import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupExporter;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupImporter;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.BackupManagerFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.HousekeepingFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.ScheduledEventHandler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.storage.StorageMetadataFileService;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.ExecutingBackupJobStageV2;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.ExecutingHousekeepingJobStage;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.PreparingBackupJobStage;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.PreparingRestoreJobStage;
import com.ericsson.adp.mgmt.backupandrestore.kms.CMKeyPassphraseService;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProviderFactory;
import com.ericsson.adp.mgmt.backupandrestore.productinfo.ProductInfoService;
import com.ericsson.adp.mgmt.backupandrestore.restore.FragmentFileService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Creates jobs.
 */
@SuppressWarnings("PMD.TooManyFields")
@Service
public class JobFactory {

    private static final Logger log = LogManager.getLogger(JobFactory.class);

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private JobExecutor jobExecutor;
    private ActionService actionService;
    private ActionRepository actionRepository;
    private BackupRepository backupRepository;
    private BackupLocationService backupLocationService;
    private BackupFileService backupFileService;
    private FragmentFileService fragmentFileService;
    private AgentRepository agentRepository;
    private NotificationService notificationService;
    private AgentDiscoveryService agentDiscoveryService;
    private StorageMetadataFileService storageMetadataFileService;
    private CMMediatorService cmMediatorService;
    private BackupImporter backupImporter;
    private BackupExporter backupExporter;
    private ProductInfoService productInfoService;
    private boolean autoDeleteFailures;
    private CMKeyPassphraseService exportPasswordService;
    private BackupManagerFileService backupManagerFileService;
    private PersistProviderFactory persistProviderFactory;

    private ScheduledEventHandler scheduledEventHandler;
    private HousekeepingFileService housekeepingFileService;

    private S3Config s3Config;
    private SchedulerFileService schedulerFileService;

    /**
     * Creates jobs based on action.
     *
     * @param backupManager
     *            owner of action.
     * @param action
     *            to be executed.
     * @return job.
     */
    public Job createJob(final BackupManager backupManager, final Action action) {
        Job job;

        switch (action.getName()) {
            case CREATE_BACKUP:
                job = getCreateBackupJob(backupManager);
                break;
            case DELETE_BACKUP:
            case HOUSEKEEPING_DELETE_BACKUP:
                job = getDeleteBackupJob();
                break;
            case RESTORE:
                job = getRestoreJob(backupManager, action);
                break;
            case IMPORT:
                job = getImportBackupJob();
                break;
            case EXPORT:
                job = getExportBackupJob();
                break;
            case HOUSEKEEPING:
                job = getHouseKeepingJob(action);
                break;
            default:
                job = new NotImplementedJob();
        }

        return setJobInformation(job, backupManager, action);
    }

    @Autowired
    public void setActionService(final ActionService actionService) {
        this.actionService = actionService;
    }

    /**
     * Create a backup job
     * @param backupManager the backup manager
     * @return the CreateBackupJob
     */
    private Job getCreateBackupJob(final BackupManager backupManager) {
        final List<Agent> agents = getAgents(backupManager);
        if (!agents.isEmpty()) {
            final CreateBackupJob job = new CreateBackupJob();
            job.setBackupRepository(backupRepository);
            job.setBackupLocationService(backupLocationService);
            job.setAgents(agents);
            job.setAgentDiscoveryService(agentDiscoveryService);
            job.setStorageMetadataFileService(storageMetadataFileService);
            job.setAutoDeleteFailures(autoDeleteFailures);
            job.setActionRepository(actionRepository);
            job.setCmMediatorService(cmMediatorService);
            if (agents.stream().allMatch(agent -> API_V2_0.equals(agent.getApiVersion()))) {
                job.setJobStage(new ExecutingBackupJobStageV2(agents, job, notificationService));
            } else {
                job.setJobStage(new PreparingBackupJobStage(agents, job, notificationService));
            }
            job.setAwsConfig(s3Config);
            return job;
        }
        return new AgentNotRegisteredJob();
    }

    private Job getRestoreJob(final BackupManager backupManager, final Action action) {
        if (backupManager.isVirtual() && backupManager.getBackupManagerId().endsWith(ResetConfigJob.RESET_BRM_SUFFIX)) {
            final ResetConfigJob job = new ResetConfigJob();
            job.setBackupLocationService(backupLocationService);
            job.setProvider(persistProviderFactory.getPersistProvider());
            job.setBrmFileService(backupManagerFileService);
            job.setHandler(scheduledEventHandler);
            job.setHousekeepingFileService(housekeepingFileService);
            job.setSchedulerFileService(schedulerFileService);
            return job;
        }
        final List<Agent> agents = getAgents(backupManager);

        if (!agents.isEmpty()) {
            final RestoreJob job = new RestoreJob();
            final List<Agent> agentsParticipatingInRestore = getRestoreAgents(backupManager, action.getBackupName());
            job.setBackupLocationService(backupLocationService);
            job.setFragmentFileService(fragmentFileService);
            job.setAgents(agentsParticipatingInRestore);
            job.setCmMediatorService(cmMediatorService);
            job.setProductInfoService(productInfoService);
            job.setJobStage(new PreparingRestoreJobStage(agentsParticipatingInRestore, job, notificationService));
            job.setActionRepository(actionRepository);
            job.setAwsConfig(s3Config);
            return job;
        }
        return new AgentNotRegisteredJob();
    }

    /**
     * Job Stage to clean databases until limit is reached
     * @return Job
     */
    private Job getHouseKeepingJob(final Action action) {
        final HousekeepingJob job = new HousekeepingJob();
        job.setBackupRepository(backupRepository);
        job.setExecuteAsTask(action.isExecutedAsTask());
        job.setJobStage(new ExecutingHousekeepingJobStage(new ArrayList<>(), job, notificationService));
        job.setActionRepository(actionRepository);
        job.setCmMediatorService(cmMediatorService);
        return job;
    }

    private Job getDeleteBackupJob() {
        final DeleteBackupJob job = new DeleteBackupJob();
        job.setBackupLocationService(backupLocationService);
        job.setBackupRepository(backupRepository);
        job.setActionRepository(actionRepository);
        job.setAwsConfig(s3Config);
        job.setCmMediatorService(cmMediatorService);
        return job;
    }

    private Job getExportBackupJob() {

        final ExportBackupJob job = new ExportBackupJob();
        job.setBackupLocationService(backupLocationService);
        job.setBackupFileService(backupFileService);
        job.setBackupExporter(backupExporter);
        job.setActionRepository(actionRepository);
        job.setExportPasswordService(exportPasswordService);
        job.setCmMediatorService(cmMediatorService);
        return job;
    }

    private Job getImportBackupJob() {

        final ImportBackupJob job = new ImportBackupJob();
        job.setBackupLocationService(backupLocationService);
        job.setBackupFileService(backupFileService);
        job.setBackupImporter(backupImporter);
        job.setActionRepository(actionRepository);
        job.setCmMediatorService(cmMediatorService);
        return job;
    }

    private Job setJobInformation(final Job job, final BackupManager backupManager, final Action action) {
        job.setBackupManager(backupManager);
        job.setAction(action);
        job.setLock(lock);
        job.setJobExecutor(jobExecutor);
        job.setNotificationService(notificationService);
        job.setActionService(actionService);
        job.setActionRepository(actionRepository);
        return job;
    }

    private List<Agent> getAgents(final BackupManager backupManager) {
        return agentRepository.getAgents().stream()
                .filter(agent -> backupManager.ownsAgent(agent.getScope(), agent.getAgentId())).collect(Collectors.toList());
    }

    private List<Agent> getRestoreAgents(final BackupManager backupManager, final String backupName) {
        BackupManager ownerOfBackupBeingRestored = backupManager;
        if (backupManager.isVirtual() && !backupManager.ownsBackup(backupName)) {
            // If we're doing a virtual restore, use the parent to get the list of potential agents
            final Optional<BackupManager> optionalBackupManager = backupManager.getParent();
            if (optionalBackupManager.isPresent()) {
                ownerOfBackupBeingRestored = optionalBackupManager.get();
            }
        }
        final List<String> agentIdsForBackup = backupLocationService.getAgentIdsForBackup(ownerOfBackupBeingRestored, backupName);
        return getAgents(backupManager)
                .stream().filter(agent -> agentIdsForBackup.contains(agent.getAgentId())).collect(Collectors.toList());
    }

    @Autowired
    public void setBackupRepository(final BackupRepository backupRepository) {
        this.backupRepository = backupRepository;
    }

    @Autowired
    public void setActionRepository(final ActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }

    @Autowired
    public void setBackupLocationService(final BackupLocationService backupLocationService) {
        this.backupLocationService = backupLocationService;
    }

    @Autowired
    public void setBackupFileService(final BackupFileService backupFileService) {
        this.backupFileService = backupFileService;
    }

    @Autowired
    public void setFragmentFileService(final FragmentFileService fragmentFileService) {
        this.fragmentFileService = fragmentFileService;
    }

    @Autowired
    public void setAgentRepository(final AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Autowired
    public void setNotificationService(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Autowired
    public void setAgentDiscoveryService(final AgentDiscoveryService agentDiscoveryService) {
        this.agentDiscoveryService = agentDiscoveryService;
    }

    @Autowired
    public void setStorageMetadataFileService(final StorageMetadataFileService storageMetadataFileService) {
        this.storageMetadataFileService = storageMetadataFileService;
    }

    @Autowired
    public void setCmMediatorService(final CMMediatorService cmMediatorService) {
        this.cmMediatorService = cmMediatorService;
    }

    @Autowired
    public void setJobExecutor(final JobExecutor jobExecutor) {
        this.jobExecutor = jobExecutor;
    }

    @Autowired
    public void setBackupImporter(final BackupImporter backupImporter) {
        this.backupImporter = backupImporter;
    }

    @Autowired
    public void setBackupExporter(final BackupExporter backupExporter) {
        this.backupExporter = backupExporter;
    }

    @Autowired
    public void setProductInfoService(final ProductInfoService productInfoService) {
        this.productInfoService = productInfoService;
    }

    /**
     * Set the autoDeleteFailures feature to enabled or disabled.
     * @param autoDeleteFailures boolean, true is enabled, false isn't
     * */
    @Value("${backup.autoDeleteFailures:false}")
    public void setAutoDeleteFailures(final boolean autoDeleteFailures) {
        if (autoDeleteFailures) {
            log.info("Automatic deletion of failed backups enabled");
        }
        this.autoDeleteFailures = autoDeleteFailures;
    }

    /**
     * Autowired the AWS configuration into the job Factory
     * @param s3Config the AWS configuration
     */
    @Autowired
    public void setAwsConfig(final S3Config s3Config) {
        this.s3Config = s3Config;
    }

    @Autowired
    public void setKeyManagementService(final CMKeyPassphraseService exportPasswordService) {
        this.exportPasswordService = exportPasswordService;
    }

    @Autowired
    public void setBackupManagerFileService(final BackupManagerFileService backupManagerFileService) {
        this.backupManagerFileService = backupManagerFileService;
    }

    @Autowired
    public void setPersistProviderFactory(final PersistProviderFactory persistProviderFactory) {
        this.persistProviderFactory = persistProviderFactory;
    }

    @Autowired
    public void setScheduledEventHandler(final ScheduledEventHandler scheduledEventHandler) {
        this.scheduledEventHandler = scheduledEventHandler;
    }

    @Autowired
    public void setHousekeepingFileService(final HousekeepingFileService housekeepingFileService) {
        this.housekeepingFileService = housekeepingFileService;
    }

    @Autowired
    public void setSchedulerFileService(final SchedulerFileService schedulerFileService) {
        this.schedulerFileService = schedulerFileService;
    }
}
