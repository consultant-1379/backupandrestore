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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a stage for Restore.
 */
public abstract class RestoreJobStage extends JobStage<RestoreJob> {

    /**
     * Creates Stage.
     * @param agents participating in action.
     * @param job owner of stage.
     * @param notificationService to send notifications.
     */
    public RestoreJobStage(final List<Agent> agents, final RestoreJob job, final NotificationService notificationService) {
        super(agents, job, notificationService);
    }

    @Override
    public JobStage<RestoreJob> moveToFailedStage() {
        return new FailedRestoreJobStage(agents, job, notificationService, getProgressPercentage(), agentProgress);
    }

    @Override
    public String toString() {
        final Map<Object, Object> progress = agentProgress.entrySet().stream().
                collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getProgress()));
        return this.getClass().getSimpleName() + " - " + progress;
    }

    @Override
    protected JobStage<RestoreJob> getNextStageFailure() {
        return new FailedRestoreJobStage(agents, job, notificationService, getProgressPercentage(), agentProgress);
    }

    @Override
    protected int getNumberOfNonFinalStages() {
        return 3;
    }
}
