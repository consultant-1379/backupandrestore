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
package com.ericsson.adp.mgmt.backupandrestore.backup.state;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.FragmentFolder;
import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumCalculator;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.BackupFileChunk;
import com.ericsson.adp.mgmt.data.CustomMetadataFileChunk;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.google.protobuf.ByteString;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class BackupCustomMetadataStateTest {

    private static final String TEST_CUSTOM_METADATA_FILE_NAME = "TestingCustomMetadata";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path location = null;
    private BackupCustomMetadataState state;

    private FragmentFolder fragmentFolder;
    private CreateBackupJob job;

    @Before
    public void setUp() {
        location = folder.getRoot().toPath();
        this.fragmentFolder = new FragmentFolder(location);
        this.job = createMock(CreateBackupJob.class);
        expect(job.getAwsConfig()).andReturn(new S3Config()).anyTimes();
        replay(job);
        state = new BackupCustomMetadataState(fragmentFolder, job, null);
    }

    @After
    public void tearDown() throws Exception {
        state.close();
    }

    @Test
    public void processMessage_customMetadataContentAndChecksum_backupCustomMetadataState() throws Exception {
        final BackupData fileNameMessage = BackupData.newBuilder().setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setFileName(TEST_CUSTOM_METADATA_FILE_NAME).build()).build();
        final BackupData message = BackupData.newBuilder().setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setContent(ByteString.copyFrom("ABCtre".getBytes())).build())
                .build();
        state.processMessage(fileNameMessage, "dummyStreamId");
        final BackupState nextState = state.processMessage(message, "dummyStreamId");
        assertTrue(nextState instanceof BackupCustomMetadataState);
        assertEquals(Arrays.asList("ABCtre"), Files.readAllLines(fragmentFolder.getCustomMetadataFileFolder().resolve(TEST_CUSTOM_METADATA_FILE_NAME)));

        final String checksumValue = getChecksum();
        final BackupData checksum = BackupData.newBuilder().setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setChecksum(checksumValue).build()).build();
        final BackupState nextState2 = state.processMessage(checksum,"dummyStreamId");
        assertEquals(Arrays.asList(checksumValue), Files.readAllLines(fragmentFolder.getCustomMetadataFileFolder().resolve(TEST_CUSTOM_METADATA_FILE_NAME + ".md5")));
        assertTrue(nextState2 instanceof BackupCustomMetadataState);
    }

    @Test(expected = BackupServiceException.class)
    public void processMessage_backupFileChunk_exception() throws Exception {
        final BackupData fileNameMessage = BackupData.newBuilder().setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setFileName(TEST_CUSTOM_METADATA_FILE_NAME).build()).build();
        final BackupData message = BackupData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setContent(ByteString.copyFrom("abc".getBytes())).build()).build();
        state.processMessage(fileNameMessage, "dummyStreamId");
        state.processMessage(message, "dummyStreamId");
    }

    private String getChecksum() {
        final ChecksumCalculator checksumCalculator = new ChecksumCalculator();
        checksumCalculator.addBytes("ABCtre".getBytes());
        return checksumCalculator.getChecksum();
    }
}
