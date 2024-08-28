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
package com.ericsson.adp.mgmt.backupandrestore.grpc.backup;

import com.ericsson.adp.mgmt.backupandrestore.grpc.DataInterfaceImplementation;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.backup.BackupMetadataWriter;
import com.ericsson.adp.mgmt.backupandrestore.backup.state.BackupMetadataState;
import com.ericsson.adp.mgmt.backupandrestore.backup.state.BackupState;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;
import com.ericsson.adp.mgmt.data.BackupData;
import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * observes message from BackupDataChannel stream and directs them to relevant classes to process
 */
public class BackupDataStream implements StreamObserver<BackupData> {

    private static final Logger log = LogManager.getLogger(BackupDataStream.class);
    private static final int STREAM_ID_LENGTH = 8;

    private BackupState state;
    private final StreamObserver<Empty> orchestratorStream;
    private String streamId = "";

    /**
     * @param backupMetadataWriter used to write metadata.
     * @param job used to get information regarding the ongoing backup.
     * @param orchestratorStream empty stream observer.
     * @param idValidator to validate fragmentId.
     */
    public BackupDataStream(final BackupMetadataWriter backupMetadataWriter, final CreateBackupJob job,
                            final StreamObserver<Empty> orchestratorStream, final IdValidator idValidator) {
        this.orchestratorStream = orchestratorStream;
        this.state = new BackupMetadataState(backupMetadataWriter, job, idValidator);
    }

    /**
     * Method to generate an ID for the BackupDataStream instance
     * @return generated random ID
     */
    private String generateStreamId() {
        final boolean useLetters = true;
        final boolean useNumbers = true;
        return RandomStringUtils.random(STREAM_ID_LENGTH, useLetters, useNumbers);
    }

    public String getStreamId() {
        return this.streamId;
    }

    /**
     * Method to generate an ID for the BackupDataStream instance
     */
    public void updateStreamId() {
        this.streamId = generateStreamId();
    }

    @Override
    public void onNext(final BackupData message) {
        try {
            this.state = state.processMessage(message, getStreamId());
        } catch (final Exception e) {

            String logMessage;
            if (message.hasBackupFileChunk() || message.hasCustomMetadataFileChunk()) {
                logMessage = message.getDataMessageType().toString();
                log.error("Closing backup data channel (streamId:{}) due to error processing message of type <{}>", getStreamId(), logMessage, e);

            } else {
                logMessage = message.toString();
                log.error("Closing backup data channel (streamId: {}) due to error processing message <{}>", getStreamId(), logMessage, e);
            }
            this.orchestratorStream.onError(Status.ABORTED.withDescription(e.getMessage()).withCause(e).asRuntimeException());
            this.state = state.fail();
        }
    }

    @Override
    public void onError(final Throwable throwable) {
        log.info("backup data stream (streamId:{}) connection closed due to error", this.getStreamId());
        this.state = state.fail();
        DataInterfaceImplementation.removeStreamId(this.streamId);
    }

    @Override
    public void onCompleted() {
        log.info("backup data stream (streamId:{}) connection closed", this.streamId);
        this.state = state.complete();
        this.orchestratorStream.onNext(Empty.getDefaultInstance());
        this.orchestratorStream.onCompleted();
        DataInterfaceImplementation.removeStreamId(this.getStreamId());
    }

}
