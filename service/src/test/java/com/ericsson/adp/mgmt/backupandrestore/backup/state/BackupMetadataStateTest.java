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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupMetadataWriter;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidIdException;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.FragmentFolder;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.BackupFileChunk;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.util.Arrays;

public class BackupMetadataStateTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private BackupMetadataState state;
    private Metadata metadata;
    private CreateBackupJob job;
    private IdValidator idValidator;

    @Before
    public void setUp() {
        final BackupMetadataWriter backupMetadataWriter = new BackupMetadataWriter();
        backupMetadataWriter.setJsonService(new JsonService());

        metadata = Metadata
                .newBuilder()
                .setAgentId("agentID")
                .setFragment(Fragment.newBuilder().setFragmentId("fragmentId").setSizeInBytes("bytes").setVersion("version"))
                .setBackupName("backupName")
                .build();

        job = createMock(CreateBackupJob.class);
        expect(job.getAwsConfig()).andReturn(new S3Config()).anyTimes();
        expect(job.getFragmentFolder(metadata)).andReturn(new FragmentFolder(folder.getRoot().toPath()));
        job.receiveNewFragment(metadata.getAgentId(), metadata.getFragment().getFragmentId());
        expectLastCall();
        replay(job);

        idValidator = createMock(IdValidator.class);

        state = new BackupMetadataState(backupMetadataWriter, job, idValidator);
    }

    @Test
    public void processMessage_metadata_writeMetadataToDiskAndMoveToNextState() throws Exception {
        final BackupData message = BackupData
                .newBuilder()
                .setDataMessageType(DataMessageType.METADATA)
                .setMetadata(metadata).build();

        idValidator.validateId(anyObject());
        expectLastCall();
        replay(idValidator);

        final BackupState newState = state.processMessage(message, "dummyStreamId");

        assertTrue(newState instanceof BackupFileDataState);
        assertEquals(Arrays.asList("{\"fragmentId\":\"fragmentId\",\"version\":\"version\",\"sizeInBytes\":\"bytes\",\"customInformation\":{}}"),
                Files.readAllLines(folder.getRoot().toPath().resolve("Fragment.json")));
        verify(job);
    }

    @Test(expected = BackupServiceException.class)
    public void processMessage_backupFileData_throwsException() throws Exception {
        final BackupData message = BackupData
                .newBuilder()
                .setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk
                        .newBuilder()
                        .setContent(ByteString.copyFrom("abc".getBytes()))
                        .build()).build();
        state.processMessage(message, "dummyStreamId");
    }

    @Test(expected = BackupServiceException.class)
    public void processMessage_invalidMetadata_throwsException() {
        final BackupData message = BackupData
                .newBuilder()
                .setDataMessageType(DataMessageType.METADATA).build();
        state.processMessage(message, "dummyStreamId");
    }

    @Test(expected = BackupServiceException.class)
    public void processMessage_invalidFragmentId_throwsException() {
        final BackupData message = BackupData
                .newBuilder()
                .setDataMessageType(DataMessageType.METADATA)
                .setMetadata(Metadata.newBuilder().setFragment(Fragment.newBuilder()
                        .setFragmentId("abc/hy")
                        .setSizeInBytes("size")
                        .setVersion("version")
                        .build()).build())
                .build();

        idValidator.validateId(anyObject());
        expectLastCall().andThrow(new InvalidIdException(""));
        replay(idValidator);

        state.processMessage(message, "dummyStreamId");
    }

}
