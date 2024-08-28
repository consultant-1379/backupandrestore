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
package com.ericsson.adp.mgmt.backupandrestore.grpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentOutputStream;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentRepository;
import com.ericsson.adp.mgmt.backupandrestore.agent.exception.InvalidRegistrationMessageException;
import com.ericsson.adp.mgmt.backupandrestore.test.IntegrationTest;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.AgentMessageType;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.Register;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

import io.grpc.stub.StreamObserver;

public class ControlInterfaceImplementationIntegrationTest extends IntegrationTest {

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private ControlInterfaceImplementation controlInterface;

    private final List<StreamObserver<AgentControl>> outputStreams = new ArrayList<>();
    private StreamObserverStub<OrchestratorControl> orchestratorControlStream;

    @After
    public void teardown() {
        outputStreams.forEach(StreamObserver::onCompleted);
        outputStreams.clear();
    }

    @Test
    public void establishControlChannel_registrationMessage_keepAgentInformation( ) throws Exception {
        final StreamObserver<AgentControl> outputStream = callEstablishControlChannel();
        assertTrue(agentRepository.getAgents().isEmpty());
        assertTrue(outputStream instanceof AgentOutputStream);
        
        final String agentId = "123";
        outputStream.onNext(getRegistrationMessage(agentId));

        assertEquals(1, agentRepository.getAgents().size());
        assertEquals(agentId, agentRepository.getAgents().get(0).getAgentId());
    }

    @Test
    public void establishControlChannel_connectionCompleted_agentIsRemoved( ) throws Exception {
        final StreamObserver<AgentControl> outputStream = callEstablishControlChannel();
        final String agentId = "456";
        outputStream.onNext(getRegistrationMessage(agentId));

        assertEquals(1, agentRepository.getAgents().size());
        assertEquals(agentId, agentRepository.getAgents().get(0).getAgentId());

        outputStream.onCompleted();

        assertTrue(agentRepository.getAgents().isEmpty());
    }

    @Test
    public void establishControlChannel_error_agentIsRemoved( ) throws Exception {
        final StreamObserver<AgentControl> outputStream = callEstablishControlChannel();
        final String agentId = "789";
        outputStream.onNext(getRegistrationMessage(agentId));

        assertEquals(1, agentRepository.getAgents().size());
        assertEquals(agentId, agentRepository.getAgents().get(0).getAgentId());

        outputStream.onError(new RuntimeException("Qwe"));

        assertTrue(agentRepository.getAgents().isEmpty());
    }

    @Test
    public void establishControlChannel_connectionFromTwoAgentsWithSameId_keepFirstAgentAndRefuseTheSecond( ) throws Exception {
        final String agentId_1 = "111";
        final String agentId_2 = "222";
        callEstablishControlChannel().onNext(getRegistrationMessage(agentId_1));
        callEstablishControlChannel().onNext(getRegistrationMessage(agentId_2));
        callEstablishControlChannel().onNext(getRegistrationMessage(agentId_1));

        assertEquals(2, agentRepository.getAgents().size());
    }

    @Test
    public void establishControlChannel_noAgentId_doesNotRegisterAgent( ) throws Exception {
        callEstablishControlChannel().onNext(getRegistrationMessage(""));

        assertTrue(orchestratorControlStream.getError().getCause() instanceof InvalidRegistrationMessageException);
    }

    @Test
    public void establishControlChannel_noSoftwareVersion_doesNotRegisterAgent( ) throws Exception {
        callEstablishControlChannel().onNext(getRegistrationMessage("id", SoftwareVersionInfo.newBuilder().build()));

        assertTrue(orchestratorControlStream.getError().getCause() instanceof InvalidRegistrationMessageException);
    }

    @Test
    public void establishControlChannel_invalidApiVersion_doesNotRegisterAgent( ) throws Exception {
        callEstablishControlChannel().onNext(getRegistrationMessage("id", getSoftwareVersionInfo(), "1.0"));

        assertTrue(orchestratorControlStream.getError().getCause() instanceof InvalidRegistrationMessageException);
    }

    private class StreamObserverStub<T> implements StreamObserver<T> {

        private Throwable error;

        @Override
        public void onCompleted() {
            //Not needed
        }

        @Override
        public void onError(final Throwable error) {
            this.error = error;
        }

        @Override
        public void onNext(final T message) {
            //Not needed
        }

        public Throwable getError() {
            return error;
        }

    }

    private AgentControl getRegistrationMessage(final String agentId) {
        return getRegistrationMessage(agentId, getSoftwareVersionInfo());
    }

    private AgentControl getRegistrationMessage(final String agentId, final SoftwareVersionInfo softwareVersionInfo) {
        return getRegistrationMessage(agentId, softwareVersionInfo, "2.0");
    }

    private AgentControl getRegistrationMessage(final String agentId, final SoftwareVersionInfo softwareVersionInfo, final String apiVersion) {
        final Register registerMessage = Register
                .newBuilder()
                .setAgentId(agentId)
                .setSoftwareVersionInfo(softwareVersionInfo)
                .setApiVersion(apiVersion)
                .setScope("Alpha")
                .build();

        return AgentControl
                .newBuilder()
                .setAction(Action.REGISTER)
                .setAgentMessageType(AgentMessageType.REGISTER)
                .setRegister(registerMessage)
                .build();
    }

    private SoftwareVersionInfo getSoftwareVersionInfo() {
        return SoftwareVersionInfo
                .newBuilder()
                .setProductName("ProductName")
                .setProductNumber("ProductNumber")
                .setRevision("Revision")
                .setProductionDate("ProductionDate")
                .setDescription("Description")
                .setType("Type")
                .build();
    }

    private StreamObserver<AgentControl> callEstablishControlChannel() {
        this.orchestratorControlStream = new StreamObserverStub<>();
        final StreamObserver<AgentControl> outputStream = controlInterface.establishControlChannel(orchestratorControlStream);
        outputStreams.add(outputStream);
        return outputStream;
    }

}
