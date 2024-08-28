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
import java.util.concurrent.atomic.AtomicBoolean;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.HousekeepingJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;

/**
 * Represents a stage for housekeeping.
 */
public abstract class HousekeepingJobStage extends JobStage<HousekeepingJob> {

    protected final NotificationService housekeepingNotifs;
    private final AtomicBoolean isStageCompleted = new AtomicBoolean();

    /**
     * Constructor to setup the housekeeping job
     *
     * @param agents
     *            to be notified
     * @param job
     *            to be executed
     * @param housekeepingNotificationService
     *            mechanism used for notification
     */
    public HousekeepingJobStage(final List<Agent> agents, final HousekeepingJob job, final NotificationService housekeepingNotificationService) {
        super(agents, job, housekeepingNotificationService);
        this.housekeepingNotifs = housekeepingNotificationService;
        isStageCompleted.set(false);
    }

    /**
     * Request the next stage and trigger its execution
     * @return next Job Stage
     */
    public JobStage<HousekeepingJob> moveToNextStage() {
        JobStage<HousekeepingJob> nextJob = changeStages();
        if (nextJob.equals(this)) {
            nextJob = getNextStageFailure();
        }
        nextJob.trigger();
        return nextJob;
    }

    @Override
    public JobStage<HousekeepingJob> moveToFailedStage() {
        final FailedHousekeepingJobStage failedStage = new FailedHousekeepingJobStage(agents, job, housekeepingNotifs);
        failedStage.trigger();
        return failedStage;
    }

    @Override
    protected int getNumberOfNonFinalStages() {
        return 2;
    }

    @Override
    protected boolean isStageFinished() {
        return isStageCompleted.get();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    /**
     * Set the current stage status to determine if it finished or not
     * @param isFinished booelan values indicating if it finished or not.
     */
    protected void setStageFinished(final boolean isFinished) {
        isStageCompleted.set(isFinished);
    }
}
