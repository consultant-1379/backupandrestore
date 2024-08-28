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
package com.ericsson.adp.mgmt.backupandrestore.job.stage;

import static com.ericsson.adp.mgmt.backupandrestore.action.ResultType.FAILURE;
import static com.ericsson.adp.mgmt.backupandrestore.action.ResultType.SUCCESS;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_BRO_OPERATION_STAGE_DURATION_SECONDS;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.SpringContext;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.Job;
import com.ericsson.adp.mgmt.backupandrestore.job.progress.AgentProgress;
import com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.control.StageComplete;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Represents a stage in a Job.
 * @param <T> Job that has stages.
 */
public abstract class JobStage<T extends Job> {

    private static final Logger log = LogManager.getLogger(JobStage.class);
    private static final int NUMBER_OF_DECIMAL_DIGITS = 2;

    protected final List<Agent> agents;
    protected final Map<String, AgentProgress> agentProgress = new ConcurrentHashMap<>();
    protected final T job;
    protected final NotificationService notificationService;

    private final AtomicBoolean wasTriggerCalled = new AtomicBoolean(false);
    private final AtomicReference<JobStage<T>> nextStageReference = new AtomicReference<>();
    private OffsetDateTime startTime;
    private OffsetDateTime completionTime;

    /**
     * Creates Stage.
     * @param agents participating in action.
     * @param job owner of stage.
     * @param notificationService to send notifications.
     */
    public JobStage(final List<Agent> agents, final T job, final NotificationService notificationService) {
        this.agents = agents;
        this.agents.forEach(agent -> this.agentProgress.put(agent.getAgentId(), new AgentProgress()));
        this.job = job;
        this.notificationService = notificationService;
    }

    /**
     * Triggers stage.
     */
    public void trigger() {
        if (!wasTriggerCalled.get()) {
            log.info("Starting job stage <{}>", getStageName());
            wasTriggerCalled.set(true);
            startTime = OffsetDateTime.now(ZoneId.systemDefault());
            handleTrigger();
        }
    }

    /**
     * This is called from trigger, it should be overridden with the code to start running this {@link JobStage}.
     */
    protected abstract void handleTrigger();

    /**
     * Instantly moves to a failed stage.
     * @return next stage.
     */
    public abstract JobStage<T> moveToFailedStage();

    /**
     * Override with code to run if the stage is successful.
     * @return - a {@link JobStage}
     */
    protected abstract JobStage<T> getNextStageSuccess();


    /**
     * Override with code to run if the stage fails.
     * @return - a {@link JobStage}
     */
    protected abstract JobStage<T> getNextStageFailure();

    /**
     * Order in which this stage will run in the Job, starting from 1.
     * To be used in progress percentage calculation.
     * @return stage order
     */
    protected abstract int getStageOrder();

    /**
     * How many non final job stages there are for the Job.
     * To be used in progress percentage calculation.
     * @return number of non final stages.
     */
    protected abstract int getNumberOfNonFinalStages();

    /**
     * Update the progress of one agent's stage.
     * @param agentId to update.
     * @param stageCompleteMessage message from the agent indicating if its stage was successful.
     */
    public void updateAgentProgress(final String agentId, final StageComplete stageCompleteMessage) {
        this.agentProgress.get(agentId).setProgress(getProgress(stageCompleteMessage));
    }

    /**
     * Handles agent disconnecting
     * @param agentId the agent id
     */
    public void handleAgentDisconnecting(final String agentId) {
        failWaitingFragments(agentId);
        this.agentProgress.get(agentId).setProgress(Progress.DISCONNECTED);
    }

    /**
     * Handles unexpected data channel
     * Sets the agent progress to failed.
     * @param agentId agent id
     */
    public void handleUnexpectedDataChannel(final String agentId) {
        failWaitingFragments(agentId);
        this.agentProgress.get(agentId).setProgress(Progress.FAILED);
    }

    /**
     * Indicates whether job is finished.
     * @return true if finished.
     */
    public boolean isJobFinished() {
        return false;
    }

    /**
     * Gets progress percentage for that stage.
     * @return progress percentage.
     */
    public double getProgressPercentage() {
        return BigDecimal
                .valueOf((getCurrentStageProgress() / getNumberOfNonFinalStages()) + getPreviousStagesProgress())
                .setScale(NUMBER_OF_DECIMAL_DIGITS, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Receive a new fragment from agentProgress
     * @param agentId agent id
     * @param fragmentId fragment id
     */
    public void receiveNewFragment(final String agentId, final String fragmentId) {
        // Does Nothing
    }

    /**
     * Move to next job stage on fragment success
     * @param agentId agent id
     * @param fragmentId fragment id
     */
    public void fragmentSucceeded(final String agentId, final String fragmentId){
        //Place holder, does nothing.
    }

    /**
     * Move to next job stage on fragment failure
     * @param agentId agent id
     * @param fragmentId fragment id.
     */
    public void fragmentFailed(final String agentId, final String fragmentId){
        //Placeholder, does nothing.
    }

    /**
     * Indicates whether stage is successful.
     * @return true if successful.
     */
    public boolean isStageSuccessful() {
        return this.agentProgress.values().stream().allMatch(AgentProgress::didSucceed);
    }

    /**
     * Return's a {@link List} of ids for agents still in progress.
     * @return a list of strings
     */
    public List<String> idsOfAgentsInProgress() {
        return this.agentProgress.entrySet().stream().filter(agentProgressEntry -> !agentProgressEntry
                .getValue()
                .didFinish())
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " - " + this.agentProgress;
    }

    /**
     * Extract progress from message
     * @param stageCompleteMessage message
     * @return true if successful
     */
    protected Progress getProgress(final StageComplete stageCompleteMessage) {
        if (stageCompleteMessage.getSuccess()) {
            return Progress.SUCCESSFUL;
        }
        return Progress.FAILED;
    }

    /**
     * Update progress of a fragment
     * @param agentId of the agent the fragment belongs to
     * @param fragmentId of the fragment
     * @param progress of the fragment
     */
    protected void updateFragment(final String agentId, final String fragmentId, final Progress progress) {
        log.info("Updating status of fragment {} from agent {} to {}", fragmentId, agentId, progress);
        this.agentProgress.get(agentId).setFragmentProgress(fragmentId, progress);
    }

    /**
     * Change job stage based on stage progress
     * @return nextStage of the job
     */
    public synchronized JobStage<T> changeStages() {
        if (isStageFinished()) {
            completionTime = OffsetDateTime.now(ZoneId.systemDefault());
            log.info("Job stage <{}> finished with success <{}>", getStageName(), isStageSuccessful());
            buildMetrics();
            return getNextStage();
        }
        return this;
    }
    /**
     * Build metrics function
     */
    private void buildMetrics() {
        SpringContext.getBean(MeterRegistry.class).ifPresent(registry -> {
            registry.find(METRIC_BRO_OPERATION_STAGE_DURATION_SECONDS.identification())
                    .tag("action", job.getAction().getName().name())
                    .tag("backup_type", job.getBackupManagerId())
                    .tag("stage", getStageName().name())
                    .gauges()
                    .forEach(registry::remove);

            switch (job.getAction().getName()) {
                case CREATE_BACKUP:
                case RESTORE:
                    log.debug("Building '{}' gauge for action: '{}' job stage: '{}'",
                            METRIC_BRO_OPERATION_STAGE_DURATION_SECONDS.identification(), job.getAction().getName().name(), this.getStageName());
                    // Build a gauge metric for exposing job stage execution duration.
                    METRIC_BRO_OPERATION_STAGE_DURATION_SECONDS.unRegister();
                    Gauge.builder(METRIC_BRO_OPERATION_STAGE_DURATION_SECONDS.identification(),
                        () -> completionTime.toEpochSecond() - startTime.toEpochSecond())
                            .description(METRIC_BRO_OPERATION_STAGE_DURATION_SECONDS.description())
                            .baseUnit("seconds")
                            .tags("action_id", job.getAction().getActionId(), "backup_type", job.getBackupManagerId(),
                                    "action", job.getAction().getName().name(),
                                    "stage", getStageName().name(),
                                    "status", isStageSuccessful() ? SUCCESS.name() : FAILURE.name())
                            .register(registry);
                    break;
                default:
            }
        }
        );
    }

    /**
     * Get next stage.
     * @return next stage.
     */
    private JobStage<T> getNextStage() {
        if (isStageSuccessful() && nextStageReference.get() == null) {
            nextStageReference.set(getNextStageSuccess());
        } else if (nextStageReference.get() == null) {
            nextStageReference.set(getNextStageFailure());
        }
        return nextStageReference.get();
    }

    private void failWaitingFragments(final String agentId) {
        this.agentProgress.get(agentId).failWaitingFragments();
    }

    /**
     * returns true if stage finished.
     * @return - true if stage finished.
     */
    protected boolean isStageFinished() {
        return this.agentProgress.values().stream().allMatch(AgentProgress::didFinish);
    }

    private double getNumberOfSuccessfulAgents() {
        return this.agentProgress.values().stream().filter(AgentProgress::didSucceed).count();
    }

    private double getCurrentStageProgress() {
        if (this.agentProgress.isEmpty()) {
            return 0.0;
        }
        return getNumberOfSuccessfulAgents() / this.agentProgress.size();
    }

    private double getPreviousStagesProgress() {
        final int previousStageOrder = getStageOrder() - 1;
        return 1.0 * previousStageOrder / getNumberOfNonFinalStages();
    }

    /**
     * Get agents.
     * @return agents.
     */
    public List<Agent> getAgents() {
        return agents;
    }

    /**
     * Get stage name.
     * @return stage name.
     */
    public abstract JobStageName getStageName();

}
