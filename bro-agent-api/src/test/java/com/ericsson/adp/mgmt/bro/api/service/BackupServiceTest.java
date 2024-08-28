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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.bro.api.exception.FailedToTransferBackupException;
import com.ericsson.adp.mgmt.bro.api.filetransfer.FileChunkServiceUtil;
import com.ericsson.adp.mgmt.bro.api.fragment.BackupFragmentInformation;
import com.ericsson.adp.mgmt.bro.api.util.ChecksumCalculator;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.BackupFileChunk;
import com.ericsson.adp.mgmt.data.CustomMetadataFileChunk;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;

public class BackupServiceTest {

    private static final String AGENT_ID = "123";
    private static final String FRAGMENT_ID = "456";
    private static final String BACKUP_NAME = "MyBackupServiceTest";
    private static final String BACKUP_FILE_PATH = "./src/test/resources/backup.txt";
    private static final String CUSTOM_METADATA_FILE_PATH = "./src/test/resources/CustomMetadata.txt";
    private static final String SIZE_IN_BYTES = "77";
    private static final String VERSION = "0.0.0";

    private BackupFragmentInformation backupInformation;

    @Before
    public void setUp() throws Exception {
        backupInformation = getBackupFragmentInformation();
    }

    @Test
    public void backup_fragmentWithoutCustomMetadata_sendsFragment() throws Exception {
        final BackupDataStreamObserver backupDataStreamObserver = new BackupDataStreamObserver();
        final BackupService backupService = new BackupService(backupDataStreamObserver);

        backupService.backup(backupInformation, AGENT_ID, BACKUP_NAME);

        final Deque<BackupData> messages = new ArrayDeque<>(backupDataStreamObserver.getMessages());

        assertEquals(getMetadataMessage(), messages.pop());
        assertEquals(getBackupFileNameMessage(), messages.pop());

        FileChunkServiceUtil.processFileChunks(BACKUP_FILE_PATH, (chunk, bytesReadInChunk) ->
            assertEquals(getBackupChunkMessage(ByteString.copyFrom(chunk, 0, bytesReadInChunk)), messages.pop())
        );

        assertEquals(getBackupChecksumMessage(), messages.pop());

        assertTrue(messages.isEmpty());
        assertTrue(backupDataStreamObserver.completedConnection());
    }

    @Test
    public void backup_fragmentWithCustomMetadata_sendsFragment() throws Exception {
        final BackupDataStreamObserver backupDataStreamObserver = new BackupDataStreamObserver();
        final BackupService backupService = new BackupService(backupDataStreamObserver);

        backupInformation.setCustomMetadataFilePath(Optional.of(CUSTOM_METADATA_FILE_PATH));

        backupService.backup(backupInformation, AGENT_ID, BACKUP_NAME);

        final Deque<BackupData> messages = new ArrayDeque<>(backupDataStreamObserver.getMessages());

        assertEquals(getMetadataMessage(), messages.pop());

        assertEquals(getBackupFileNameMessage(), messages.pop());
        FileChunkServiceUtil.processFileChunks(BACKUP_FILE_PATH, (chunk, bytesReadInChunk) ->
            assertEquals(getBackupChunkMessage(ByteString.copyFrom(chunk, 0, bytesReadInChunk)), messages.pop())
        );
        assertEquals(getBackupChecksumMessage(), messages.pop());

        assertEquals(getCustomMetadataFileNameMessage(), messages.pop());
        FileChunkServiceUtil.processFileChunks(CUSTOM_METADATA_FILE_PATH, (chunk, bytesReadInChunk) ->
            assertEquals(getCustomMetadataChunkMessage(ByteString.copyFrom(chunk, 0, bytesReadInChunk)), messages.pop())
        );
        assertEquals(getCustomMetadataChecksumMessage(), messages.pop());

        assertTrue(messages.isEmpty());
        assertTrue(backupDataStreamObserver.completedConnection());
    }

    @Test
    public void backup_invalidBackupFile_sendsErrorOnChannelAndThrowsException() throws Exception {
        final ErrorStreamObserver errorStreamObserver = new ErrorStreamObserver();
        final BackupService backupService = new BackupService(errorStreamObserver);

        backupInformation.setBackupFilePath("this/is/not/a/valid/file.txt");

        try {
            backupService.backup(backupInformation, AGENT_ID, BACKUP_NAME);
            fail();
        } catch (final FailedToTransferBackupException e) {
            assertEquals(e.getCause(), errorStreamObserver.getError());
        } catch (final Exception e) {
            fail();
        }
    }

    private BackupFragmentInformation getBackupFragmentInformation() {
        final BackupFragmentInformation backupInformation = new BackupFragmentInformation();

        backupInformation.setBackupFilePath(BACKUP_FILE_PATH);
        backupInformation.setFragmentId(FRAGMENT_ID);
        backupInformation.setSizeInBytes(SIZE_IN_BYTES);
        backupInformation.setVersion(VERSION);
        backupInformation.setCustomInformation(getCustomInformation());

        return backupInformation;
    }

    private Map<String, String> getCustomInformation() {
        final Map<String, String> customInformation = new HashMap<>();
        customInformation.put("a", "1");
        return customInformation;
    }

    private BackupData getMetadataMessage() {
        final Fragment fragment = Fragment
                .newBuilder()
                .setFragmentId(FRAGMENT_ID)
                .setSizeInBytes(SIZE_IN_BYTES)
                .setVersion(VERSION)
                .putAllCustomInformation(getCustomInformation())
                .build();

        final Metadata metadata = Metadata
                .newBuilder()
                .setAgentId(AGENT_ID)
                .setFragment(fragment)
                .setBackupName(BACKUP_NAME)
                .build();

        return BackupData
                .newBuilder()
                .setMetadata(metadata)
                .setDataMessageType(DataMessageType.METADATA)
                .build();
    }

    private BackupData getBackupFileNameMessage() {
        return BackupData
                .newBuilder()
                .setBackupFileChunk(BackupFileChunk.newBuilder().setFileName(Paths.get(BACKUP_FILE_PATH).getFileName().toString()))
                .setDataMessageType(DataMessageType.BACKUP_FILE)
                .build();
    }

    private BackupData getCustomMetadataFileNameMessage() {
        return BackupData
                .newBuilder()
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setFileName(Paths.get(CUSTOM_METADATA_FILE_PATH).getFileName().toString()))
                .setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .build();
    }

    private BackupData getBackupChunkMessage(final ByteString chunk) {
        return BackupData
                .newBuilder()
                .setBackupFileChunk(BackupFileChunk.newBuilder().setContent(chunk).build())
                .setDataMessageType(DataMessageType.BACKUP_FILE)
                .build();
    }

    private BackupData getCustomMetadataChunkMessage(final ByteString chunk) {
        return BackupData
                .newBuilder()
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setContent(chunk).build())
                .setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .build();
    }

    private BackupData getBackupChecksumMessage() throws IOException {
        return BackupData
                .newBuilder()
                .setBackupFileChunk(BackupFileChunk.newBuilder().setChecksum(new ChecksumCalculator().calculateChecksum(BACKUP_FILE_PATH)).build())
                .setDataMessageType(DataMessageType.BACKUP_FILE)
                .build();
    }

    private BackupData getCustomMetadataChecksumMessage() throws IOException {
        return BackupData
                .newBuilder()
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setChecksum(new ChecksumCalculator().calculateChecksum(CUSTOM_METADATA_FILE_PATH)).build())
                .setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .build();
    }

    private class ErrorStreamObserver implements StreamObserver<BackupData> {

        private Throwable error;

        @Override
        public void onNext(final BackupData arg0) {

        }

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(final Throwable arg0) {
            error = arg0;
        }

        public Throwable getError() {
            return error;
        }

    }

    private class BackupDataStreamObserver implements StreamObserver<BackupData> {

        private final List<BackupData> messages = new ArrayList<>();
        private boolean completedConnection;

        @Override
        public void onNext(final BackupData arg0) {
            messages.add(arg0);
        }

        @Override
        public void onCompleted() {
            completedConnection = true;
        }

        @Override
        public void onError(final Throwable arg0) {

        }

        public List<BackupData> getMessages() {
            return messages;
        }

        public boolean completedConnection() {
            return completedConnection;
        }

    }

}
