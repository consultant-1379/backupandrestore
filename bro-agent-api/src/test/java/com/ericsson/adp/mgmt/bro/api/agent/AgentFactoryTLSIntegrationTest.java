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

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Test;

import com.ericsson.adp.mgmt.bro.api.test.TestAgentBehavior;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.ControlInterfaceGrpc.ControlInterfaceImplBase;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.stub.StreamObserver;

public class AgentFactoryTLSIntegrationTest {

    private Agent agentValid;
    private Server server;

    @After
    public void teardown() {
        if(agentValid != null) agentValid.shutdown();
        if(server != null) server.shutdown();
    }

    @Test
    public void agentTlsEnabledSendsPortHostAndRegistrationMessage_establishConnectionAndRegister() throws IOException {
        OrchestratorConnectionInformation orchestratorConnectionInformation =
                new OrchestratorConnectionInformation("127.0.0.1", 3000, "foo.test.google.fr", "src/test/resources/ca.pem");

        OrchestratorStub orchestratorStub = new OrchestratorStub();

        NettyServerBuilder serverBuilder = NettyServerBuilder
                .forPort(3000)
                .addService(orchestratorStub)
                .keepAliveTime(2, TimeUnit.MINUTES)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .permitKeepAliveTime(2, TimeUnit.MINUTES)
                .permitKeepAliveWithoutCalls(true)
                .sslContext(getSslContextBuilder().build());
        server = serverBuilder.build().start();

        agentValid = AgentFactory.createAgent(orchestratorConnectionInformation, new TestAgentBehavior());
        assertNotNull(agentValid);

        Awaitility.await().until(() -> orchestratorStub.messageArrived());
    }

    @Test
    public void agentMtlsEnabledSendsPortHostAndRegistrationMessage_establishConnectionAndRegister() throws IOException {
        OrchestratorConnectionInformation mTLSorchestratorConnectionInformation =
                new OrchestratorConnectionInformation("127.0.0.1", 3000, "foo.test.google.fr", "src/test/resources/ca.pem",
                                "src/test/resources/clientcert.pem", "src/test/resources/clientprivkey.key");

        OrchestratorStub orchestratorStub = new OrchestratorStub();

        NettyServerBuilder serverBuilder = NettyServerBuilder
                .forPort(3000)
                .addService(orchestratorStub)
                .keepAliveTime(2, TimeUnit.MINUTES)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .permitKeepAliveTime(2, TimeUnit.MINUTES)
                .permitKeepAliveWithoutCalls(true)
                .sslContext(getSslContextBuilder().build());
        server = serverBuilder.build().start();

        agentValid = AgentFactory.createAgent(mTLSorchestratorConnectionInformation, new TestAgentBehavior());
        assertNotNull(agentValid);

        Awaitility.await().until(() -> orchestratorStub.messageArrived());
    }

    private SslContextBuilder getSslContextBuilder() {
        final SslContextBuilder sslClientContextBuilder = SslContextBuilder.forServer(new File("src/test/resources/server1.pem"),
                new File("src/test/resources/server1.key")).trustManager(new File("src/test/resources/broca.pem")).clientAuth(ClientAuth.OPTIONAL);
        return GrpcSslContexts.configure(sslClientContextBuilder);
    }

    private class OrchestratorStub extends ControlInterfaceImplBase {

        private boolean messageArrived = false;

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
                    messageArrived = true;
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
            return messageArrived;
        }

    }
}