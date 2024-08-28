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

import static com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress.DISCONNECTED;
import static com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress.WAITING_RESULT;

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
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.job.progress.AgentProgress;
import com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationFailedException;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.control.StageComplete;

/**
 * Final stage for Restore, indicating it failed.
 */
public class FailedRestoreJobStage extends RestoreJobStage {

    private static final Logger log = LogManager.getLogger(FailedRestoreJobStage.class);
    private final double progressPercentage;
    private final Map<String, Progress> cancellationProgress = new ConcurrentHashMap<>();
    /**
     * Creates Stage.
     * @param agents participating in action.
     * @param job owner of stage.
     * @param notificationService to send notifications.
     * @param progressPercentage when job failed
     * @param agentProgress progress when job failed
     */
    public FailedRestoreJobStage(final List<Agent> agents, final RestoreJob job, final NotificationService notificationService,
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
            log.warn("Failed to send message for action failed: ", e);
        }
        agents.stream().filter(agent -> agent.isConnectionCancelled()).forEach(agent -> {
            agentProgress.get(agent.getAgentId()).setProgress(Progress.DISCONNECTED);
            cancellationProgress.put(agent.getAgentId(), DISCONNECTED);
            final StageComplete stageComplete = StageComplete.newBuilder().setMessage("Agent Disconnected").setSuccess(false).build();
            job.updateProgress(agent.getAgentId(), stageComplete);
        });
        getStillConnectedAgents().forEach(agent -> {
            if (!agent.getState().getClass().isAssignableFrom(RecognizedState.class)) {
                cancellationProgress.put(agent.getAgentId(), WAITING_RESULT);
                agent.cancelAction();
            }
        });
    }

    @Override
    protected JobStage<RestoreJob> getNextStageSuccess() {
        throw new GetNextStageSuccessCalledFromFailedStageException("FailedRestoreJobStage");
    }

    @Override
    protected JobStage<RestoreJob> getNextStageFailure() {
        return this;
    }

    @Override
    public List<String> idsOfAgentsInProgress() {
        return cancellationProgress.entrySet().stream().filter(agentProgressEntry -> agentProgressEntry.getValue().equals(Progress.WAITING_RESULT))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    @Override
    public JobStage<RestoreJob> moveToFailedStage() {
        return this;
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
    public double getProgressPercentage() {
        return progressPercentage;
    }

    @Override
    protected int getStageOrder() {
        return 4;
    }

    private boolean isDoneWaitingForCancellation() {
        return cancellationProgress.values().stream().noneMatch(Progress.WAITING_RESULT::equals);
    }

    @Override
    public void updateAgentProgress(final String agentId, final StageComplete stageCompleteMessage) {
        cancellationProgress.put(agentId, getProgress(stageCompleteMessage));
    }

    @Override
    public void handleAgentDisconnecting(final String agentId) {
        cancellationProgress.put(agentId, DISCONNECTED);
    }

    protected Stream<Agent> getStillConnectedAgents() {
        return agents.stream().filter(agent -> agentProgress.get(agent.getAgentId()).isConnected())
                .filter(agent -> !agent.isConnectionCancelled());
    }

    @Override
    public JobStageName getStageName() {
        return JobStageName.FAIL;
    }
}
