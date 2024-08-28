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

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
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
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.Metadata;
import com.google.protobuf.ByteString;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public class BackupFileDataStateTest {

    private static final String TEST_DATA_FILE_NAME = "TestingDataFile";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path location = null;
    private BackupFileDataState state;
    private FragmentFolder fragmentFolder;
    private CreateBackupJob job;

    @Before
    public void setUp() {
        this.location = folder.getRoot().toPath();
        this.fragmentFolder = new FragmentFolder(location);
        this.job = createMock(CreateBackupJob.class);
        this.state = new BackupFileDataState(fragmentFolder, job, Optional.empty());
        expect(job.getAwsConfig()).andReturn(new S3Config()).anyTimes();
        job.updateAgentChunkSize(anyString(), anyLong());
        expectLastCall().anyTimes();
        replay(job);
    }

    @After
    public void tearDown() throws Exception {
        state.close();
    }

    @Test(expected = BackupServiceException.class)
    public void processMessage_metadata_exception() throws Exception {
        final BackupData fileNameMessage = BackupData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setFileName(TEST_DATA_FILE_NAME).build()).build();
        final BackupData message = BackupData.newBuilder().setDataMessageType(DataMessageType.METADATA).setMetadata(Metadata.newBuilder().build())
                .build();
        state.processMessage(fileNameMessage, "dummyStreamId");
        state.processMessage(message, "dummyStreamId");
    }

    @Test
    public void processMessage_backupFileContentAndChecksum_backupCustomMetadataState() throws Exception {
        final BackupData fileNameMessage = BackupData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setFileName(TEST_DATA_FILE_NAME).build()).build();

        final BackupData message = BackupData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setContent(ByteString.copyFrom("ABCtre".getBytes())).build()).build();

        state.processMessage(fileNameMessage, "dummyStreamId");
        final BackupState nextState = state.processMessage(message, "dummyStreamId");
        assertTrue(nextState instanceof BackupFileDataState);
        assertEquals(Arrays.asList("ABCtre"), Files.readAllLines(fragmentFolder.getDataFileFolder().resolve(TEST_DATA_FILE_NAME)));

        final String checksumValue = getChecksum();
        final BackupData checksum = BackupData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setChecksum(checksumValue)).build();
        final BackupState nextState2 = state.processMessage(checksum, "dummyStreamId");
        assertEquals(Arrays.asList(checksumValue), Files.readAllLines(fragmentFolder.getDataFileFolder().resolve(TEST_DATA_FILE_NAME + ".md5")));
        assertTrue(nextState2 instanceof BackupCustomMetadataState);
    }

    private String getChecksum() {
        final ChecksumCalculator checksumCalculator = new ChecksumCalculator();
        checksumCalculator.addBytes("ABCtre".getBytes());
        return checksumCalculator.getChecksum();
    }

}
