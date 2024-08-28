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

import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.agent.exception.AgentRegistrationException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidIdException;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.AgentMessageType;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * Stream to listen to messages from an agent.
 */
public class AgentOutputStream implements StreamObserver<AgentControl> {

    private static final Logger log = LogManager.getLogger(AgentOutputStream.class);
    private static final String UNKNOWN_AGENT_ID = "Unkown";

    private final Agent agent;

    /**
     * Creates streamObserver with associated agent.
     * @param agent - origin of stream.
     */
    public AgentOutputStream(final Agent agent) {
        this.agent = agent;
    }

    @Override
    public void onNext(final AgentControl message) {
        try {
            this.agent.processMessage(message);
            if (isRegistrationMessage(message) && agent.getApiVersion() == ApiVersion.API_V4_0) {
                log.info("Sending Registration Acknowledge Message to Agent {}", agent.getAgentId());
                agent.sendAcknowledgeRegistrationMessage();
            }
        } catch (final AgentRegistrationException e) {
            log.error("Failed to register agent with message <{}>", message, e);
            this.agent.closeConnection(e.getStatus().withDescription(e.getMessage()).withCause(e).asRuntimeException());
        } catch (final InvalidIdException e) {
            log.error("Failed to register agent with message <{}>", message, e);
            this.agent.closeConnection(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asRuntimeException());
        } catch (final Exception e) {
            log.error("Processing message <{}> resulted in error", message, e);
        }
    }

    private boolean isRegistrationMessage(final AgentControl message) {
        return Action.REGISTER.equals(message.getAction()) &&
                AgentMessageType.REGISTER.equals(message.getAgentMessageType());
    }

    @Override
    public void onCompleted() {
        log.info("Agent <{}> closed connection", getAgentId());
        agent.setConnectionCancelled(true);
        closeAgentConnection();
    }

    @Override
    public void onError(final Throwable throwable) {
        log.error("Agent <{}> closed connection due to error", getAgentId(), throwable);
        agent.setConnectionCancelled(true);
        closeAgentConnection();
    }

    private void closeAgentConnection() {
        this.agent.closeConnection();
    }

    private String getAgentId() {
        try {
            return this.agent.getAgentId();
        } catch (final Exception e) {
            return UNKNOWN_AGENT_ID;
        }
    }

}
