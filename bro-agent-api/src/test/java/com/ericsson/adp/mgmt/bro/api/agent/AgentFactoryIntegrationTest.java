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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Test;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.bro.api.exception.InvalidRegistrationInformationException;
import com.ericsson.adp.mgmt.bro.api.grpc.GrpcServiceIntegrationTest;
import com.ericsson.adp.mgmt.bro.api.registration.RegistrationInformation;
import com.ericsson.adp.mgmt.bro.api.test.RegistrationInformationUtil;
import com.ericsson.adp.mgmt.bro.api.test.TestAgentBehavior;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.AgentMessageType;
import com.ericsson.adp.mgmt.control.ControlInterfaceGrpc.ControlInterfaceImplBase;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import com.ericsson.adp.mgmt.control.Register;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;

public class AgentFactoryIntegrationTest extends GrpcServiceIntegrationTest {

    private OrchestratorStub orchestratorStub;
    private Agent agentValid;

    @After
    public void teardown() {
        if(this.agentValid != null) {
            this.agentValid.shutdown();
        }
    }

    @Test
    public void establishConnectionAndRegister_agentSendsPortHostAndRegistrationMessage_orchestratorConnectsAndReceivesMessage() throws InterruptedException, ExecutionException, TimeoutException {
        this.agentValid = AgentFactory.createAgent(new OrchestratorConnectionInformation("127.0.0.1", 3000), new TestAgentBehavior());

        assertNotNull(this.agentValid);

        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> this.orchestratorStub.messageArrived());

        final AgentControl agentControl = this.orchestratorStub.getMessage();
        assertEquals(Action.REGISTER, agentControl.getAction());
        assertEquals(AgentMessageType.REGISTER, agentControl.getAgentMessageType());
        assertTrue(agentControl.hasRegister());

        final Register register = agentControl.getRegister();
        assertEquals("123", register.getAgentId());
        assertEquals("4.0", register.getApiVersion());
        assertEquals("scope", register.getScope());

        final SoftwareVersionInfo softwareVersionInfo = register.getSoftwareVersionInfo();
        assertEquals("description", softwareVersionInfo.getDescription());
        assertEquals("productionDate", softwareVersionInfo.getProductionDate());
        assertEquals("productName", softwareVersionInfo.getProductName());
        assertEquals("productNumber", softwareVersionInfo.getProductNumber());
        assertEquals("type", softwareVersionInfo.getType());
        assertEquals("revision", softwareVersionInfo.getRevision());
        assertEquals("semanticVersion", softwareVersionInfo.getSemanticVersion());
        assertEquals("commercialVersion", softwareVersionInfo.getCommercialVersion());
    }

    @Test(expected = InvalidRegistrationInformationException.class)
    public void createAgent_NullTestAgentBehaviour_InvalidRegistrationInformationException() {
        AgentFactory.createAgent(new OrchestratorConnectionInformation("127.0.0.1", 3000), new NullTestAgentBehaviour());
    }

    @Test(expected = InvalidRegistrationInformationException.class)
    public void createAgent_BlankTestAgentBehaviour_InvalidRegistrationInformationException() {
        AgentFactory.createAgent(new OrchestratorConnectionInformation("127.0.0.1", 3000), new BlankTestAgentBehaviour());
    }

    @SuppressWarnings("deprecation")
    @Test(expected = InvalidRegistrationInformationException.class)
    public void createAgentWithoutOrchestratorConnectionInformation_BlankTestAgentBehaviour_InvalidRegistrationInformationException() {
        AgentFactory.createAgent("127.0.0.1", 3000, new BlankTestAgentBehaviour());
    }

    @Override
    protected List<BindableService> getServices() {
        this.orchestratorStub = new OrchestratorStub();
        return Arrays.asList(this.orchestratorStub);
    }

    private class OrchestratorStub extends ControlInterfaceImplBase {

        private boolean messageArrived = false;
        private AgentControl message;

        @Override
        public StreamObserver<AgentControl> establishControlChannel(final StreamObserver<OrchestratorControl> responseObserver) {
            return new StreamObserver<AgentControl>() {

                @Override
                public void onNext(final AgentControl message) {
                    if (message.hasRegister()) {
                        final OrchestratorControl registrationAck = OrchestratorControl.newBuilder()
                                .setOrchestratorMessageType(OrchestratorMessageType.REGISTER_ACKNOWLEDGE)
                                .build();
                        responseObserver.onNext(registrationAck);
                    }
                    OrchestratorStub.this.messageArrived = true;
                    OrchestratorStub.this.message = message;
                }

                @Override
                public void onError(final Throwable arg0) {
                    //Does nothing
                }

                @Override
                public void onCompleted() {
                    //Does nothing
                }
            };
        }

        public boolean messageArrived() {
            return this.messageArrived;
        }

        public AgentControl getMessage() {
            return this.message;
        }

    }

    private class NullTestAgentBehaviour extends TestAgentBehavior {
        @Override
        public RegistrationInformation getRegistrationInformation() {
            return RegistrationInformationUtil.getNullTestRegistrationInformation();
        }
    }

    private class BlankTestAgentBehaviour extends TestAgentBehavior {
        @Override
        public RegistrationInformation getRegistrationInformation() {
            return RegistrationInformationUtil.getBlankTestRegistrationInformation();
        }
    }

}
