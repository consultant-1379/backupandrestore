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

import java.util.Iterator;

import com.ericsson.adp.mgmt.bro.api.grpc.BackupDataStream;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.ControlInterfaceGrpc;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.DataInterfaceGrpc;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.data.RestoreData;

import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

/**
 * Responsible for managing GRPC Connection to orchestrator.
 */
public class OrchestratorGrpcChannel {

    private final ManagedChannel channel;
    private StreamObserver<AgentControl> controlStream;

    /**
     * Creates OrchestratorGrpcChannel based on managedChannel.
     * @param channel managedChannel.
     */
    protected OrchestratorGrpcChannel(final ManagedChannel channel) {
        this.channel = channel;
    }

    /**
     * Establishes control channel to orchestrator.
     * @param streamObserver to handle messages from orchestrator.
     */
    protected void establishControlChannel(final OrchestratorStreamObserver streamObserver) {
        controlStream = ControlInterfaceGrpc.newStub(channel).withWaitForReady().establishControlChannel(streamObserver);
    }

    /**
     * Sends control message to orchestrator.
     * @param message to be sent.
     */
    protected void sendControlMessage(final AgentControl message) {
        if (controlStream != null) {
            controlStream.onNext(message);
        }
    }

    /**
     * Gets stream to transmit backup data on.
     * @return stream.
     */
    protected StreamObserver<BackupData> getBackupStream() {
        return new BackupDataStream(channel.newCall(DataInterfaceGrpc.getBackupMethod(), CallOptions.DEFAULT));
    }

    /**
     * Gets restoreData iterator.
     * @param metadata for the restore.
     * @return iterator for the restore data.
     */
    protected Iterator<RestoreData> getRestoreDataIterator(final Metadata metadata) {
        return DataInterfaceGrpc.newBlockingStub(channel).restore(metadata);
    }

    /**
     * Shuts channel down.
     */
    protected void shutdown() {
        channel.shutdownNow();
    }

}
