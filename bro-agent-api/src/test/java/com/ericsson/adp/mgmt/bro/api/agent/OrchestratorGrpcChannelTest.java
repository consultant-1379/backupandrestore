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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.bro.api.grpc.BackupDataStream;
import com.ericsson.adp.mgmt.bro.api.grpc.GrpcServiceIntegrationTest;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.ControlInterfaceGrpc.ControlInterfaceImplBase;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.DataInterfaceGrpc.DataInterfaceImplBase;
import com.google.protobuf.Empty;

import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;

public class OrchestratorGrpcChannelTest extends GrpcServiceIntegrationTest {

    private OrchestratorStub orchestratorStub;
    private DataStub dataStub;
    private OrchestratorGrpcChannel orchestratorGrpcChannel;

    @Before
    public void setup() {
        orchestratorGrpcChannel = new OrchestratorGrpcChannel(channel);
    }

    @Test
    public void establishControlChannel_streamObserver_connectsToOrchestrator() throws Exception {
        orchestratorGrpcChannel.establishControlChannel(EasyMock.createMock(OrchestratorStreamObserver.class));

        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(orchestratorStub::establishedControlChannel);

        assertTrue(orchestratorStub.establishedControlChannel());
    }

    @Test
    public void sendControlMessage_message_sendsMessageToOrchestrator() throws Exception {
        orchestratorGrpcChannel.establishControlChannel(EasyMock.createMock(OrchestratorStreamObserver.class));
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(orchestratorStub::establishedControlChannel);

        final AgentControl message = AgentControl.getDefaultInstance();

        orchestratorGrpcChannel.sendControlMessage(message);

        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(orchestratorStub::messageArrived);

        assertEquals(message, orchestratorStub.getMessage());
    }

    @Test
    public void getBackupStream_channel_opensBackupStream() throws Exception {
        final StreamObserver<BackupData> backupStream = orchestratorGrpcChannel.getBackupStream();

        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(dataStub::calledBackupStream);

        assertTrue(dataStub.calledBackupStream());
        assertTrue(backupStream instanceof BackupDataStream);
    }

    @Test
    public void shutdown_channel_shutsChannelDown() throws Exception {
        orchestratorGrpcChannel.shutdown();

        channel.awaitTermination(2, TimeUnit.SECONDS);

        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(channel::isTerminated);
    }

    @Override
    protected List<BindableService> getServices() {
        this.orchestratorStub = new OrchestratorStub();
        this.dataStub = new DataStub();
        return Arrays.asList(orchestratorStub, dataStub);
    }

    private class OrchestratorStub extends ControlInterfaceImplBase {

        private boolean establishedControlChannel;
        private boolean messageArrived;
        private AgentControl message;

        @Override
        public StreamObserver<AgentControl> establishControlChannel(final StreamObserver<OrchestratorControl> responseObserver) {
            establishedControlChannel = true;
            return new StreamObserver<AgentControl>() {

                @Override
                public void onNext(final AgentControl m) {
                    messageArrived = true;
                    message = m;
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

        public boolean establishedControlChannel() {
            return establishedControlChannel;
        }

        public boolean messageArrived() {
            return messageArrived;
        }

        public AgentControl getMessage() {
            return message;
        }

    }

    private class DataStub extends DataInterfaceImplBase {

        private boolean calledBackupStream;

        @Override
        public StreamObserver<BackupData> backup(final StreamObserver<Empty> responseObserver) {
            this.calledBackupStream = true;
            return null;
        }

        public boolean calledBackupStream() {
            return calledBackupStream;
        }

    }
}
