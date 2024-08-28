/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.brotestagent.test;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;

import com.ericsson.adp.mgmt.brotestagent.BackupAndRestoreAgent;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;

public abstract class GrpcServiceIntegrationTest {

    private static final Logger log = LogManager.getLogger(GrpcServiceIntegrationTest.class);

    protected Server server;
    private static final String SERVER_CERTIFICATE_PATH = "src/test/resources/ExampleCertificates/server1.pem";
    private static final String SERVER_PRIVATE_KEY_PATH = "src/test/resources/ExampleCertificates/server1.key";
    private static final String SERVER_CERTIFICATE_AUTHORITY_PATH = "src/test/resources/ExampleCertificates/broca.pem";

    @Before
    public void baseGrpcSetup() throws Exception {
        startGrpcServer();
    }

    @After
    public void baseGrpcTeardown() {
        try {
            log.info("Shutting down GRPC server after test");
            stopGrpcServer();
            if(BackupAndRestoreAgent.getExecutorService() != null) {
                BackupAndRestoreAgent.killAgent();
            }
            Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> BackupAndRestoreAgent.getExecutorService() == null);
        } catch (final Exception e) {}
    }

    protected void startGrpcServer() throws Exception {
        final NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(3000).sslContext(getSslContextBuilder().build());;
        getServices().forEach(serverBuilder::addService);
        server = serverBuilder.build().start();
    }

    private SslContextBuilder getSslContextBuilder() {
        SslContextBuilder sslClientContextBuilder = null;
                sslClientContextBuilder = SslContextBuilder.forServer(new File(SERVER_CERTIFICATE_PATH),
                        new File(SERVER_PRIVATE_KEY_PATH)).trustManager(new File(SERVER_CERTIFICATE_AUTHORITY_PATH)).clientAuth(ClientAuth.OPTIONAL);
        return GrpcSslContexts.configure(sslClientContextBuilder);
    }

    protected void stopGrpcServer() throws Exception {
        server.shutdownNow();
        server.awaitTermination();
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(server::isTerminated);
    }

    protected void restartGrpcServer() throws Exception {
        stopGrpcServer();
        startGrpcServer();
    }

    protected abstract List<BindableService> getServices();

}
