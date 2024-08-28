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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.HousekeepingJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;

/**
 * Validates the next stage to be processed
 * If the Job was triggered by a HOUSEKEEPING action it finishes the job
 * If the Job was triggered by a CREATE_BACKUP or IMPORT action it calls the next stage
 *
 */
public class PostActionHousekeepingJobStage extends HousekeepingJobStage {

    private static final Logger log = LogManager.getLogger(PostActionHousekeepingJobStage.class);
    private final AtomicBoolean isSuccesful = new AtomicBoolean();
    /**
     * Constructor
     * @param agents agents to be notified
     * @param job Housekeeping job with the tasks to be executed
     * @param notificationService Notification service
     */
    public PostActionHousekeepingJobStage(final List<Agent> agents, final HousekeepingJob job, final NotificationService notificationService) {
        super(agents, job, notificationService);
        isSuccesful.set(false);
    }

    @Override
    protected void handleTrigger() {
        log.debug("To include additional action call <{}>", job.getBackupManagerId());
        isSuccesful.set(true);
        setStageFinished(true);
        moveToNextStage();
    }

    @Override
    protected JobStage<HousekeepingJob> getNextStageSuccess() {
        return new CompletedHousekeepingJobStage(agents, job, housekeepingNotifs);
    }

    @Override
    protected JobStage<HousekeepingJob> getNextStageFailure() {
        return new FailedHousekeepingJobStage(agents, job, housekeepingNotifs);
    }

    @Override
    public boolean isJobFinished() {
        return true;
    }

    @Override
    public boolean isStageSuccessful() {
        return isSuccesful.get();
    }
    @Override
    protected int getStageOrder() {
        return 2;
    }

    @Override
    public JobStageName getStageName() {
        return JobStageName.POST_ACTIONS;
    }

}
