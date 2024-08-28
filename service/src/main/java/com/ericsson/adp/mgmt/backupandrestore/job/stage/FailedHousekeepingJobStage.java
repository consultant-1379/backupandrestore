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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.HousekeepingJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;

/**
 * Final stage for housekeeping, indicating it failed.
 */
public class FailedHousekeepingJobStage extends HousekeepingJobStage {

    private static final Logger log = LogManager.getLogger(FailedHousekeepingJobStage.class);

    /**
     * Creates Stage.
     *
     * @param agents
     *            participating in action.
     * @param job
     *            owner of stage.
     * @param notificationService
     *            to send notifications.
     */

    public FailedHousekeepingJobStage(final List<Agent> agents, final HousekeepingJob job, final NotificationService notificationService) {
        super(agents, job, notificationService);
    }

    @Override
    protected void handleTrigger() {
        log.warn("Failed Housekeeping <{}>", job.getBackupManagerId());
        setStageFinished(true);
    }

    @Override
    public JobStage<HousekeepingJob> moveToFailedStage() {
        return this;
    }

    @Override
    public JobStage<HousekeepingJob> moveToNextStage() {
        return this;
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
    protected JobStage<HousekeepingJob> getNextStageSuccess() {
        return this;
    }

    @Override
    protected JobStage<HousekeepingJob> getNextStageFailure() {
        return this;
    }

    @Override
    protected int getStageOrder() {
        return 3;
    }

    @Override
    public JobStageName getStageName() {
        return JobStageName.FAIL;
    }

}
