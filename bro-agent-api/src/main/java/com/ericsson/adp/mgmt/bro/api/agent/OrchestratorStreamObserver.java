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

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

/**
 * Listens to messages from orchestrator and redirects them to agent.
 */
public class OrchestratorStreamObserver implements StreamObserver<OrchestratorControl> {

    private static final Logger log = LogManager.getLogger(OrchestratorStreamObserver.class);
    private static final int MAXIMUM_NUMBER_OF_ATTEMPTS_TO_REGISTER_AGAIN = 20;

    private final Agent agent;
    private final int attemptsToRegisterAgain;

    /**
     * Creates stream observer that redirects messages to agent.
     * @param agent - to receive messages.
     */
    public OrchestratorStreamObserver(final Agent agent) {
        this (agent, 0);
    }

    /**
     * Creates stream observer that redirects messages to agent.
     * @param agent - to receive messages.
     * @param attemptsToRegisterAgain - how many times it has tried to register again
     */
    public OrchestratorStreamObserver(final Agent agent, final int attemptsToRegisterAgain) {
        this.agent = agent;
        this.attemptsToRegisterAgain = attemptsToRegisterAgain;
    }

    @Override
    public void onNext(final OrchestratorControl message) {
        final Optional<String> previousMessageReceived = agent.getLastMessageTypeReceived();
        final OrchestratorMessageType orchestratorMessageType = message.getOrchestratorMessageType();
        setLastMessageTypeReceived(Optional.of(orchestratorMessageType.toString()));
        if (message.hasRegisterAcknowledge()) {
            logRegistrationAck(message, previousMessageReceived);
        } else {
            logReceivedMessageTypeAndAction(message);
            log.debug("The Message details are <{}>", message);
            agent.process(message);
        }
    }

    @Override
    public void onCompleted() {
        log.info("Channel to Orchestrator was closed");
        this.agent.register(new OrchestratorStreamObserver(agent));
    }

    @Override
    public void onError(final Throwable throwable) {
        log.info("Channel to Orchestrator was closed due to error: {}", throwable != null ? throwable.getMessage() : "");
        setLastMessageTypeReceived(Optional.of("ERROR"));
        if (isV4ApiNotSupportedByOrchestrator(throwable)) {
            registerUsingApiVersionV3();
        } else if (shouldWaitAndRegisterAgain(throwable)) {
            sleep(30000);
            log.info("The agent is attempting to register again. <{} of {}> retries.", attemptsToRegisterAgain,
                    MAXIMUM_NUMBER_OF_ATTEMPTS_TO_REGISTER_AGAIN);
            this.agent.register(new OrchestratorStreamObserver(agent, attemptsToRegisterAgain + 1));
        } else if (shouldImmediatelyTryToRegisterAgain(throwable)) {
            agent.setGrpcApiVersion(GrpcApiVersion.V4);
            this.agent.register(new OrchestratorStreamObserver(agent));
        } else {
            log.info("Not attempting to register again after {} retries", attemptsToRegisterAgain, throwable);
        }
    }

    private void logRegistrationAck(final OrchestratorControl message, final Optional<String> lastMessageReceived) {
        if (lastMessageReceived.isEmpty() || lastMessageReceived.get().equals("ERROR")) {
            logReceivedMessageTypeAndAction(message);
        }
    }

    private void logReceivedMessageTypeAndAction(final OrchestratorControl message) {
        log.info("Agent <{}> received a <{}> message from orchestrator for an action of <{}>", agent.getAgentId(),
                message.getOrchestratorMessageType(), message.getAction());
    }

    private void registerUsingApiVersionV3() {
        log.warn("The registration failed as the orchestrator does not support"
                + " agent GRPC API version 4.0");
        agent.setGrpcApiVersion(GrpcApiVersion.V3);
        log.info("The agent will attempt to register using the agent GRPC API version 3.0");
        this.agent.register(new OrchestratorStreamObserver(agent));
    }

    private boolean isV4ApiNotSupportedByOrchestrator(final Throwable throwable) {
        if (throwable instanceof StatusRuntimeException) {
            final StatusRuntimeException exception = (StatusRuntimeException) throwable;
            return hasInvalidArgumentCode(exception) && agent.isGrpcApiVersion4()
                    && exception.getStatus().getDescription()
                            .contains("Invalid api version. Only [2.0, 3.0] are supported.");
        }
        return false;
    }

    private boolean shouldWaitAndRegisterAgain(final Throwable throwable) {
        if (throwable instanceof StatusRuntimeException && !isUnauthenticated((StatusRuntimeException) throwable)) {
            return isAgentIdAlreadyRegistered((StatusRuntimeException) throwable) &&
                    attemptsToRegisterAgain < MAXIMUM_NUMBER_OF_ATTEMPTS_TO_REGISTER_AGAIN;
        }
        return false;
    }

    private boolean isAgentIdAlreadyRegistered(final StatusRuntimeException exception) {
        return Status.ALREADY_EXISTS.getCode().equals(exception.getStatus().getCode());
    }

    private boolean isUnauthenticated(final StatusRuntimeException exception) {
        return Status.UNAUTHENTICATED.getCode().equals(exception.getStatus().getCode());
    }

    private boolean shouldImmediatelyTryToRegisterAgain(final Throwable throwable) {
        if (throwable instanceof StatusRuntimeException && !isUnauthenticated((StatusRuntimeException) throwable)) {
            return !hasInvalidArgumentCode((StatusRuntimeException) throwable) && !isAgentIdAlreadyRegistered((StatusRuntimeException) throwable);
        }
        return true;
    }

    private boolean hasInvalidArgumentCode(final StatusRuntimeException exception) {
        return Status.INVALID_ARGUMENT.getCode().equals(exception.getStatus().getCode());
    }

    private void sleep(final int time) {
        try {
            Thread.sleep(time);
        } catch (final Exception e) {
            log.error("Failed to sleep", e);
        }
    }

    /**
     * Set the type of message received by the OrchestratorStreamObserver
     * @param orchestratorMessageType the type of message received from the orchestrator
     */
    protected void setLastMessageTypeReceived(final Optional<String> orchestratorMessageType) {
        this.agent.setLastMessageTypeReceived(orchestratorMessageType);
    }
}
