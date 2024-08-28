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

package com.ericsson.adp.mgmt.backupandrestore.agent.state;

import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_BRO_GRANULAR_ENDTIME;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_BRO_GRANULAR_STAGE_DURATION_SECONDS;

import java.time.OffsetDateTime;

import com.ericsson.adp.mgmt.backupandrestore.SpringContext;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentInputStream;
import com.ericsson.adp.mgmt.backupandrestore.backup.SoftwareVersion;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.JobWithStages;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress;
import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;
import com.ericsson.adp.mgmt.control.AgentControl;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Represents the state of the agent, as seen by the orchestrator.
 */
public interface AgentState {

    String ACTION = "action";
    String ACTION_ID = "action_id";
    String BACKUP_TYPE = "backup_type";
    String STAGE = "stage";
    String AGENT = "agent";
    String STATUS = "status";

    /**
     * Processes message sent from the agent.
     *
     * @param message
     *            sent from the agent.
     * @return new state, consequence of processing the message.
     */
    AgentStateChange processMessage(final AgentControl message);

    /**
     * Gets agent id.
     *
     * @return agent id.
     */
    String getAgentId();

    /**
     * Gets agent's API version.
     *
     * @return agent's API version.
     */
    ApiVersion getApiVersion();

    /**
     * Gets agent scope.
     * @return scope.
     */
    String getScope();

    /**
     * Gets agent's software version.
     *
     * @return software version.
     */
    SoftwareVersion getSoftwareVersion();

    /**
     * Steps to do upon error
     */
    default void handleClosedConnection() {
        //Not needed
    }

    /**
     * Resets state to Recognized State.
     * @return recognized state.
     */
    RecognizedState resetState();

    /**
     * Update bro.granular.duration.seconds metric
     * @param agentId the agent id
     * @param agentProgress states the progress of the agent.
     * @param stageStartTime start time of the stage of the agent.
     * @param job responsible for doing backup or restore.
     */
    default void updateGranularDurationsSeconds(final String agentId, final Progress agentProgress
            , final OffsetDateTime stageStartTime, final JobWithStages<?> job) {

        long duration;

        if (agentProgress.equals(Progress.SUCCESSFUL)) {
            duration = OffsetDateTime.now().toEpochSecond() - stageStartTime.toEpochSecond();
        } else {
            duration = 0;
        }

        SpringContext.getBean(MeterRegistry.class).ifPresent(registry -> {
            registry.find(METRIC_BRO_GRANULAR_STAGE_DURATION_SECONDS.identification()).tag(BACKUP_TYPE, job.getBackupManagerId())
                    .tag(STAGE, job.getJobStage().getStageName().name())
                    .tag(AGENT, agentId)
                    .tag(BACKUP_TYPE, job.getBackupManagerId())
                    .tag(ACTION, job.getAction().getName().toString())
                    .gauges().forEach(registry::remove);

            METRIC_BRO_GRANULAR_STAGE_DURATION_SECONDS.unRegister();
            Gauge.builder(METRIC_BRO_GRANULAR_STAGE_DURATION_SECONDS.identification(), () -> duration)
                    .tags(
                            AGENT, agentId,
                            ACTION, job.getAction().getName().toString(),
                            ACTION_ID, job.getActionId(),
                            BACKUP_TYPE, job.getBackupManagerId(),
                            STAGE, job.getJobStage().getStageName().name())
                    .description(METRIC_BRO_GRANULAR_STAGE_DURATION_SECONDS.description())
                    .register(registry);;
        });
    }

    /**
     * Creates bro.granular.end.time metric.
     * @param job responsible for doing backup or restore.
     * @param agentId the agent id
     * @param agentProgress states the progress of the agent.
     */
    default void createGranularEndTime(final JobWithStages<?> job, final String agentId, final Progress agentProgress) {
        final OffsetDateTime endTime = OffsetDateTime.now();

        SpringContext.getBean(MeterRegistry.class).ifPresent(registry -> {
            registry.find(METRIC_BRO_GRANULAR_ENDTIME.identification())
                    .tag(STAGE, job.getJobStage().getStageName().name())
                    .tag(AGENT, agentId)
                    .tag(BACKUP_TYPE, job.getBackupManagerId())
                    .tag(ACTION, job.getAction().getName().toString())
                    .tag(STATUS, agentProgress.name())
                    .gauges().forEach(registry::remove);

            METRIC_BRO_GRANULAR_ENDTIME.unRegister();
            Gauge.builder(METRIC_BRO_GRANULAR_ENDTIME.identification(), endTime::toEpochSecond)
                    .tags(
                            AGENT, agentId,
                            ACTION, job.getAction().getName().toString(),
                            ACTION_ID, job.getActionId(),
                            BACKUP_TYPE, job.getBackupManagerId(),
                            STAGE, job.getJobStage().getStageName().name(),
                            STATUS, agentProgress.name())
                    .description(METRIC_BRO_GRANULAR_ENDTIME.description())
                    .register(registry);
        });
    }

    /**
     * Triggers backup preparation.
     * @param inputStream used to trigger backup.
     * @param job responsible for doing backup.
     * @return new state, consequence of triggering a backup.
     */
    default AgentStateChange prepareForBackup(final AgentInputStream inputStream, final CreateBackupJob job) {
        throw new UnsupportedOperationException(getErrorMessage("prepare for backup"));
    }

    /**
     * Triggers backup execution.
     * @param inputStream used to trigger backup.
     * @return new state, consequence of triggering a backup.
     */
    default AgentStateChange executeBackup(final AgentInputStream inputStream) {
        throw new UnsupportedOperationException(getErrorMessage("execute backup"));
    }

    /**
     * Triggers backup post action.
     * @param inputStream used to trigger backup.
     * @return new state, consequence of triggering a backup.
     */
    default AgentStateChange executeBackupPostAction(final AgentInputStream inputStream) {
        throw new UnsupportedOperationException(getErrorMessage("execute backup post action"));
    }

    /**
     * Triggers restore preparation.
     * @param inputStream used to trigger restore.
     * @param job responsible for doing restore.
     * @return new state, consequence of triggering a restore.
     */
    default AgentStateChange prepareForRestore(final AgentInputStream inputStream, final RestoreJob job) {
        throw new UnsupportedOperationException(getErrorMessage("prepare for restore"));
    }

    /**
     * Triggers restore execution.
     * @param inputStream used to trigger restore.
     * @return new state, consequence of triggering a restore.
     */
    default AgentStateChange executeRestore(final AgentInputStream inputStream) {
        throw new UnsupportedOperationException(getErrorMessage("execute restore"));
    }

    /**
     * Triggers restore post action.
     * @param inputStream used to trigger restore.
     * @return new state, consequence of triggering a restore.
     */
    default AgentStateChange executeRestorePostAction(final AgentInputStream inputStream) {
        throw new UnsupportedOperationException(getErrorMessage("execute restore post action"));
    }

    /**
     * Provides an error message of the form:
     * Unable to " + errorScope + " because Agent <demoAgent> is in state <someStateClass>"
     *
     * @param errorScope the scope where the error occurred
     * @return fullErrorMessage
     */
    default String getErrorMessage(final String errorScope) {
        return "Unable to " + errorScope + " because Agent <" + this.getAgentId() + "> is in state <" + getClass().getSimpleName() + ">";
    }

    /**
     * Cancels current action.
     * @param inputStream  user to trigger cancellation.
     * @return new state, consequence of canceling action.
     */
    AgentStateChange cancelAction(AgentInputStream inputStream);

}
