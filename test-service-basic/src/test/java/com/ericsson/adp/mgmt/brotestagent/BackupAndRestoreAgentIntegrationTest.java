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
package com.ericsson.adp.mgmt.brotestagent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Test;

import com.ericsson.adp.mgmt.bro.api.exception.InvalidRegistrationInformationException;
import com.ericsson.adp.mgmt.brotestagent.test.GrpcServiceIntegrationTest;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.ControlInterfaceGrpc.ControlInterfaceImplBase;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;

import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;

public class BackupAndRestoreAgentIntegrationTest extends GrpcServiceIntegrationTest {

    private ControlInterface controlInterface;

    @Test
    public void main_startsTestAgent_testAgentIsAlive() throws Exception {
        startTestAgent();

        waitUntil(this.controlInterface::receivedMessage);
        assertNotNull(BackupAndRestoreAgent.getAgent());
    }

    @Test
    public void main_startsTestAgent_noProperties() throws Exception {
        startTestAgent(new String[] {});

        waitUntil(this.controlInterface::receivedMessage);
        assertNotNull(BackupAndRestoreAgent.getAgent());
    }

    @Test
    public void main_startsTestAgentAndChannelToOrchestratorCloses_agentRetries() throws Exception {
        startTestAgent();

        waitUntil(this.controlInterface::receivedMessage);

        this.controlInterface.reset();

        restartGrpcServer();

        waitUntil(this.controlInterface::receivedMessage);
    }

    @Test
    public void main_orchestratorUnavailableWhenAgentStarts_agentRetries() throws Exception {
        stopGrpcServer();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> startTestAgent());

        assertFalse(this.controlInterface.receivedMessage);

        startGrpcServer();

        waitUntil(this.controlInterface::receivedMessage);
        executor.shutdown();
    }

    @Test
    public void main_startsTestAgentWithNonExistingBehavior_usesDefaultBehaviorAndTriesToRegister() throws Exception {
        startTestAgent("src/test/resources/invalidBehavior.properties");

        waitUntil(this.controlInterface::receivedMessage);
        assertNotNull(BackupAndRestoreAgent.getAgent());
    }

    @Test(expected = InvalidRegistrationInformationException.class)
    public void main_startsTestAgentWithInvalidAgentId_throwsException() throws Exception {
        startTestAgent("src/test/resources/invalidAgentId.properties");
    }

    @Override
    protected List<BindableService> getServices() {
        this.controlInterface = new ControlInterface();
        return Arrays.asList(this.controlInterface);
    }

    private void startTestAgent() {
        startTestAgent(new String[] { "src/test/resources/application.properties" });
    }

    private void startTestAgent(final String propertiesFile) {
        startTestAgent(new String[] { propertiesFile });
    }

    private void startTestAgent(final String[] args) {
        BackupAndRestoreAgent.main(args);
        waitUntil(() -> BackupAndRestoreAgent.getAgent() != null);
    }

    private void waitUntil(final Callable<Boolean> condition) {
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(condition);
    }

    private class ControlInterface extends ControlInterfaceImplBase {

        private boolean receivedMessage;

        @Override
        public StreamObserver<AgentControl> establishControlChannel(final StreamObserver<OrchestratorControl> ochestratorStreamObserver) {
            return new StreamObserver<AgentControl>() {

                @Override
                public void onNext(final AgentControl message) {
                    if(message.hasRegister()) {
                        final OrchestratorControl registrationAck = OrchestratorControl.newBuilder()
                                .setOrchestratorMessageType(OrchestratorMessageType.REGISTER_ACKNOWLEDGE)
                                .build();
                        ochestratorStreamObserver.onNext(registrationAck);
                    }
                    ControlInterface.this.receivedMessage = true;
                }

                @Override
                public void onError(final Throwable arg0) {
                    //Not needed
                }

                @Override
                public void onCompleted() {
                    //Not needed
                }
            };
        }

        public boolean receivedMessage() {
            return this.receivedMessage;
        }

        public void reset() {
            this.receivedMessage = false;
        }

    }

}
