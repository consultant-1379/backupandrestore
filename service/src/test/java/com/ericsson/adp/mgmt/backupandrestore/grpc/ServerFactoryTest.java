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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.SSLException;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.ControlInterfaceGrpc;
import com.ericsson.adp.mgmt.control.OrchestratorControl;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextService;
import io.grpc.stub.StreamObserver;

public class ServerFactoryTest {

    private Server server;
    private ServerFactory factory;
    private StreamObserverStub streamObserverStub;
    private static final String CERTIFICATES_PATH = "src/test/resources/";

    @Before
    public void setup() {
        SslContextService sslContextService = new SslContextService();
        sslContextService.setCertificateChainFilePath(CERTIFICATES_PATH + "server1.pem");
        sslContextService.setPrivateKeyFilePath(CERTIFICATES_PATH + "server1.key");
        sslContextService.setCertificateAuthorityFilePath(CERTIFICATES_PATH + "broca.pem");
        factory = new ServerFactory();
        factory.setPort(3005);
        factory.setControlInterface(new ControlInterfaceStub());
        factory.setDataInterface(new DataInterfaceImplementation());
        factory.setSslContextService(sslContextService);
        this.streamObserverStub = new StreamObserverStub();
    }

    @After
    public void teardown() {
        if(server != null) {
            server.shutdown();
        }
    }

    @Test
    public void getServer_destinationPort_actualServerAtSpecifiedPort() throws Exception {
        server = factory.getServer();

        assertFalse(server.isTerminated());
        assertEquals(3005, server.getPort());

        final List<String> services = server.getServices().stream().map(service -> service.getServiceDescriptor().getName()).collect(Collectors.toList());

        assertEquals(2, services.size());
        assertTrue(services.contains("com.ericsson.adp.mgmt.control.ControlInterface"));
        assertTrue(services.contains("com.ericsson.adp.mgmt.data.DataInterface"));
    }

    @Test
    public void getServer_tlsEnabled_agentWithTlsEnabledIsAbleToRegister() throws Exception {
        factory.setTlsEnabled(true);

        server = factory.getServer();

        assertFalse(server.isTerminated());

        ManagedChannel channel = NettyChannelBuilder
                .forAddress("localhost", 3005)
                .keepAliveTime(2, TimeUnit.MINUTES)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .sslContext(buildSslContext(CERTIFICATES_PATH + "ca.pem", CERTIFICATES_PATH + "clientcert.pem" , CERTIFICATES_PATH + "clientprivkey.key"))
                .build();
        InputStreamObserver inputStreamObserver = new InputStreamObserver();
        StreamObserver<AgentControl> streamObserver = ControlInterfaceGrpc.newStub(channel).establishControlChannel(inputStreamObserver);
        AgentControl message = getRegistrationMessage();

        streamObserver.onNext(message);

        Awaitility.await().until(() -> streamObserverStub.getMessage() != null);

        assertEquals(message, streamObserverStub.getMessage());
    }

    @Test
    public void getServer_tlsEnabled_agentWithoutTlsEnabledFailsToRegister() throws Exception {
        factory.setTlsEnabled(true);

        server = factory.getServer();

        assertFalse(server.isTerminated());

        ManagedChannel channel = NettyChannelBuilder
                .forAddress("localhost", 3005)
                .usePlaintext()
                .build();

        InputStreamObserver inputStreamObserver = new InputStreamObserver();
        StreamObserver<AgentControl> streamObserver = ControlInterfaceGrpc.newStub(channel).establishControlChannel(inputStreamObserver);
        AgentControl message = getRegistrationMessage();

        streamObserver.onNext(message);

        Awaitility.await().until(() -> inputStreamObserver.getError() != null);
        StatusRuntimeException exception = (StatusRuntimeException)inputStreamObserver.getError();
        assertEquals("UNAVAILABLE: Network closed for unknown reason", exception.getMessage());

    }

    @Test(expected = IllegalArgumentException.class)
    public void getServer_tlsEnabled_channelFailsWithInvalidCa() throws Exception {
        factory.setTlsEnabled(true);

        server = factory.getServer();

        assertFalse(server.isTerminated());

        NettyChannelBuilder
                .forAddress("localhost", 3005)
                .keepAliveTime(2, TimeUnit.MINUTES)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .overrideAuthority("foo.test.google.fr")
                .sslContext(buildSslContext(CERTIFICATES_PATH + "invalidCert.pem", null, null))
                .build();
    }

    private static SslContext buildSslContext(final String trustCertCollectionFilePath,
                final String clientCertificatePath, final String clientPrivKeyPath) throws SSLException {
        final SslContextBuilder builder = GrpcSslContexts.forClient();
        if (trustCertCollectionFilePath != null) {
            builder.trustManager(new File(trustCertCollectionFilePath));
        }
        if (clientCertificatePath != null) {
            builder.keyManager(new File(clientCertificatePath),
                new File(clientPrivKeyPath));
        }
        return builder.build();
    }

    private AgentControl getRegistrationMessage() {
        return AgentControl
                .newBuilder()
                .setAction(Action.REGISTER)
                .build();
    }

    private class ControlInterfaceStub extends ControlInterfaceImplementation {

        @Override
        public StreamObserver<AgentControl> establishControlChannel(StreamObserver<OrchestratorControl> orchestratorControlStream) {
            return streamObserverStub;
        }
    }

    private class InputStreamObserver implements StreamObserver<OrchestratorControl> {
        private Throwable error;

        @Override
        public void onCompleted() {
            //Does nothing
        }

        @Override
        public void onError(Throwable arg0) {
            error = arg0;
        }

        @Override
        public void onNext(OrchestratorControl arg0) {
            //Does nothing

        }

        public Throwable getError() {
            return error;
        }

    }

    private class StreamObserverStub implements StreamObserver<AgentControl> {

        private AgentControl message;

        @Override
        public void onCompleted() {
            //Does nothing
        }

        @Override
        public void onError(Throwable arg0) {
            //Does nothing
        }

        @Override
        public void onNext(AgentControl arg0) {
            message = arg0;
        }

        public AgentControl getMessage() {
            return message;
        }
    }
}
