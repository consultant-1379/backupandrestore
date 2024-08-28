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
package com.ericsson.adp.mgmt.backupandrestore.agent;

import com.ericsson.adp.mgmt.backupandrestore.agent.state.AgentState;
import com.ericsson.adp.mgmt.backupandrestore.agent.state.AgentStateChange;
import com.ericsson.adp.mgmt.backupandrestore.agent.state.UnrecognizedState;
import com.ericsson.adp.mgmt.backupandrestore.backup.SoftwareVersion;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;
import com.ericsson.adp.mgmt.control.AgentControl;
import io.grpc.StatusRuntimeException;

/**
 * Wraps connection to an agent and its information.
 */
public class Agent {

    private final AgentInputStream inputStream;
    private final AgentRepository agentRepository;
    private AgentState state;

    // This is set from AgentOutputStream which listens to message from the agent
    private boolean isConnectionCancelled;

    /**
     * Creates agent with its channel.
     * @param inputStream channel to communicate with agent.
     * @param agentRepository access to agentRepository.
     * @param idValidator to validate agentId.
     */
    public Agent(final AgentInputStream inputStream, final AgentRepository agentRepository, final IdValidator idValidator) {
        this.inputStream = inputStream;
        this.agentRepository = agentRepository;
        state = new UnrecognizedState(idValidator);
    }

    /**
     * Process message sent from agent.
     *
     * @param message
     *            sent from agent.
     */
    public void processMessage(final AgentControl message) {
        changeState(state.processMessage(message));
    }

    public String getAgentId() {
        return state.getAgentId();
    }

    public String getScope() {
        return state.getScope();
    }

    public String getScopeWithDefault() {
        return state.getScope().isEmpty() ?
                "DEFAULT" : "DEFAULT;" + state.getScope();
    }

    public SoftwareVersion getSoftwareVersion() {
        return state.getSoftwareVersion();
    }

    public AgentState getState() {
        return state;
    }

    public ApiVersion getApiVersion() {
        return state.getApiVersion();
    }

    /**
     * Set connection state
     * @param connectionCancelled if connection between agent and BRO was cancelled
     * */
    public void setConnectionCancelled(final boolean connectionCancelled) {
        isConnectionCancelled = connectionCancelled;
    }

    /**
     * Triggers backup preparation.
     * @param job responsible for doing backup.
     */
    public void prepareForBackup(final CreateBackupJob job) {
        changeState(state.prepareForBackup(inputStream, job));
    }

    /**
     * Triggers backup execution.
     */
    public void executeBackup() {
        changeState(state.executeBackup(inputStream));
    }

    /**
     * Triggers backup post action
     */
    public void executeBackupPostAction() {
        changeState(state.executeBackupPostAction(inputStream));
    }

    /**
     * Triggers restore preparation.
     * @param job responsible for doing restore.
     */
    public void prepareForRestore(final RestoreJob job) {
        changeState(state.prepareForRestore(inputStream, job));
    }

    /**
     * Triggers restore execution.
     */
    public void executeRestore() {
        changeState(state.executeRestore(inputStream));
    }

    /**
     * Triggers restore post actions.
     */
    public void executeRestorePostAction() {
        changeState(state.executeRestorePostAction(inputStream));
    }

    /**
     * Completes whatever action agent is participating in.
     */
    public void finishAction() {
        state = state.resetState();
    }

    /**
     * Cancels whatever action agent is participating in.
     */
    public void cancelAction() {
        changeState(state.cancelAction(inputStream));
    }

    /**
     * Acknowledges Agent registration message.
     */
    public void sendAcknowledgeRegistrationMessage() {
        inputStream.acknowledge();
    }

    /**
     * Closes agent connection and clean up.
     */
    public void closeConnection() {
        inputStream.close();
        state.handleClosedConnection();
        cleanup();
    }

    /**
     * Closes agent connection due to error and clean up.
     *
     * @param exception
     *            reason for closing
     */
    public void closeConnection(final StatusRuntimeException exception) {
        inputStream.close(exception);
        state.handleClosedConnection();
        cleanup();
    }

    /**
     * Checks if connection is cancelled
     *
     * @return isConnectionCancelled
     */
    public boolean isConnectionCancelled() {
        return isConnectionCancelled;
    }

    private void cleanup() {
        agentRepository.removeAgent(this);
    }

    private void changeState(final AgentStateChange stateChange) {
        state = stateChange.getNextState();
        stateChange.postAction(this, agentRepository);
    }

}
