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
import java.util.stream.Collectors;

import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.exception.TimedOutHousekeepingException;
import com.ericsson.adp.mgmt.backupandrestore.job.HousekeepingJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;

/**
 * Stage in which housekeeping is executed.
 */
public class ExecutingHousekeepingJobStage extends HousekeepingJobStage {

    private final AtomicBoolean isSuccessful = new AtomicBoolean();

    /**
     * Constructor covering the elements required for housekeeping
     *
     * @param agents
     *            Agents involved in the housekeeping
     *            Job to execute
     * @param job
     *           job containing the required to execute housekeeping tasks
     * @param notificationService
     *            service used for agent notifications
     */
    public ExecutingHousekeepingJobStage(final List<Agent> agents, final HousekeepingJob job, final NotificationService notificationService) {
        super(agents, job, notificationService);
        isSuccessful.set(true);
    }

    /**
     * Verifies backup manager maximum number of backups and autodelete.
     * If maximum number of backups are reached housekeeping is executed.
     */
    @Override
    protected void handleTrigger() {
        String backupsTobeRemoved;
        if (job.isMaxNumberBackups()) {
            try {
                final List<String> backupsToRemove = job.executeHousekeeping();
                isSuccessful.set(true);
                backupsTobeRemoved = "Deleted Backups: <" + backupsToRemove.stream().collect(Collectors.joining(",")) + ">";
            } catch (InterruptedException | TimedOutHousekeepingException e ) {
                backupsTobeRemoved = "Interrupted";
                job.getAction().setResult(ResultType.FAILURE);
                isSuccessful.set(false);
                Thread.currentThread().interrupt();
            }
        } else {
            backupsTobeRemoved = "";
            job.getAction().setResult(ResultType.SUCCESS);
            isSuccessful.set(true);
        }
        job.getAction().setAdditionalInfo(backupsTobeRemoved);
        setStageFinished(true);
        moveToNextStage();
    }

    @Override
    protected JobStage<HousekeepingJob> getNextStageSuccess() {
        return new PostActionHousekeepingJobStage(agents, job, housekeepingNotifs);
    }

    @Override
    protected JobStage<HousekeepingJob> getNextStageFailure() {
        return new FailedHousekeepingJobStage(agents, job, housekeepingNotifs);
    }

    @Override
    public boolean isStageSuccessful() {
        return isSuccessful.get();
    }

    @Override
    public boolean isJobFinished() {
        return isStageFinished();
    }

    @Override
    protected int getStageOrder() {
        return 1;
    }

    @Override
    public JobStageName getStageName() {
        return JobStageName.EXECUTION;
    }

}
