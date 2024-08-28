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
import static com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress.FAILED;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V4_0;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.progress.AgentProgress;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Stage in which agents are preparing for a Backup.
 */
public class PreparingBackupJobStage extends BackupJobStage {
    /**
     * Creates Stage.
     * @param agents participating in action.
     * @param job owner of stage.
     * @param notificationService to send notifications.
     */
    public PreparingBackupJobStage(final List<Agent> agents, final CreateBackupJob job, final NotificationService notificationService) {
        super(agents, job, notificationService);
    }

    @Override
    protected void handleTrigger() {
        notificationService.notifyAllActionStarted(job.getAction());
        agents.stream().filter(agent -> API_V2_0.equals(agent.getApiVersion())).forEach(
            agent -> updateAgentProgressForV2ToSucceeded(agent.getAgentId()));
        agents.stream().filter(agent -> API_V3_0.equals(agent.getApiVersion()) ||
            API_V4_0.equals(agent.getApiVersion())).forEach(agent -> agent.prepareForBackup(job));
    }

    @Override
    protected JobStage<CreateBackupJob> getNextStageSuccess() {
        return new ExecutingBackupJobStage(agents, job, notificationService);
    }

    @Override
    public void receiveNewFragment(final String agentId, final String fragmentId) {
        if (doesAgentAPISupportStages(agentId)) {
            throw new BackupServiceException("Backup data channel opened for fragment <"
                    + fragmentId + "> by agent <" + agentId + "> during " + this.getClass().getSimpleName());
        } else {
            agentProgress.get(agentId).handleNewFragment(fragmentId);
        }
    }

    @Override
    public void fragmentSucceeded(final String agentId, final String fragmentId) {
        if (!doesAgentAPISupportStages(agentId)) {
            updateFragment(agentId, fragmentId, SUCCESSFUL);
        }
    }

    private boolean doesAgentAPISupportStages(final String agentId) {
        return agents.stream().filter(agent -> API_V3_0.equals(agent.getApiVersion())).map(Agent::getAgentId)
            .collect(Collectors.toList()).contains(agentId);
    }

    @Override
    public void fragmentFailed(final String agentId, final String fragmentId) {
        if (!doesAgentAPISupportStages(agentId)) {
            updateFragment(agentId, fragmentId, FAILED);
        }
    }

    private void updateAgentProgressForV2ToSucceeded(final String agentId) {
        this.agentProgress.get(agentId).setProgress(SUCCESSFUL);
    }

    @Override
    protected boolean isStageFinished() {
        return this.agentProgress.values().stream().allMatch(AgentProgress::didFinish);
    }

    @Override
    protected int getStageOrder() {
        return 1;
    }

    @Override
    public JobStageName getStageName() {
        return JobStageName.PREPARATION;
    }

}
