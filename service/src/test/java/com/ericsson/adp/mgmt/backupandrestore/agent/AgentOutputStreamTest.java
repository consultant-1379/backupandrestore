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

import static com.ericsson.adp.mgmt.action.Action.REGISTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.util.function.Supplier;

import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;
import com.ericsson.adp.mgmt.control.AgentMessageType;

import org.junit.Before;
import org.junit.Test;
import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.agent.exception.InvalidRegistrationMessageException;
import com.ericsson.adp.mgmt.control.AgentControl;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;


public class AgentOutputStreamTest {

    private AgentStub agentStub;
    private AgentOutputStream streamObserver;

    @Before
    public void setup() {
        this.agentStub = new AgentStub();
        this.streamObserver = new AgentOutputStream(this.agentStub);
    }

    @Test
    public void onNext_registrationMessageWithApiVersionV4_sendsRegistrationAcknowledgeMessage() {
        Agent agentMock = mock(Agent.class);
        streamObserver = new AgentOutputStream(agentMock);
        AgentControl message = AgentControl.newBuilder()
                .setAction(Action.REGISTER)
                .setAgentMessageType(AgentMessageType.REGISTER)
                .build();
        when(agentMock.getApiVersion()).thenReturn(ApiVersion.API_V4_0);
        this.streamObserver.onNext(message);
        verify(agentMock).processMessage(message);
        verify(agentMock).sendAcknowledgeRegistrationMessage();

    }

    @Test
    public void onNext_registrationMessageWithNonV4ApiVersion_doesNotSendRegistrationAckMessage() {
        Agent agentMock = mock(Agent.class);
        streamObserver = new AgentOutputStream(agentMock);
        AgentControl message = AgentControl.newBuilder()
                .setAction(REGISTER)
                .setAgentMessageType(AgentMessageType.REGISTER)
                .build();
        when(agentMock.getApiVersion()).thenReturn(ApiVersion.API_V3_0);
        this.streamObserver.onNext(message);
        verify(agentMock).processMessage(message);
        verify(agentMock, never()).sendAcknowledgeRegistrationMessage();
    }
    
    @Test
    public void onNext_message_agentHandlesMessage() throws Exception {
        this.streamObserver.onNext(AgentControl.newBuilder().setAction(Action.CANCEL).build());

        assertEquals(Action.CANCEL, this.agentStub.getMessage().getAction());
    }

    @Test
    public void onNext_messageWithInvalidRegistrationInformation_sendsErrorBackToAgent() throws Exception {
        this.agentStub.setProcessMessageBehavior(() -> { throw new InvalidRegistrationMessageException(); });

        this.streamObserver.onNext(AgentControl.getDefaultInstance());

        assertEquals(Status.INVALID_ARGUMENT.getCode(), this.agentStub.getException().getStatus().getCode());
        assertTrue(this.agentStub.getException().getCause() instanceof InvalidRegistrationMessageException);
    }

    @Test
    public void onNext_anyMessageThrowsException_logsAndKeepsChannelAlive() throws Exception {
        this.agentStub.setProcessMessageBehavior(() -> { throw new RuntimeException(); });

        this.streamObserver.onNext(AgentControl.getDefaultInstance());

        assertNull(this.agentStub.getException());
    }

    @Test
    public void onCompleted_registeredAgent_removesAgentFromRepository() throws Exception {
        this.streamObserver.onCompleted();

        assertTrue(this.agentStub.closedConnection());
    }

    @Test
    public void onCompleted_unregisteredAgent_removesAgentFromRepository() throws Exception {
        this.agentStub.setAgentIdSupplier(() -> { throw new RuntimeException(); });

        this.streamObserver.onCompleted();

        assertTrue(this.agentStub.closedConnection());
    }

    @Test
    public void onError_registeredAgent_removesAgentFromRepository() throws Exception {
        this.streamObserver.onError(new RuntimeException("Boo"));

        assertTrue(this.agentStub.closedConnection());
    }

    @Test
    public void onError_unregisteredAgent_removesAgentFromRepository() throws Exception {
        this.agentStub.setAgentIdSupplier(() -> { throw new RuntimeException(); });

        this.streamObserver.onError(new RuntimeException("Boo"));

        assertTrue(this.agentStub.closedConnection());
    }

    private class AgentStub extends Agent {

        private AgentControl message;
        private StatusRuntimeException exception;
        private Runnable processMessageBehavior = () -> {};
        private boolean closedConnection;
        private Supplier<String> agentIdSupplier = () -> "1";

        public AgentStub() {
            super(null, null, null);
        }

        @Override
        public void processMessage(final AgentControl message) {
            this.message = message;
            this.processMessageBehavior.run();
        }

        @Override
        public String getAgentId() {
            return agentIdSupplier .get();
        }

        @Override
        public void closeConnection() {
            this.closedConnection = true;
        }

        @Override
        public void closeConnection(final StatusRuntimeException exception) {
            this.exception = exception;
        }

        public AgentControl getMessage() {
            return message;
        }

        public StatusRuntimeException getException() {
            return exception;
        }

        public boolean closedConnection() {
            return closedConnection;
        }

        public void setProcessMessageBehavior(final Runnable processMessageBehavior) {
            this.processMessageBehavior = processMessageBehavior;
        }

        public void setAgentIdSupplier(final Supplier<String> agentIdSupplier) {
            this.agentIdSupplier = agentIdSupplier;
        }

   }

}


