package com.ericsson.adp.mgmt.bro.api.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.bro.api.exception.FailedToDownloadException;
import com.ericsson.adp.mgmt.bro.api.filetransfer.FileChunkServiceUtil;
import com.ericsson.adp.mgmt.bro.api.util.ChecksumCalculator;
import com.ericsson.adp.mgmt.data.BackupFileChunk;
import com.ericsson.adp.mgmt.data.CustomMetadataFileChunk;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.RestoreData;
import com.google.protobuf.ByteString;

public class RestoreServiceTest {

    private static final Path BACKUP_FILE_PATH = Paths.get("src/test/resources/backup.txt");
    private static final Path CUSTOM_METADATA_FILE_PATH = Paths.get("src/test/resources/CustomMetadata.txt");
    private static final String RESTORED_FILE_NAME = "restoredData.txt";
    private static final String RESTORED_METADATA_FILE_NAME = "restoredDataMetadata.txt";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private RestoreService service;
    private ChecksumCalculator calculator;
    private final List<RestoreData> restoreDataList = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        this.calculator = new ChecksumCalculator();
        this.service = new RestoreService(getRestoreLocation().toString());
    }

    @After
    public void tearDown() throws IOException {
        this.restoreDataList.clear();
    }

    @Test
    public void download_nonEmptyCustomMetadataPath_writeBackupandMetadataChunk() throws Exception {
        addFileName();
        addBackupChunks();
        addValidChecksum();
        addcustomMetadataFileName(RESTORED_METADATA_FILE_NAME);
        addCustomMetadataChunks();

        this.service.download(this.restoreDataList.iterator());

        assertEquals(Files.readAllLines(BACKUP_FILE_PATH), Files.readAllLines(getRestoreLocation().resolve(RESTORED_FILE_NAME)));
        assertEquals(Files.readAllLines(CUSTOM_METADATA_FILE_PATH), Files.readAllLines(getRestoreLocation().resolve(RESTORED_METADATA_FILE_NAME)));
    }

    @Test(expected = FailedToDownloadException.class)
    public void download_receivedMetadataChunkButMetadataPathIsEmpty_throwException() throws Exception{
        addFileName();
        addBackupChunks();
        addValidChecksum();
        addcustomMetadataFileName("");
        addCustomMetadataChunks();

        this.service.download(this.restoreDataList.iterator());
    }

    @Test
    public void download_notReceivedMetadataChunkAndMetadataPathIsEmpty_writebackupChunk() throws Exception {
        addFileName();
        addBackupChunks();
        addValidChecksum();

        this.service.download(this.restoreDataList.iterator());

        assertEquals(Files.readAllLines(BACKUP_FILE_PATH), Files.readAllLines(getRestoreLocation().resolve(RESTORED_FILE_NAME)));
    }

    @Test(expected = FailedToDownloadException.class)
    public void download_backupFileChecksumMismatch_throwException() throws Exception{
        addFileName();
        addBackupChunks();
        addInvalidChecksum();

        this.service.download(this.restoreDataList.iterator());
    }

    private void addBackupChunks() throws IOException {
        FileChunkServiceUtil.processFileChunks(BACKUP_FILE_PATH.toString(), (chunk, numberOfBytesRead) -> {
            this.restoreDataList.add(RestoreData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                    .setBackupFileChunk(BackupFileChunk.newBuilder().setContent(ByteString.copyFrom(chunk, 0, numberOfBytesRead)).build()).build());
            this.calculator.addBytes(chunk, 0, numberOfBytesRead);
        });
    }

    private void addFileName() {
        this.restoreDataList.add(RestoreData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setFileName(RESTORED_FILE_NAME).build()).build());
    }

    private void addcustomMetadataFileName(final String fileName) {
        this.restoreDataList.add(RestoreData.newBuilder().setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setFileName(fileName).build()).build());
    }

    private void addValidChecksum() {
        this.restoreDataList.add(RestoreData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setChecksum(this.calculator.getChecksum()).build()).build());
    }

    private void addInvalidChecksum() {
        this.restoreDataList.add(RestoreData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setChecksum("abcd").build()).build());
    }

    private void addCustomMetadataChunks() throws IOException {
        FileChunkServiceUtil.processFileChunks(CUSTOM_METADATA_FILE_PATH.toString(), (chunk, numberOfBytesRead) -> {
            this.restoreDataList
                    .add(RestoreData.newBuilder().setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                            .setCustomMetadataFileChunk(
                                    CustomMetadataFileChunk.newBuilder().setContent(ByteString.copyFrom(chunk, 0, numberOfBytesRead)).build())
                            .build());
            this.calculator.addBytes(chunk, 0, numberOfBytesRead);
        });

        this.restoreDataList.add(RestoreData.newBuilder().setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setChecksum(this.calculator.getChecksum()).build()).build());
    }

    private Path getRestoreLocation() {
        return this.folder.getRoot().toPath();
    }

}
