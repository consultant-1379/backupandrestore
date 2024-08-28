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
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import com.ericsson.adp.mgmt.control.RegisterAcknowledge;

import io.grpc.Status;

public class OrchestratorStreamObserverTest {

    private OrchestratorStreamObserver orchestratorStreamObserver;
    private Agent agent;

    @Before
    public void setup() {
        this.agent = createMock(Agent.class);

        this.orchestratorStreamObserver = new OrchestratorStreamObserver(agent);
    }

    @Test
    public void onNext_receivesMessageFromOrchestrator_processMessage() throws Exception {
        expect(agent.getLastMessageTypeReceived()).andReturn(Optional.of(OrchestratorMessageType.REGISTER_ACKNOWLEDGE.toString()));
        this.agent.setLastMessageTypeReceived(Optional.of(OrchestratorMessageType.REQUEST_DEPENDENCIES.toString()));
        expectLastCall();
        expect(agent.getAgentId()).andReturn("id");
        final OrchestratorControl message = OrchestratorControl.newBuilder()
                .setOrchestratorMessageType(OrchestratorMessageType.REQUEST_DEPENDENCIES)
                .build();
        this.agent.process(message);
        expectLastCall();
        replay(this.agent);

        this.orchestratorStreamObserver.onNext(message);
        verify(agent);
    }

    @Test
    public void onNext_receivesAckMessageFromOrchestrator_ackNotReceivedYet_onlyLogMessage() throws Exception {
        expect(agent.getLastMessageTypeReceived()).andReturn(Optional.empty());
        this.agent.setLastMessageTypeReceived(Optional.of(OrchestratorMessageType.REGISTER_ACKNOWLEDGE.toString()));
        expectLastCall();
        // logging calls getAgentId
        expect(agent.getAgentId()).andReturn("id");
        final OrchestratorControl message = OrchestratorControl.newBuilder()
                .setOrchestratorMessageType(OrchestratorMessageType.REGISTER_ACKNOWLEDGE)
                .setRegisterAcknowledge(RegisterAcknowledge.newBuilder().build())
                .build();
        replay(this.agent);

        this.orchestratorStreamObserver.onNext(message);
        verify(agent);
    }

    @Test
    public void onNext_receivesAckMessageFromOrchestrator_previouslyReceivedErrorMessage_onlyLogMessage() throws Exception {
        expect(agent.getLastMessageTypeReceived()).andReturn(Optional.of("ERROR"));
        this.agent.setLastMessageTypeReceived(Optional.of(OrchestratorMessageType.REGISTER_ACKNOWLEDGE.toString()));
        expectLastCall();
        // logging calls getAgentId
        expect(agent.getAgentId()).andReturn("id");
        final OrchestratorControl message = OrchestratorControl.newBuilder()
                .setOrchestratorMessageType(OrchestratorMessageType.REGISTER_ACKNOWLEDGE)
                .setRegisterAcknowledge(RegisterAcknowledge.newBuilder().build())
                .build();
        replay(this.agent);

        this.orchestratorStreamObserver.onNext(message);
        verify(agent);
    }

    @Test
    public void onNext_receivesAckMessageFromOrchestrator_previouslyReceivedAck_noMessageLoggedOrProccessed() throws Exception {
        expect(agent.getLastMessageTypeReceived()).andReturn(Optional.of(OrchestratorMessageType.REGISTER_ACKNOWLEDGE.toString()));
        this.agent.setLastMessageTypeReceived(Optional.of(OrchestratorMessageType.REGISTER_ACKNOWLEDGE.toString()));
        expectLastCall();
        final OrchestratorControl message = OrchestratorControl.newBuilder()
                .setOrchestratorMessageType(OrchestratorMessageType.REGISTER_ACKNOWLEDGE)
                .setRegisterAcknowledge(RegisterAcknowledge.newBuilder().build())
                .build();
        replay(this.agent);

        this.orchestratorStreamObserver.onNext(message);
        verify(agent);
    }

    @Test
    public void onCompleted_orchestratorEndsConnection_triesToRegisterAgain() throws Exception {
        agent.register(anyObject(OrchestratorStreamObserver.class));
        expectLastCall();
        replay(this.agent);

        this.orchestratorStreamObserver.onCompleted();

        verify(this.agent);
    }

    @Test
    public void onError_receivesInvalidArgumentCodeDueToInvalidAPIVersionFromOrchestrator_triesToRegisterAgain() throws Exception {
        expect(agent.isGrpcApiVersion4()).andReturn(true);
        agent.setGrpcApiVersion(GrpcApiVersion.V3);
        expectLastCall();
        agent.setLastMessageTypeReceived(Optional.of("ERROR"));
        expectLastCall();
        agent.register(anyObject(OrchestratorStreamObserver.class));
        expectLastCall();
        replay(this.agent);

        this.orchestratorStreamObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid api version. Only [2.0, 3.0] are supported.")
                                                .asRuntimeException());

        verify(this.agent);
    }

    @Test
    public void onError_receivesInvalidArgumentCodeDueToOtherReasons_doesNothing() throws Exception {
        expect(agent.isGrpcApiVersion4()).andReturn(true);
        agent.setLastMessageTypeReceived(Optional.of("ERROR"));
        expectLastCall();
        replay(this.agent);

        this.orchestratorStreamObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid argument code")
                                                .asRuntimeException());

        verify(this.agent);
    }

    @Test
    public void onError_alreadyExistsStatusWhileUnderRetryLimit_sleepsAndTriesToRegister() throws Exception {
        agent.setLastMessageTypeReceived(Optional.of("ERROR"));
        expectLastCall();
        agent.register(anyObject(OrchestratorStreamObserver.class));
        expectLastCall();
        replay(this.agent);

        this.orchestratorStreamObserver.onError(Status.ALREADY_EXISTS.asRuntimeException());

        verify(this.agent);
    }

    @Test
    public void onError_alreadyExistsStatusWhileAtRetryLimit_doesNothing() throws Exception {
        agent.setLastMessageTypeReceived(Optional.of("ERROR"));
        expectLastCall();
        this.orchestratorStreamObserver = new OrchestratorStreamObserver(agent, 20);

        replay(this.agent);

        this.orchestratorStreamObserver.onError(Status.ALREADY_EXISTS.asRuntimeException());

        verify(this.agent);
    }

    @Test
    public void onError_receivesStatusRuntimeExceptionWithAnyOtherCodeFromOrchestrator_triesToRegisterAgain() throws Exception {
        agent.setGrpcApiVersion(GrpcApiVersion.V4);
        expectLastCall();
        agent.setLastMessageTypeReceived(Optional.of("ERROR"));
        expectLastCall();
        agent.register(anyObject(OrchestratorStreamObserver.class));
        expectLastCall();
        replay(this.agent);

        this.orchestratorStreamObserver.onError(Status.CANCELLED.asRuntimeException());

        verify(this.agent);
    }

    @Test
    public void onError_receivesAnyOtherExceptionFromOrchestrator_triesToRegisterAgain() throws Exception {
        agent.setGrpcApiVersion(GrpcApiVersion.V4);
        expectLastCall();
        this.agent.setLastMessageTypeReceived(Optional.of("ERROR"));
        expectLastCall();
        agent.register(anyObject(OrchestratorStreamObserver.class));
        expectLastCall();
        replay(this.agent);

        this.orchestratorStreamObserver.onError(new RuntimeException());

        verify(this.agent);
    }

    @Test
    public void onError_receivesNullFromOrchestrator_triesToRegisterAgain() throws Exception {
        agent.setGrpcApiVersion(GrpcApiVersion.V4);
        expectLastCall();
        agent.register(anyObject(OrchestratorStreamObserver.class));
        expectLastCall();
        this.agent.setLastMessageTypeReceived(Optional.of("ERROR"));
        expectLastCall();
        replay(this.agent);

        this.orchestratorStreamObserver.onError(null);

        verify(this.agent);
    }

}