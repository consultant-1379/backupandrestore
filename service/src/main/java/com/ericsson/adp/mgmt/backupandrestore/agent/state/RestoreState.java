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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress;
import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentInputStream;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.AgentMessageType;
import com.ericsson.adp.mgmt.control.Register;

import java.time.OffsetDateTime;

/**
 * Represents a registered agent who is participating in a restore.
 */
public abstract class RestoreState extends RecognizedState {

    private static final Logger log = LogManager.getLogger(RestoreState.class);
    protected final RestoreJob job;
    private final OffsetDateTime stageStartTime;

    /**
     * Creates state based on agent's registration information.
     * @param registrationInformation agent's information.
     * @param job responsible for doing restore.
     */
    public RestoreState(final Register registrationInformation, final RestoreJob job) {
        super(registrationInformation);
        this.job = job;
        this.stageStartTime = OffsetDateTime.now();
    }

    @Override
    public AgentStateChange processMessage(final AgentControl message) {
        if (!isRestoreStageCompleteMessage(message)) {
            return new AgentStateChange(this);
        }
        log.info("Received Stage Complete message <{}> from agent <{}>", message, getAgentId());

        final boolean isSucceed = message.getStageComplete().getSuccess();

        updateGranularDurationsSeconds(getAgentId(), isSucceed ? Progress.SUCCESSFUL : Progress.FAILED, stageStartTime, job);
        createGranularEndTime(job, getAgentId(), isSucceed ? Progress.SUCCESSFUL : Progress.FAILED);

        return new AgentStateChange(this, () -> job.updateProgress(getAgentId(), message.getStageComplete()));
    }

    @Override
    public AgentStateChange prepareForBackup(final AgentInputStream inputStream, final CreateBackupJob job) {
        throw new UnsupportedOperationException(
                "Unable to prepare for backup because Agent <" + getAgentId() + "> is already participating in a restore");
    }

    @Override
    public AgentStateChange prepareForRestore(final AgentInputStream inputStream, final RestoreJob job) {
        throw new UnsupportedOperationException(
                "Unable to prepare for restore because Agent <" + getAgentId() + "> is already participating in a restore");
    }

    @Override
    public AgentStateChange cancelAction(final AgentInputStream inputStream) {
        return new AgentStateChange(new CancelingActionState(registrationInformation, job),
            () -> inputStream.cancelAction(Action.RESTORE));
    }

    @Override
    public RecognizedState resetState() {
        throw new UnsupportedOperationException("Unable to reset state because Agent <" +
                this.getAgentId() + "> is in state " + getClass().getSimpleName());
    }

    @Override
    public void handleClosedConnection() {
        job.handleAgentDisconnecting(getAgentId());
    }

    /**
     * Verifies if message is a stage complete for restore.
     * @param message to be checked
     * @return true if it is
     */
    protected boolean isRestoreStageCompleteMessage(final AgentControl message) {
        return Action.RESTORE.equals(message.getAction()) && AgentMessageType.STAGE_COMPLETE.equals(message.getAgentMessageType());
    }
}
