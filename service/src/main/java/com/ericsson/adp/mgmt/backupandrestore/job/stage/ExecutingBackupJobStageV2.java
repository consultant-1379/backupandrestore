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

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import java.util.List;

/**
 * Stage in which agents are executing a Backup.
 */
public class ExecutingBackupJobStageV2 extends ExecutingBackupJobStage {

    /**
     * Creates Stage.
     * @param agents participating in action.
     * @param job owner of stage.
     * @param notificationService to send notifications.
     */
    public ExecutingBackupJobStageV2(final List<Agent> agents, final CreateBackupJob job, final NotificationService notificationService) {
        super(agents, job, notificationService);
    }

    @Override
    protected void handleTrigger() {
        agents.stream().forEach(agent -> agent.prepareForBackup(job));
    }

    @Override
    protected JobStage<CreateBackupJob> getNextStageSuccess() {
        return new CompletedBackupJobStage(agents, job, notificationService);
    }


    @Override
    protected int getStageOrder() {
        return 1;
    }

}
