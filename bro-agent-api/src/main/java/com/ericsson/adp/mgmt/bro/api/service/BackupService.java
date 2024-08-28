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
package com.ericsson.adp.mgmt.bro.api.service;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.bro.api.exception.FailedToTransferBackupException;
import com.ericsson.adp.mgmt.bro.api.filetransfer.FileChunkServiceUtil;
import com.ericsson.adp.mgmt.bro.api.fragment.BackupFragmentInformation;
import com.ericsson.adp.mgmt.bro.api.grpc.BackupFileMessageBuilder;
import com.ericsson.adp.mgmt.bro.api.grpc.BackupMessageBuilder;
import com.ericsson.adp.mgmt.bro.api.grpc.CustomMetadataFileMessageBuilder;
import com.ericsson.adp.mgmt.bro.api.util.ChecksumCalculator;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;

import static com.ericsson.adp.mgmt.bro.api.grpc.GRPCConfig.AGENT_FRAGMENT_CHUNK_SIZE;

/**
 * Backup Service streams data to backup in chunks
 */
public class BackupService {

    private static final Logger log = LogManager.getLogger(BackupService.class);
    private final StreamObserver<BackupData> backupStream;

    /**
     * Creates BackupService.
     * @param backupStream
     *            The stream to use to send the backup data to the orchestrator
     */
    public BackupService(final StreamObserver<BackupData> backupStream) {
        this.backupStream = backupStream;
    }

    /**
     * Sends chunk of files and checksum
     *
     * @param fragmentInformation
     *            The information about this backup required to perform an upload
     * @param agentId
     *            The ID of this agent
     * @param backupName
     *            The name given on request of the backup
     * @throws FailedToTransferBackupException
     *             Is thrown if there is an issue in the transfer of the backup
     */
    public void backup(final BackupFragmentInformation fragmentInformation, final String agentId, final String backupName)
            throws FailedToTransferBackupException {
        try {
            sendMetadata(agentId, fragmentInformation, backupName);
            sendFile(fragmentInformation.getBackupFilePath(), getBackupFileMessagerBuilder());
            final Optional<String> custom = fragmentInformation.getCustomMetadataFilePath();
            if (custom.isPresent()) {
                sendFile(custom.get(), getCustomMetadataFileMessageBuilder());
            }
            endRequest();
        } catch (final Exception e) {
            log.error("Error sending file for: " + backupName, e);
            sendErrorResponse(e);
            throw new FailedToTransferBackupException("There was an error while trying to transfer: " + fragmentInformation.getBackupFilePath(), e);
        }
    }

    private void sendMetadata(final String agentId, final BackupFragmentInformation fragmentInformation, final String backupName) {
        final Fragment fragment = getFragment(fragmentInformation);
        final Metadata metadata = Metadata.newBuilder().setAgentId(agentId).setFragment(fragment).setBackupName(backupName).build();
        final BackupData message = BackupData.newBuilder().setMetadata(metadata).setDataMessageType(DataMessageType.METADATA).build();
        log.debug("Sending backup metadata {}", metadata);
        sendMessage(message);
    }

    private void sendFile(final String path, final BackupMessageBuilder backupMessageBuilder) throws IOException {
        log.debug("Transferring file <{}>", path);
        final ChecksumCalculator calculator = new ChecksumCalculator();

        log.debug("Sending message with file name");
        sendMessage(backupMessageBuilder.getFileNameMessage(getFileName(path)));

        log.debug("Sending message(s) with file data");
        FileChunkServiceUtil.processFileChunks(path, (chunk, bytesReadInChunk) -> {
            log.debug("Sending message with file chunk");
            sendMessage(backupMessageBuilder.getDataMessage(ByteString.copyFrom(chunk, 0, bytesReadInChunk)));
            calculator.addBytes(chunk, 0, bytesReadInChunk);
        }, AGENT_FRAGMENT_CHUNK_SIZE.getValue());

        log.debug("Sending message with checksum");
        sendMessage(backupMessageBuilder.getChecksumMessage(calculator.getChecksum()));

        log.debug("Finished transferring file <{}>", path);
    }

    private void sendMessage(final BackupData message) {
        backupStream.onNext(message);
    }

    private void sendErrorResponse(final Exception exception) {
        backupStream.onError(exception);
    }

    private void endRequest() {
        backupStream.onCompleted();
    }

    private Fragment getFragment(final BackupFragmentInformation fragmentInformation) {
        return Fragment
                .newBuilder()
                .setFragmentId(fragmentInformation.getFragmentId())
                .setSizeInBytes(fragmentInformation.getSizeInBytes())
                .setVersion(fragmentInformation.getVersion())
                .putAllCustomInformation(fragmentInformation.getCustomInformation())
                .build();
    }

    private String getFileName(final String path) {
        return Paths.get(path).getFileName().toString();
    }

    private BackupFileMessageBuilder getBackupFileMessagerBuilder() {
        return new BackupFileMessageBuilder();
    }

    private CustomMetadataFileMessageBuilder getCustomMetadataFileMessageBuilder() {
        return new CustomMetadataFileMessageBuilder();
    }

}
