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

import com.ericsson.adp.mgmt.backupandrestore.agent.AgentInputStream;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.JobWithStages;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.AgentMessageType;
import com.ericsson.adp.mgmt.control.Register;

/**
 * Represents a registered agent who is canceling its current action.
 */
public class CancelingActionState extends RecognizedState {

    private static final Logger log = LogManager.getLogger(CancelingActionState.class);
    private final JobWithStages<?> job;

    /**
     * Creates state.
     * @param registrationInformation agent information.
     * @param job to be canceled.
     */
    public CancelingActionState(final Register registrationInformation, final JobWithStages<?> job) {
        super(registrationInformation);
        this.job = job;
    }

    @Override
    public AgentStateChange processMessage(final AgentControl message) {
        if (!isStageCompleteMessage(message)) {
            return new AgentStateChange(this);
        }
        log.info("Received Stage Complete message <{}> from agent <{}>", message, getAgentId());
        return new AgentStateChange(new RecognizedState(registrationInformation),
            () -> job.updateProgress(getAgentId(), message.getStageComplete()));
    }

    @Override
    public AgentStateChange prepareForBackup(final AgentInputStream inputStream, final CreateBackupJob job) {
        throw new UnsupportedOperationException(
                "Unable to execute backup because Agent <" + getAgentId() + "> is already cancelling its action");
    }

    @Override
    public AgentStateChange prepareForRestore(final AgentInputStream inputStream, final RestoreJob job) {
        throw new UnsupportedOperationException(
                "Unable to prepare for restore because Agent <" + getAgentId() + "> is already cancelling its action");
    }

    @Override
    public void handleClosedConnection() {
        job.handleAgentDisconnecting(getAgentId());
    }

    private boolean isStageCompleteMessage(final AgentControl message) {
        return AgentMessageType.STAGE_COMPLETE.equals(message.getAgentMessageType());
    }

}
