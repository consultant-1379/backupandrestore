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
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import java.util.List;

/**
 * Represents a stage for Backup.
 */
public abstract class BackupJobStage extends JobStage<CreateBackupJob> {

    /**
     * Creates Stage.
     * @param agents participating in action.
     * @param job owner of stage.
     * @param notificationService to send notifications.
     */
    public BackupJobStage(final List<Agent> agents, final CreateBackupJob job, final NotificationService notificationService) {
        super(agents, job, notificationService);
    }

    @Override
    public JobStage<CreateBackupJob> moveToFailedStage() {
        final FailedBackupJobStage failedStage = new FailedBackupJobStage(agents, job, notificationService, getProgressPercentage(), agentProgress);
        failedStage.trigger();
        return failedStage;
    }

    @Override
    public void receiveNewFragment(final String agentId, final String fragmentId) {
        throw new BackupServiceException("Backup data channel opened for fragment <"
                + fragmentId + "> by agent <" + agentId + "> during " + this.getClass().getSimpleName());
    }

    @Override
    protected JobStage<CreateBackupJob> getNextStageFailure() {
        return new FailedBackupJobStage(agents, job, notificationService, getProgressPercentage(), agentProgress);
    }

    @Override
    protected int getNumberOfNonFinalStages() {
        return 3;
    }
}
