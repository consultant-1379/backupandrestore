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

import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V2_0;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress.SUCCESSFUL;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V4_0;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.progress.AgentProgress;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import java.util.List;

/**
 * Stage in which agents are doing post actions after a Backup.
 */
public class PostActionBackupJobStage extends BackupJobStage {

    /**
     * Creates Stage.
     *
     * @param agents participating in action.
     * @param job owner of stage.
     * @param notificationService to send notifications.
     */
    public PostActionBackupJobStage(final List<Agent> agents, final CreateBackupJob job, final NotificationService notificationService) {
        super(agents, job, notificationService);
    }

    @Override
    public void handleTrigger() {
        agents.stream().filter(agent -> API_V2_0.equals(agent.getApiVersion())).forEach(agent -> {
            agent.finishAction();
            updateAgentProgressForV2ToSucceeded(agent.getAgentId());
        });
        agents.stream().filter(agent -> API_V3_0.equals(agent.getApiVersion()) ||
                API_V4_0.equals(agent.getApiVersion())).forEach(Agent::executeBackupPostAction);
    }

    @Override
    protected JobStage<CreateBackupJob> getNextStageSuccess() {
        return new CompletedBackupJobStage(agents, job, notificationService);
    }

    @Override
    protected int getStageOrder() {
        return 3;
    }

    @Override
    protected boolean isStageFinished() {
        return this.agentProgress.values().stream().allMatch(AgentProgress::didFinish);
    }

    private void updateAgentProgressForV2ToSucceeded(final String agentId) {
        this.agentProgress.get(agentId).setProgress(SUCCESSFUL);
    }

    @Override
    public JobStageName getStageName() {
        return JobStageName.POST_ACTIONS;
    }
}
