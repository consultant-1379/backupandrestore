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

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import java.util.List;

/**
 * Stage in which agents are preparing for Restore.
 */
public class PreparingRestoreJobStage extends RestoreJobStage {

    /**
     * Creates Stage.
     * @param agents participating in action.
     * @param job owner of stage.
     * @param notificationService to send notifications.
     */
    public PreparingRestoreJobStage(final List<Agent> agents, final RestoreJob job, final NotificationService notificationService) {
        super(agents, job, notificationService);
    }

    @Override
    protected void handleTrigger() {
        this.notificationService.notifyAllActionStarted(job.getAction());
        agents.forEach(agent -> agent.prepareForRestore(job));
    }

    @Override
    protected JobStage<RestoreJob> getNextStageSuccess() {
        return new ExecutingRestoreJobStage(agents, job, notificationService);
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
