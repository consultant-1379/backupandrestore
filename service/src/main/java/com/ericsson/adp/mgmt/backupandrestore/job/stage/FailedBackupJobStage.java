/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.job.stage;

import static com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress.DISCONNECTED;
import static com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress.WAITING_RESULT;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V2_0;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V4_0;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ericsson.adp.mgmt.backupandrestore.agent.state.RecognizedState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.exception.GetNextStageSuccessCalledFromFailedStageException;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.progress.AgentProgress;
import com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationFailedException;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.control.StageComplete;

/**
 * Final stage for Backup, indicating it was Failure.
 */
public class FailedBackupJobStage extends BackupJobStage {

    private static final Logger log = LogManager.getLogger(FailedBackupJobStage.class);
    private final double progressPercentage;
    private final Map<String, Progress> cancellationProgress = new ConcurrentHashMap<>();

    /**
     * Creates Stage.
     * @param agents participating in action.
     * @param job owner of stage.
     * @param progressPercentage when job failed
     * @param agentProgress progress when job failed
     * @param notificationService to send notification
     */
    public FailedBackupJobStage(final List<Agent> agents, final CreateBackupJob job, final NotificationService notificationService,
                                final double progressPercentage, final Map<String, AgentProgress> agentProgress) {
        super(agents, job, notificationService);
        this.progressPercentage = progressPercentage;
        this.agentProgress.putAll(agentProgress);
    }

    @Override
    public void handleTrigger() {
        try {
            notificationService.notifyAllActionFailed(job.getAction());
        } catch (final NotificationFailedException e) {
            log.warn("Failed to send notification for action failed: ", e);
        }
        agents.stream().filter(agent -> API_V2_0.equals(agent.getApiVersion())).forEach(Agent::finishAction);

        agents.stream().filter(agent -> agent.isConnectionCancelled()).forEach(agent -> {
            agentProgress.get(agent.getAgentId()).setProgress(Progress.DISCONNECTED);
            cancellationProgress.put(agent.getAgentId(), DISCONNECTED);
            final StageComplete stageComplete = StageComplete.newBuilder().setMessage("Agent Disconnected").setSuccess(false).build();
            job.updateProgress(agent.getAgentId(), stageComplete);
        });
        getStillConnectedAgents().filter(agent ->  API_V3_0.equals(agent.getApiVersion()) ||
                API_V4_0.equals(agent.getApiVersion())).forEach(agent -> {
                    if (!agent.getState().getClass().isAssignableFrom(RecognizedState.class)) {
                        cancellationProgress.put(agent.getAgentId(), WAITING_RESULT);
                        agent.cancelAction();
                    }
                });
    }

    @Override
    public JobStage<CreateBackupJob> moveToFailedStage() {
        return this;
    }

    @Override
    public double getProgressPercentage() {
        return progressPercentage;
    }

    @Override
    public void updateAgentProgress(final String agentId, final StageComplete stageCompleteMessage) {
        cancellationProgress.put(agentId, getProgress(stageCompleteMessage));
    }

    @Override
    public List<String> idsOfAgentsInProgress() {
        return cancellationProgress.entrySet().stream().filter(agentProgressEntry -> agentProgressEntry.getValue().equals(Progress.WAITING_RESULT))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override
    public void handleAgentDisconnecting(final String agentId) {
        cancellationProgress.put(agentId, DISCONNECTED);
    }

    private boolean isDoneWaitingForCancellation() {
        return cancellationProgress.values().stream().noneMatch(WAITING_RESULT::equals);
    }

    @Override
    public boolean isJobFinished() {
        return isDoneWaitingForCancellation();
    }

    @Override
    public boolean isStageSuccessful() {
        return false;
    }

    @Override
    protected JobStage<CreateBackupJob> getNextStageSuccess() {
        throw new GetNextStageSuccessCalledFromFailedStageException("FailedBackupJobStage");
    }

    @Override
    protected JobStage<CreateBackupJob> getNextStageFailure() {
        return this;
    }

    @Override
    protected int getStageOrder() {
        return 4;
    }

    private Stream<Agent> getStillConnectedAgents() {
        return agents.stream().filter(agent -> !API_V2_0.equals(agent.getApiVersion())).
                filter(agent -> !agent.isConnectionCancelled()).
                filter(agent -> agentProgress.get(agent.getAgentId()).isConnected());
    }

    @Override
    public JobStageName getStageName() {
        return JobStageName.FAIL;
    }
}
