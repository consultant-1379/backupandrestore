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

package com.ericsson.adp.mgmt.bro.api.agent;

import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.action.Action.RESTORE;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.bro.api.registration.RegistrationInformation;
import com.ericsson.adp.mgmt.bro.api.registration.RegistrationMessageFactory;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.AgentMessageType;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.StageComplete;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.data.RestoreData;

import io.grpc.stub.StreamObserver;

/**
 * Represents agent, that listens to messages from orchestrator and replies back.
 */
public class Agent {
    private static final String AGENT_API_VERSION = "5.9.0";
    private static final Logger log = LogManager.getLogger(Agent.class);

    private final AgentBehavior agentBehavior;
    private final OrchestratorGrpcChannel channel;

    private AgentState state;
    private int secondsToRetryACK = OrchestratorConnectionInformation.DEFAULT_REGISTRATION_ACK_TIMEOUT;
    private GrpcApiVersion grpcApiVersion = GrpcApiVersion.V4;

    private final AtomicReference<Optional<String>> lastMessageTypeReceived = new AtomicReference<>(Optional.empty());

    /**
     * Creates agent with specific behavior.
     * @param agentBehavior specific agent behavior.
     * @param channel to orchestrator.
     */
    protected Agent(final AgentBehavior agentBehavior,
                    final OrchestratorGrpcChannel channel) {
        this.agentBehavior = agentBehavior;
        this.channel = channel;
        this.state = new WaitingForActionState(this);
        log.info("Agent is using the agent api version {}", AGENT_API_VERSION);
    }

    /**
     * Checks a message from the Orchestrator to identify what needs to be done on the agent.
     * @param message A control message from the orchestrator
     *
     */
    protected void process(final OrchestratorControl message) {
        if (isGrpcApiVersion4() && message.hasPreparation() && message.getAction() == RESTORE) {
            // If its Restore Preparation message we will update the state to RestorePreparationState but wait for
            // all fragments to be collected before we move to Execution stage
            if (message.getPreparation().getFragmentList().isEmpty()) {
                changeState(this.state.processMessage(message));
            } else {
                this.state = this.state.processMessage(message);
            }
        } else if (isGrpcApiVersion4() && message.hasFragmentListEntry()) {
            this.state = this.state.processMessage(message);
        } else {
            changeState(this.state.processMessage(message));
        }
    }

    protected boolean isGrpcApiVersion4() {
        return this.grpcApiVersion.equals(GrpcApiVersion.V4);
    }

    /**
     * Send registration message/s to orchestrator.
     *
     * This method has been synchronized for two primary reasons. Firstly, it ensures that only a single
     * thread is able to send registration messages to the orchestrator. Secondly, the method updates and
     * checks the lastMessageReceived flag of the agent which must be an atomic operation.
     *
     * Synchronizing this operation prevents a potential race condition that could lead to unexpected
     * registration retry behavior.
     *
     * @param orchestratorStreamObserver to the stream that is listening to messages from the orchestrator
     */
    protected synchronized void register(final OrchestratorStreamObserver orchestratorStreamObserver) {
        changeState(this.state.cancelAction());
        setLastMessageTypeReceived(Optional.empty());
        log.info("Establishing control channel with Agent<{}>.", getAgentId());
        this.channel.establishControlChannel(orchestratorStreamObserver);
        log.info("Agent <{}> has established a control channel.", getAgentId());
        final AgentControl registrationMessage = getRegistrationMessage();
        if (isGrpcApiVersion4() && secondsToRetryACK > 0) {
            sendRegistrationMessageUntilAcknowledged(registrationMessage);
        } else {
            sendRegistrationMessage(registrationMessage);
        }
    }

    private void sendRegistrationMessageUntilAcknowledged(final AgentControl registrationMessage) {
        final ExecutorService registrationExecutor = Executors.newSingleThreadExecutor();
        try {
            while (!lastMessageTypeReceived.get().isPresent()) {
                final Future<Optional<String>> responseCheck = registrationExecutor.submit(
                    () -> sendControlMessageAndWaitForResponse(registrationMessage));
                final Optional<String> response = waitForOrchestratorResponse(responseCheck);
                if (response.isPresent()) {
                    log.info("The Agent received a response <{}> from the Orchestrator.", response.get());
                    break;
                }
                log.warn("The Agent did not receive a response from the Orchestrator"
                            + " and will attempt to re-send the registration message.");
            }
        } finally {
            registrationExecutor.shutdown();
            log.debug("The Agent has completed the registration procedure and has shut down its registration executor");
        }
    }

    private void sendRegistrationMessage(final AgentControl registrationMessage) {
        log.info("Sending registration message to Orchestrator: <{}>", registrationMessage);
        this.channel.sendControlMessage(registrationMessage);
        log.info("Registration message sent.");
    }

    private Optional<String> waitForOrchestratorResponse(final Future<Optional<String>> responseCheck) {
        try {
            return responseCheck.get(secondsToRetryACK, TimeUnit.SECONDS);
        } catch (Exception e) {
            return Optional.empty();
        } finally {
            responseCheck.cancel(true);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Optional<String> sendControlMessageAndWaitForResponse(final AgentControl registrationMessage) {
        sendRegistrationMessage(registrationMessage);
        while (lastMessageTypeReceived.get().isEmpty()) {
            if (Thread.currentThread().isInterrupted()) {
                log.debug("The Agent registration execution thread is interrupted");
                break;
            }
            sleep();
        }
        return lastMessageTypeReceived.get();
    }

    /**
     * Execute a backup
     * @param backupExecutionActions actions the agent can execute
     */
    protected void executeBackup(final BackupExecutionActions backupExecutionActions) {
        safelyExecuteAgentBehavior(() -> this.agentBehavior.executeBackup(backupExecutionActions), BACKUP);
    }

    /**
     * Calling this initiates prepare for backup actions.
     * @param backupPreparationActions - the {@link BackupPreparationActions} to initiate.
     */
    public void prepareForBackup(final BackupPreparationActions backupPreparationActions) {
        safelyExecuteAgentBehavior(() -> this.agentBehavior.prepareForBackup(backupPreparationActions), BACKUP);
    }

    /**
     * Calling this initiates post backup actions.
     * @param postBackupActions - the {@link PostBackupActions} to initiate.
     */
    public void postBackup(final PostBackupActions postBackupActions) {
        safelyExecuteAgentBehavior(() -> this.agentBehavior.postBackup(postBackupActions), BACKUP);
    }

    /**
     * Provides stream to send backup data.
     * @return backup stream.
     */
    protected StreamObserver<BackupData> getBackupStream() {
        return this.channel.getBackupStream();
    }

    /**
     * Provides iterator to get restore data.
     * @param metadata for the restore
     * @return iterator which provides the restore data.
     */
    protected Iterator<RestoreData> getRestoreDataIterator(final Metadata metadata) {
        return this.channel.getRestoreDataIterator(metadata);
    }

    /**
     * Prepares for Restore
     * @param preparationActions restore preparation actions to be performed
     */
    protected void prepareForRestore(final RestorePreparationActions preparationActions) {
        safelyExecuteAgentBehavior(() ->
                this.agentBehavior.prepareForRestore(preparationActions),
                RESTORE);
    }

    /**
     * Executes a Restore
     * @param executionActions restore execution actions to be performed
     */
    protected void executeRestore(final RestoreExecutionActions executionActions) {
        safelyExecuteAgentBehavior(() -> this.agentBehavior.executeRestore(executionActions), RESTORE);
    }

    /**
     * Cancels an action
     * @param cancelActions actions to be performed on cancel
     */
    protected void cancelAction(final CancelActions cancelActions) {
        safelyExecuteAgentBehavior(() -> this.agentBehavior.cancelAction(cancelActions), RESTORE);
    }

    /**
     * Performs actions post Restore
     * @param postActions post restore actions to be performed
     */
    protected void postRestore(final PostRestoreActions postActions) {
        safelyExecuteAgentBehavior(() -> this.agentBehavior.postRestore(postActions), RESTORE);
    }

    /**
     * Sends a stage complete message with the supplied information
     * @param success to indicate if the action was successful
     * @param message to be logged
     * @param action type
     */
    protected void sendStageCompleteMessage(final boolean success, final String message, final Action action) {
        final AgentControl agentMessage = getStageCompleteMessage(message, success, action);
        log.info("Sending Stage Complete message <{}>", agentMessage);
        this.channel.sendControlMessage(agentMessage);
    }

    /**
     * Shuts agent down.
     */
    protected void shutdown() {
        this.channel.shutdown();
    }

    /**
     * Finishes the current action.
     */
    protected void finishAction() {
        changeState(this.state.finishAction());
    }

    protected String getAgentId() {
        return this.agentBehavior.getRegistrationInformation().getAgentId();
    }

    /**
     * Get the GRPC API version used by the agent
     * @return the agent's GRPC API version
     */
    protected GrpcApiVersion getAgentGrpcApiVersion() {
        return this.grpcApiVersion;
    }

    /**
     * Sets the GRPC API version used by the agent
     * @param version the agent's GRPC API version
     */
    protected void setGrpcApiVersion(final GrpcApiVersion version) {
        this.grpcApiVersion = version;
    }

    private AgentControl getRegistrationMessage() {
        final RegistrationInformation registrationInformation = this.agentBehavior.getRegistrationInformation();
        registrationInformation.setApiVersion(this.grpcApiVersion.toString());
        log.info("The agent is using the agent GRPC API version {}", registrationInformation.getApiVersion());
        return RegistrationMessageFactory.getRegistrationMessage(registrationInformation);
    }

    private AgentControl getStageCompleteMessage(final String message, final boolean success, final Action action) {
        final StageComplete stageComplete = StageComplete.newBuilder().setMessage(message).setSuccess(success).build();
        return AgentControl.newBuilder().setAction(action).setAgentMessageType(AgentMessageType.STAGE_COMPLETE).setStageComplete(stageComplete)
                .build();
    }

    private void changeState(final AgentState stateChange) {
        this.state = stateChange;
        this.state.trigger();
    }

    private void safelyExecuteAgentBehavior(final Runnable behavior, final Action action) {
        try {
            behavior.run();
        } catch (final Exception e) {
            log.error("Exception executing agent behavior. This exception should have been caught by the agent", e);
            this.sendStageCompleteMessage(false, "Exception executing agent behavior", action);
        }
    }

    public void setSecondsToRetryACK(final int seconds_to_retry_ack) {
        this.secondsToRetryACK = seconds_to_retry_ack;
    }

    /**
     * Set the type of the last message received by the Agent from the orchestrator
     * @param messageType the type of message received from the orchestrator
     */
    protected void setLastMessageTypeReceived(final Optional<String> messageType) {
        this.lastMessageTypeReceived.set(messageType);
    }

    /**
     * Get the type of the last message received by the Agent from the orchestrator
     * @return The type of message received from the orchestrator
     */
    protected Optional<String> getLastMessageTypeReceived() {
        return lastMessageTypeReceived.get();
    }
}
