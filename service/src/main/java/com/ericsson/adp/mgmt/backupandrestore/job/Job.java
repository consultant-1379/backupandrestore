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

import static com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType.FINISHED;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.CREATE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.RESTORE;
import static com.ericsson.adp.mgmt.backupandrestore.action.ResultType.FAILURE;
import static com.ericsson.adp.mgmt.backupandrestore.action.ResultType.SUCCESS;
import static com.ericsson.adp.mgmt.backupandrestore.util.ExceptionUtils.getRootCause;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_BRO_GRANULAR_OPERATIONS_TOTAL;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_BRO_OPERATION_STAGE_DURATION_SECONDS;
import static com.ericsson.adp.mgmt.backupandrestore.util.OSUtils.sleep;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import com.ericsson.adp.mgmt.backupandrestore.action.*;
import com.ericsson.adp.mgmt.backupandrestore.exception.AnotherActionRunningException;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.exception.ExportException;
import com.ericsson.adp.mgmt.backupandrestore.exception.ImportException;
import com.ericsson.adp.mgmt.backupandrestore.exception.ImportExportException;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupManagerbroNotAllowedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.CacheManager;

import com.ericsson.adp.mgmt.backupandrestore.SpringContext;
import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStageName;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationFailedException;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.data.Metadata;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;

/**
 * Executes action.
 */
public abstract class Job {

    private static final Logger log = LogManager.getLogger(Job.class);

    private static final int JOB_MONITOR_PERIOD_IN_SECONDS = 300;

    protected BackupManager backupManager;
    protected Action action;
    protected ActionRepository actionRepository;
    protected NotificationService notificationService;
    protected ActionService actionService;
    protected List<AgentStageInfo> agentStageInfos = new CopyOnWriteArrayList<>();
    private S3Config s3Config;
    private JobExecutor jobExecutor;
    private CMMediatorService cmMediatorService;

    /**
     * Runs job.
     * @return a list of actions to be run immediately after this job finishes executing, e.g. a list of DELETE_BACKUP
     *         actions for a housekeeping job or an EXPORT job for a scheduled CREATE_BACKUP
     */
    public List<CreateActionRequest> run() {
        ResultType result = ResultType.SUCCESS;
        switch (action.getName()) {
            case CREATE_BACKUP:
            case IMPORT:
                log.debug("Init execution of housekeeping for:{} id:{}", action.getName(), action.getActionId());
                result = actionService.executeHousekeeping(backupManager);
                break;
            default:
                break;
        }
        if (result != FAILURE) {
            log.debug("Continue the execution of {} id:{}", action.getName(), action.getActionId());
            setAsRunningJob();
            actionRepository.performActionCleanup();
            runJob();
        } else {
            failAction(String.format("%s Housekeeping failure", action.getName()));
        }
        return getPostExecutionActions();
    }

    /**
     * Handle an unexpected data channel opening.
     * @param metadata The metadata.
     */
    public void handleUnexpectedDataChannel(final Metadata metadata) {
        log.info("Attempted to open unexpected data channel with metadata {} for job", metadata);
    }

    /**
     * Get the list of actions to be taken immediately after this job finishes executing. Inheritors should override
     * this if they want jobs to execute after they have finished
     * @return optionally, a list of actions to be created and executed
     * */
    protected List<CreateActionRequest> getPostExecutionActions() {
        return new LinkedList<>();
    }

    /**
     * Start job.
     */
    protected abstract void triggerJob();

    /**
     * Indicates whether the job has finished.
     *
     * @return true if finished.
     */
    protected abstract boolean didFinish();

    /**
     * Steps to successfully complete the job.
     */
    protected abstract void completeJob();

    /**
     * Steps to handle job failure.
     */
    protected abstract void fail();

    /**
     * Hook for monitoring a job. Override this to use a custom message
     */
    protected void monitor() {
        log.info("<{}> in progress.", this.getClass().getSimpleName());
    }

    public void setBackupManager(final BackupManager backupManager) {
        this.backupManager = backupManager;
    }

    protected void setActionRepository(final ActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }

    public String getActionId() {
        return action.getActionId();
    }

    public String getBackupManagerId() {
        return backupManager.getBackupManagerId();
    }

    public BackupManager getBackupManager() {
        return backupManager;
    }

    public Action getAction() {
        return action;
    }

    protected void setAction(final Action action) {
        this.action = action;
    }

    /**
     * Used to be used to set the internal job lock to a shared lock across all jobs, as jobs were responsible for
     * their own order of execution. Now does nothing, as the job executor manages order of execution
     * @param lock - unused
     * @deprecated redundant function
     * */
    @Deprecated(since = "05/11/2020")
    protected void setLock(final ReentrantReadWriteLock lock) {
        // Jobs no longer manage a shared lock, so this does nothing
    }

    protected void setJobExecutor(final JobExecutor jobExecutor) {
        this.jobExecutor = jobExecutor;
    }

    protected void setNotificationService(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    protected void setActionService(final ActionService actionService) {
        this.actionService = actionService;
    }

    private void runJob() {
        final JobMonitor monitor = new JobMonitor(this, JOB_MONITOR_PERIOD_IN_SECONDS);
        removePreviousJobMetrics();
        try {
            log.info("Job starting for {} action - <{}>", action.getName(), action.getActionId());
            lockBackup();
            if (action.getBackupManagerId().endsWith("-bro") &&
                    action.getName() != ActionType.RESTORE) {
                throw new BackupManagerbroNotAllowedException("Only " + ActionType.RESTORE + " is allowed for -bro backup manager");
            }
            triggerJob();
            waitForJobToFinish();
            completeJob();
            successfullyCompleteAction();
        } catch (final ImportExportException | ImportException | ExportException importExportException ) {
            handleFailure(importExportException);
            failAction(importExportException.getMessage() , getRootCause(importExportException).getMessage());
        } catch (final Exception e) {
            final String grpcFailure =
                "CANCELLED: call already cancelled.";
            String runTimeError;
            if (e.getMessage().contains(grpcFailure)) {
                runTimeError = "Agent Disconnected";
            } else {
                runTimeError = e.getMessage();
            }
            handleFailure(e);
            failAction(runTimeError);
        } finally {
            unlockBackup();
            monitor.stop();
            clearRunningJob();
            actionRepository.performActionCleanup();
            clearPVCMetricsCache();
        }
    }

    private void handleFailure(final Exception exception) {
        log.error("Job failed for action - <{}>", action.getActionId(), exception);
        try {
            fail();
        } catch (final Exception exec) {
            log.warn("Exception while converting Job - <{}> to failed state.", action.getActionId(), exec);
        }
    }

    private void clearPVCMetricsCache() {
        SpringContext.getBean(CacheManager.class).ifPresent(cacheManager ->
            cacheManager.getCacheNames().forEach(cacheName -> {
                log.debug("Clearing the PVC metric cache <{}>", cacheName);
                cacheManager.getCache(cacheName).clear();
            })
        );
    }

    /**
     * This method only applies to HOUSEKEEPING_DELETE, EXPORT and RESTORE actions
     * to ensure that only one operation can act on the backup at a time.
     * It attempts to update the ongoingAction on the backup. If the backup
     * already has a running action, then the update fails and an
     * exception will be thrown.
     */
    private void lockBackup() {
        if (action.isHousekeepingDelete() || action.isExport() || action.isRestore()) {
            final Backup backup = backupManager.getBackup(action.getBackupName(),
                    action.isRestore() ? Ownership.READABLE : Ownership.OWNED);
            if (!backup.setActionLock(action)) {
                final Optional<Action> ongoingAction = backup.getActionLock();
                final String ongoingActionId = ongoingAction.isPresent() ? ongoingAction.get().getActionId() + " " : "";
                final String failureMessage = String.format("Failed to run %s on backup %s as another action %s is running on backup %s",
                        action.getName(), backup.getName(), ongoingActionId, backup.getName());
                throw new AnotherActionRunningException(failureMessage);
            }
        }
    }

    /**
     * This method only applies to HOUSEKEEPING_DELETE, EXPORT and RESTORE actions
     * to ensure that only one operation can act on the backup at a time.
     * It attempts to remove the ongoingAction on the backup.
     * An action can only unlock the backup if it is the backup's ongoing action.
     */
    private void unlockBackup() {
        if (action.isHousekeepingDelete() || action.isExport() || action.isRestore()) {
            try {
                final Backup backup = backupManager.getBackup(action.getBackupName(),
                        action.isRestore() ? Ownership.READABLE : Ownership.OWNED);
                backup.removeActionLock(action);
            } catch (BackupNotFoundException exception) {
                String failureReason = "the backup could not be found";
                if (action.isHousekeepingDelete() && action.getResult().equals(ResultType.SUCCESS)) {
                    failureReason = "the backup is already deleted";
                }
                log.info("The <{}> action did not unlock the backup <{}> as " + failureReason , action.getName(), action.getBackupName());
            }
        }
    }

    /**
     * Update bro.granular.operations.total
     */
    protected void updateGranularOperationsTotal() {
        final Map<String, List<AgentStageInfo>> groupByAgentId = agentStageInfos
                .stream()
                .collect(Collectors.groupingBy(AgentStageInfo::getAgentId));

        groupByAgentId.forEach((agentId, agentStageInfos) -> {
            final boolean isFailure = agentStageInfos
                    .stream()
                    .anyMatch(agentStageInfo -> agentStageInfo.getAgentProgress().equals(Progress.FAILED)
                            || agentStageInfo.getAgentProgress().equals(Progress.DISCONNECTED));

            METRIC_BRO_GRANULAR_OPERATIONS_TOTAL.unRegister();
            Metrics.counter(METRIC_BRO_GRANULAR_OPERATIONS_TOTAL.identification(),
                    "agent", agentId,
                    "action", getAction().getName().toString(),
                    "status", isFailure ? Progress.FAILED.name() : Progress.SUCCESSFUL.name(),
                    "backup_type", this.getBackupManagerId()).increment();
        });
    }

    public List<AgentStageInfo> getAgentStageInfos() {
        return this.agentStageInfos;
    }

    private void successfullyCompleteAction() {
        log.info("Successfully completing action {} - <{}>", action.getName(), action.getActionId());
        action.setState(FINISHED);
        if (action.getResult() == ResultType.NOT_AVAILABLE) {
            action.setResult(ResultType.SUCCESS);
        }
        action.setProgressPercentage(1.0);
    }

    private void failAction(final String reason) {
        failAction(reason, reason);
    }

    private void failAction(final String reason, final String additionalInfo) {
        log.warn("Failing action <{}> - {}", action.getActionId(), reason);
        action.setResult(FAILURE);
        action.setAdditionalInfo(additionalInfo);
    }

    /**
     * Mark the action as completed. This is exposed so that job executors that must
     * take some post job execution actions are able to control the time at which the
     * action is marked as finished, and the action complete notification is sent
     * */
    public final void completeAction() {
        action.setState(FINISHED);
        action.setCompletionTime(OffsetDateTime.now(ZoneId.systemDefault()));
        action.setLastUpdateTime(action.getCompletionTime());
        SpringContext.getBean(MeterRegistry.class).ifPresent(this::buildMetrics);
        action.persist();

        if (shouldNotify(action.getResult())) {
            try {
                notificationService.notifyAllActionCompleted(action);
            } catch (final NotificationFailedException e) {
                log.warn("Failed to send notification for action completed: {}" , e.getMessage());
            }
        }
    }

    private void setAsRunningJob() {
        jobExecutor.setCurrentJob(Optional.of(this));
    }

    private void clearRunningJob() {
        jobExecutor.setCurrentJob(Optional.empty());
    }

    private void waitForJobToFinish() {
        do {
            sleep(1000);
        } while (!didFinish());
    }

    private boolean shouldNotify(final ResultType result ) {
        return (action.getName().equals(CREATE_BACKUP) || isRegularRestoreJob())
                && SUCCESS.equals(result);
    }

    private boolean isRegularRestoreJob() {
        return action.getName().equals(RESTORE) && !(this instanceof ResetConfigJob);
    }

    /**
     * Inject the AWS configuration into the job that needs
     * @param s3Config the AWS configuration
     */
    protected void setAwsConfig(final S3Config s3Config) {
        this.s3Config = s3Config;
    }

    /**
     * Get the AWS configuration
     * @return the AWS configuration
     */
    public S3Config getAwsConfig() {
        return s3Config;
    }
    private void removePreviousJobMetrics() {
        // Remove 'bro.operation.stage.duration.seconds' metric
        SpringContext.getBean(MeterRegistry.class).ifPresent(registry -> {
            registry.find(METRIC_BRO_OPERATION_STAGE_DURATION_SECONDS.identification())
                    .tag("backup_type", getBackupManagerId())
                    .tag("status", action.getResult().name())
                    .gauges()
                    .forEach(registry::remove);
            // Remove 'bro.operation.stage.duration.seconds' metric
            registry.find("bro.granular.stage.info")
                    .gauges()
                    .forEach(registry::remove);
            }
        );
    }

    private void buildMetrics(final MeterRegistry registry) {
        action.updateOperationsTotalMetric();
        action.updateLastOperationInfoMetric();
        switch (action.getName()) {
            case DELETE_BACKUP:
            case EXPORT:
            case IMPORT:
                log.debug("Building '{}' gauge for action: '{}'",
                        METRIC_BRO_OPERATION_STAGE_DURATION_SECONDS.identification(), action.getName().name());
                final var duration = action.getCompletionTime().toEpochSecond() - action.getStartTime().toEpochSecond();
                // Build a gauge metric for exposing job execution duration.
                METRIC_BRO_OPERATION_STAGE_DURATION_SECONDS.unRegister();
                Gauge.builder(METRIC_BRO_OPERATION_STAGE_DURATION_SECONDS.identification(), () -> duration)
                        .description(METRIC_BRO_OPERATION_STAGE_DURATION_SECONDS.description()).baseUnit("seconds")
                        .tags("backup_type", getBackupManagerId(),
                                "action", action.getName().name(),
                                "stage", JobStageName.EXECUTION.name(),
                                "status", action.getResult().name())
                        .register(registry);
                break;
            default:
                break;
        }
    }


    public void setCmMediatorService(final CMMediatorService cmMediatorService) {
        this.cmMediatorService = cmMediatorService;
    }

    public CMMediatorService getCMMediatorService() {
        return this.cmMediatorService;
    }

    /**
     * Reset Configuration, Schema and subscription in CM
     * @param validateConfig true is need to validate if config exists, false otherwise
     */
    public void resetCM(final boolean validateConfig) {
        // if not validate config, then run the thread directly
        // if true, then it look for the configuration in CMM, on false it run the thread
        if (!cmMediatorService.getCMMClient().isReady()) {
            return;
        }
        if (!validateConfig || !cmMediatorService.isConfigurationinCMM()) {
            new Thread(() -> {
                backupManager.backupManagerLevelProgressReportResetCreated();
                cmMediatorService.prepareCMMediator(false);
            }).start();
        }
    }
}
