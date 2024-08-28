/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.job.stage;

import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.HousekeepingJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;

/**
 * Final stage for Housekeeping, indicating it was successful.
 */
public class CompletedHousekeepingJobStage extends HousekeepingJobStage {

    /**
     * Creates Stage.
     * @param agents participating in action.
     * @param job owner of stage.
     * @param notificationService to send notifications.
     */
    public CompletedHousekeepingJobStage(final List<Agent> agents, final HousekeepingJob job, final NotificationService notificationService) {
        super(agents, job, notificationService);
    }

    @Override
    protected void handleTrigger() {
        job.updateBackupManagerHousekeeping();
        setStageFinished(true);
    }

    @Override
    protected JobStage<HousekeepingJob> getNextStageSuccess() {
        return null;
    }

    @Override
    public boolean isStageSuccessful() {
        return true;
    }

    @Override
    protected int getStageOrder() {
        return 3;
    }

    @Override
    public double getProgressPercentage() {
        return 1.0;
    }

    @Override
    public boolean isJobFinished() {
        return true;
    }

    @Override
    protected JobStage<HousekeepingJob> getNextStageFailure() {
        return this;
    }

    @Override
    public JobStage<HousekeepingJob> moveToNextStage() {
        return this;
    }

    @Override
    public JobStageName getStageName() {
        return JobStageName.COMPLETE;
    }

}
