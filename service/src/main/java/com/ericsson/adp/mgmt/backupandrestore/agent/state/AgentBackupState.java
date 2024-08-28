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
package com.ericsson.adp.mgmt.backupandrestore.agent.state;

import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.util.OSUtils.sleep;
import static com.ericsson.adp.mgmt.control.AgentMessageType.STAGE_COMPLETE;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentInputStream;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.Register;

import java.time.OffsetDateTime;

/**
 * Represents a registered agent who is participating in a backup.
 */
public abstract class AgentBackupState extends RecognizedState {

    private static final Logger log = LogManager.getLogger(AgentBackupState.class);
    protected final CreateBackupJob job;
    private final OffsetDateTime stageStartTime;

    /**
     * Creates state based on agent's registration information.
     * @param registrationInformation agent's information.
     * @param job responsible for doing backup.
     */
    public AgentBackupState(final Register registrationInformation, final CreateBackupJob job) {
        super(registrationInformation);
        this.job = job;
        this.stageStartTime = OffsetDateTime.now();
    }

    @Override
    public AgentStateChange processMessage(final AgentControl message) {
        if (!isBackupStageCompleteMessage(message)) {
            return new AgentStateChange(this);
        }
        log.info("Received Stage Complete message <{}> from agent <{}>", message, getAgentId());

        final boolean isSucceed = message.getStageComplete().getSuccess();

        updateGranularDurationsSeconds(getAgentId(), isSucceed ? Progress.SUCCESSFUL : Progress.FAILED, stageStartTime, job);
        createGranularEndTime(job, getAgentId(), isSucceed ? Progress.SUCCESSFUL : Progress.FAILED);

        sleep(200);
        return new AgentStateChange(this, () -> job.updateProgress(getAgentId(), message.getStageComplete()));
    }

    @Override
    public AgentStateChange prepareForRestore(final AgentInputStream inputStream, final RestoreJob job) {
        throw new UnsupportedOperationException("Unable to prepare for restore as agent <" + getAgentId() + "> is already participating in a backup");
    }

    @Override
    public AgentStateChange prepareForBackup(final AgentInputStream inputStream, final CreateBackupJob job) {
        throw new UnsupportedOperationException("Unable to prepare for backup as agent <" + getAgentId() + "> is already participating in a backup");
    }

    @Override
    public void handleClosedConnection() {
        job.handleAgentDisconnecting(getAgentId());
    }

    @Override
    public AgentStateChange cancelAction(final AgentInputStream inputStream) {
        return new AgentStateChange(new CancelingActionState(registrationInformation, job), () -> inputStream.cancelAction(BACKUP));
    }

    @Override
    public RecognizedState resetState() {
        throw new UnsupportedOperationException("Unable to reset state because Agent " + getAgentId() + " is in state " + getClass().getSimpleName());
    }

    /**
     * Verifies if message is a stage complete for backup.
     * @param message to be checked
     * @return true if it is
     */
    protected boolean isBackupStageCompleteMessage(final AgentControl message) {
        return BACKUP.equals(message.getAction()) && STAGE_COMPLETE.equals(message.getAgentMessageType());
    }

}
