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
import com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Final stage for Backup, indicating it was successful.
 */
public class CompletedBackupJobStage extends BackupJobStage {

    private static final Logger log = LogManager.getLogger(CompletedBackupJobStage.class);

    /**
     * Creates Stage.
     * @param agents participating in action.
     * @param job owner of stage.
     * @param notificationService to send notifications.
     */
    public CompletedBackupJobStage(final List<Agent> agents, final CreateBackupJob job, final NotificationService notificationService) {
        super(agents, job, notificationService);
    }

    @Override
    public void handleTrigger() {
        agents.forEach(agent -> {
            agent.finishAction();
            agentProgress.get(agent.getAgentId()).setProgress(Progress.SUCCESSFUL);
        });
        log.info("Completed backup <{}>", job.getAction().getBackupName());
    }

    @Override
    public boolean isJobFinished() {
        return true;
    }

    @Override
    public boolean isStageSuccessful() {
        return true;
    }

    @Override
    public double getProgressPercentage() {
        return 1.0;
    }

    @Override
    protected JobStage<CreateBackupJob> getNextStageSuccess() {
        return this;
    }

    @Override
    protected int getStageOrder() {
        return 4;
    }

    @Override
    public JobStageName getStageName() {
        return JobStageName.COMPLETE;
    }
}
