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

import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_BRO_GRANULAR_STAGE_INFO;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_BRO_OPERATION_TRANSFERRED_BYTES;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ericsson.adp.mgmt.backupandrestore.SpringContext;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStageName;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.exception.JobFailedException;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStage;
import com.ericsson.adp.mgmt.control.StageComplete;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress;

/**
 * Represents jobs that deal with agents and have stages.
 *
 * @param <T> Job that has stages.
 */
public abstract class JobWithStages<T extends Job> extends Job {

    private static final Logger log = LogManager.getLogger(JobWithStages.class);
    private static final String JOB_ACTION = "action";
    private static final String ACTION_ID = "action_id";
    private static final String BACKUP_TYPE = "backup_type";
    private static final String STAGE = "stage";
    private static final String STATUS = "status";
    private static final String AGENT = "agent";
    private static final String BACKUP_NAME = "backup_name";

    protected List<Agent> agents = new ArrayList<>();
    protected JobStage<T> jobStage;
    protected Instant stageStartTime = Instant.now();
    private final Map<String, Long> agentChunkSizes = new ConcurrentHashMap<>();

    /**
     * Where data regarding fragment is stored.
     *
     * @param metadata contains fragment information.
     * @return where fragment data is stored.
     */
    public abstract FragmentFolder getFragmentFolder(Metadata metadata);

    /**
     * Update the progress of one agent's stage.
     *
     * @param agentId to update.
     * @param stageCompleteMessage message from the agent indicating if its stage was successful.
     */
    public void updateProgress(final String agentId, final StageComplete stageCompleteMessage) {
        final boolean isSucceed = stageCompleteMessage.getSuccess();
        final JobStageName stageName = this.jobStage.getStageName();
        log.info("Updating progress of agent <{}> with success <{}> for stage <{}>", agentId, isSucceed,
                stageName);

        if (!isSucceed) {
            updateProgressInfo(agentId, stageCompleteMessage, stageName);
        }
        agentStageInfos.add(new AgentStageInfo(agentId, stageName, isSucceed ? Progress.SUCCESSFUL : Progress.FAILED));
        updateGranularStageInfo(agentId, isSucceed ? Progress.SUCCESSFUL : Progress.FAILED);

        jobStage.updateAgentProgress(agentId, stageCompleteMessage);
        changeAndTriggerJobStage();

        getAction().addMessage(
                MessageFormat.format("Agent: {0}, Stage: {1}, {2}", agentId, stageName, stageCompleteMessage));
        updateProgressPercentage();
    }

    private synchronized void updateProgressInfo(final String agentId, final StageComplete stageCompleteMessage, final JobStageName stageName) {
        String progressInfo = getAction().getProgressInfo();
        final String newProgressInfoItem = String.format( "{Agent: %s failed at Stage: %s, %s}", agentId, stageName, stageCompleteMessage);
        if (progressInfo != null && !progressInfo.isBlank()) {
            progressInfo = String.format("%s, %s", progressInfo, newProgressInfoItem);
        } else {
            progressInfo = newProgressInfoItem;
        }
        getAction().setProgressInfo(progressInfo);
        try {
            getCMMediatorService().enqueueProgressReport(getAction());
        } catch (Exception e) {
            log.error("Failed to update the progress report in CMM", e);
        }
    }


    /**
     * Changes and then triggers.
     */
    protected synchronized void changeAndTriggerJobStage() {
        stageStartTime = Instant.now();
        jobStage = jobStage.changeStages();
        jobStage.trigger();
    }

    /**
     * Handles agent disconnecting
     *
     * @param agentId the agent id
     */
    public void handleAgentDisconnecting(final String agentId) {
        updateGranularStageInfo(agentId, Progress.DISCONNECTED);
        agentStageInfos.add(new AgentStageInfo(agentId, this.jobStage.getStageName(), Progress.DISCONNECTED));

        jobStage.handleAgentDisconnecting(agentId);
        updateProgressPercentage();
        changeAndTriggerJobStage();
    }

    /**
     * Handles unexpected data channel
     * Set the agent's progress to failed
     * @param agentId the agent id
     */
    protected void handleUnexpectedDataChannel(final String agentId) {
        jobStage.handleUnexpectedDataChannel(agentId);
        updateProgressPercentage();
        changeAndTriggerJobStage();
    }

    /**
     * Update bro.granular.stage.info metric
     * @param agentId the agent id
     * @param agentProgress states the progress of the agent
     */
    protected void updateGranularStageInfo(final String agentId, final Progress agentProgress) {
        SpringContext.getBean(MeterRegistry.class).ifPresent(registry -> {
            METRIC_BRO_GRANULAR_STAGE_INFO.unRegister();
            Gauge.builder(METRIC_BRO_GRANULAR_STAGE_INFO.identification(), () -> 1)
                    .tags(
                            AGENT, agentId,
                            JOB_ACTION, action.getName().toString(),
                            ACTION_ID, this.getActionId(),
                            STATUS, agentProgress.name(),
                            BACKUP_TYPE, this.getBackupManagerId(),
                            STAGE, jobStage.getStageName().name())
                    .description(METRIC_BRO_GRANULAR_STAGE_INFO.description())
                    .register(registry);
            }
        );
    }


    @Override
    protected boolean didFinish() {
        return this.jobStage.isJobFinished();
    }

    @Override
    protected void completeJob() {
        updateGranularOperationsTotal();
        logAgentTransferredBytes();
        if (!this.jobStage.isStageSuccessful()) {
            throw new JobFailedException(
                    String.format("Job %s failed at stage %s", getAction().getActionId(), this.jobStage));
        }
    }

    @Override
    protected void fail() {
        updateProgressPercentage();
        jobStage = jobStage.moveToFailedStage();
        jobStage.trigger();
        log.error("Job {} failed. Progress: {}", this.getAction().getActionId(), this.jobStage);
    }

    /**
     * Persists action with updated progress percentage
     */
    protected synchronized void updateProgressPercentage() {
        action.setProgressPercentage(this.jobStage.getProgressPercentage());
        action.persist();
    }

    protected List<Agent> getAgents() {
        return this.agents;
    }

    protected void setAgents(final List<Agent> agents) {
        this.agents = agents;
    }

    protected void setJobStage(final JobStage<T> jobStage) {
        this.jobStage = jobStage;
    }

    /**
     * Updates transferred chunk size for an agent
     * @param agentId the agent id
     * @param transferredBytes size of chunk transferred
     */
    public void updateAgentChunkSize(final String agentId, final long transferredBytes) {
        agentChunkSizes.merge(agentId, transferredBytes, (oldValue, newValue) -> oldValue + newValue);
    }

    private long getAgentTransferredBytes(final String agentId) {
        return agentChunkSizes.getOrDefault(agentId, 0L);
    }

    private void logAgentTransferredBytes() {
        agentChunkSizes.forEach( (agent, bytes) -> {
            log.info("Agent <{}> transferred <{}> bytes", agent, bytes);
        });
    }

    /**
     * Builds a metric for tracking each agent's transferred bytes
     * in the last backup/restore operation.
     */
    protected void buildJobPerfMetric() {
        switch (action.getName()) {
            case CREATE_BACKUP:
            case RESTORE:
                // Clear and repopulate metric with current operation
                SpringContext.getBean(MeterRegistry.class).ifPresent(meterRegistry -> {
                    meterRegistry
                            .find(METRIC_BRO_OPERATION_TRANSFERRED_BYTES.identification())
                            .tag(BACKUP_TYPE, getBackupManagerId())
                            .tag(JOB_ACTION, getAction().getName().name())
                            .gauges().forEach(meterRegistry::remove);

                    getAgents().forEach(agent -> {
                        // Metric for monitoring transferred data size by each agent
                        METRIC_BRO_OPERATION_TRANSFERRED_BYTES.unRegister();
                        Gauge.builder(METRIC_BRO_OPERATION_TRANSFERRED_BYTES.identification(), () -> getAgentTransferredBytes(agent.getAgentId()))
                                .description(METRIC_BRO_OPERATION_TRANSFERRED_BYTES.description())
                                .baseUnit("bytes")
                                .tags(BACKUP_TYPE, getBackupManagerId(),
                                        JOB_ACTION, getAction().getName().name(),
                                        BACKUP_NAME, action.getBackupName(),
                                        ACTION_ID, this.getActionId(),
                                        AGENT, agent.getAgentId())
                                .register(meterRegistry);
                    });
                });
                break;
            default:
                break;
        }
    }

    public JobStage<T> getJobStage() {
        return jobStage;
    }

    @Override
    protected void monitor() {
        super.monitor();
        if (this.jobStage.idsOfAgentsInProgress().isEmpty()) {
            log.info("Job monitor for JobStage<{}> has not found any agents still in progress, attempting to move to the next stage",
                    this.jobStage.getStageName());
            this.changeAndTriggerJobStage();
        } else {
            log.info("JobStage<{}> is waiting for the following agents to complete: ", this.jobStage.getStageName());
            this.jobStage.idsOfAgentsInProgress().forEach(agentId -> log.info("Agent: <{}>.", agentId));
        }
    }
}
