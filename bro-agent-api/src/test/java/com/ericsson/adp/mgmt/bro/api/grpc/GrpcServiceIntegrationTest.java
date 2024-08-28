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
package com.ericsson.adp.mgmt.bro.api.grpc;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public abstract class GrpcServiceIntegrationTest {

    protected Server server;
    protected ManagedChannel channel;

    @Before
    public void baseGrpcSetup() throws Exception {
        final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(3000);
        getServices().forEach(serverBuilder::addService);
        server = serverBuilder.build().start();
        channel = ManagedChannelBuilder.forAddress("127.0.0.1", 3000).usePlaintext().build();
    }

    @After
    public void baseGrpcTeardown() throws Exception {
        channel.shutdownNow();
        channel.awaitTermination(3, TimeUnit.SECONDS);
        server.shutdownNow();
        server.awaitTermination();
    }

    protected abstract List<BindableService> getServices();

}