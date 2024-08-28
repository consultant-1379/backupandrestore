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
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.FragmentFolder;
import com.ericsson.adp.mgmt.backupandrestore.test.EmptyStreamObserver;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.BackupFileChunk;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.util.Arrays;

public class BackupDataStreamTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private BackupDataStream streamObserver;
    private BackupMetadataWriter backupMetadataWriter;

    @Before
    public void setup() {
        backupMetadataWriter = new BackupMetadataWriter();
        backupMetadataWriter.setJsonService(new JsonService());
    }

    @Test
    public void onNext_metadataMessage_writeFragment() throws Exception {
        final CreateBackupJob job = EasyMock.createMock(CreateBackupJob.class);
        expect(job.getFragmentFolder(EasyMock.anyObject(Metadata.class))).andReturn(new FragmentFolder(folder.getRoot().toPath()));
        expect(job.getAwsConfig()).andReturn(new S3Config()).anyTimes();
        job.receiveNewFragment("agentID", "fragmentId");
        EasyMock.expectLastCall();
        EasyMock.replay(job);

        streamObserver = new BackupDataStream(backupMetadataWriter, job, new EmptyStreamObserver(), mockIdValidator());

        final BackupData message = BackupData
                .newBuilder()
                .setDataMessageType(DataMessageType.METADATA)
                .setMetadata(Metadata
                        .newBuilder()
                        .setAgentId("agentID")
                        .setFragment(Fragment.newBuilder().setFragmentId("fragmentId").setSizeInBytes("bytes").setVersion("version"))
                        .setBackupName("backupName")
                        .build()).build();

        streamObserver.onNext(message);

        assertEquals(Arrays.asList("{\"fragmentId\":\"fragmentId\",\"version\":\"version\",\"sizeInBytes\":\"bytes\",\"customInformation\":{}}"), Files.readAllLines(folder.getRoot().toPath().resolve("Fragment.json")));
        EasyMock.verify(job);
    }

    @Test
    public void onNext_unexpectedMetadataMessage_sendErrorMessageAndUpdateJob() throws Exception {
        final CreateBackupJob job = EasyMock.createMock(CreateBackupJob.class);
        expect(job.getFragmentFolder(EasyMock.anyObject(Metadata.class))).andReturn(new FragmentFolder(folder.getRoot().toPath()));
        expect(job.getAwsConfig()).andReturn(new S3Config()).anyTimes();
        job.receiveNewFragment("agentID", "fragmentId");
        EasyMock.expectLastCall();
        job.fragmentFailed("agentID", "fragmentId");
        EasyMock.expectLastCall();
        EasyMock.replay(job);

        final EmptyStreamObserver emptyStreamObserver = createMock(EmptyStreamObserver.class);

        streamObserver = new BackupDataStream(backupMetadataWriter, job, emptyStreamObserver, mockIdValidator());

        final BackupData message = BackupData
                .newBuilder()
                .setDataMessageType(DataMessageType.METADATA)
                .setMetadata(Metadata
                        .newBuilder()
                        .setAgentId("agentID")
                        .setFragment(Fragment.newBuilder().setFragmentId("fragmentId").setSizeInBytes("bytes").setVersion("version"))
                        .setBackupName("backupName")
                        .build()).build();

        emptyStreamObserver.onError(anyObject(StatusRuntimeException.class));
        expectLastCall();
        replay(emptyStreamObserver);

        streamObserver.onNext(message);
        streamObserver.onNext(message);

        verify(emptyStreamObserver);
        verify(job);
    }

    @Test
    public void onNext_unexpectedBackupFileMessage_sendErrorMessageAndUpdateJob() throws Exception {
        final CreateBackupJob job = EasyMock.createMock(CreateBackupJob.class);
        EasyMock.replay(job);

        final EmptyStreamObserver emptyStreamObserver = createMock(EmptyStreamObserver.class);

        streamObserver = new BackupDataStream(backupMetadataWriter, job, emptyStreamObserver, mockIdValidator());

        final BackupData message = BackupData
                .newBuilder()
                .setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk
                        .newBuilder()
                        .setContent(ByteString.copyFrom("newBackup".getBytes()))
                        .build()).build();

        emptyStreamObserver.onError(anyObject(StatusRuntimeException.class));
        expectLastCall();
        replay(emptyStreamObserver);

        streamObserver.onNext(message);

        verify(emptyStreamObserver);
        verify(job);
    }


    @Test
    public void onNext_invalidMetadataMessage_sendErrorMessageWithoutUpdatingJob() throws Exception {
        final CreateBackupJob job = EasyMock.createMock(CreateBackupJob.class);
        EasyMock.replay(job);

        final EmptyStreamObserver emptyStreamObserver = createMock(EmptyStreamObserver.class);

        streamObserver = new BackupDataStream(backupMetadataWriter, job, emptyStreamObserver, mockIdValidator());

        final BackupData message = BackupData
                .newBuilder()
                .setDataMessageType(DataMessageType.METADATA)
                .setMetadata(Metadata
                        .newBuilder()
                        .build()).build();

        emptyStreamObserver.onError(anyObject(StatusRuntimeException.class));
        expectLastCall();
        replay(emptyStreamObserver);

        streamObserver.onNext(message);

        verify(emptyStreamObserver);
        verify(job);
    }

    @Test
    public void onError_notHoldingMetadata_failsStateWithoutUpdatingJob() throws Exception {
        final CreateBackupJob job = EasyMock.createMock(CreateBackupJob.class);
        EasyMock.replay(job);

        final EmptyStreamObserver emptyStreamObserver = createMock(EmptyStreamObserver.class);

        streamObserver = new BackupDataStream(backupMetadataWriter, job, emptyStreamObserver, mockIdValidator());

        streamObserver.onError(new RuntimeException());

        verify(job);
    }

    @Test
    public void onError_holdingMetadata_failsStateAndUpdatesJob() throws Exception {
        final CreateBackupJob job = EasyMock.createMock(CreateBackupJob.class);
        expect(job.getFragmentFolder(EasyMock.anyObject(Metadata.class))).andReturn(new FragmentFolder(folder.getRoot().toPath()));
        expect(job.getAwsConfig()).andReturn(new S3Config()).anyTimes();
        job.receiveNewFragment("agentID", "fragmentId");
        EasyMock.expectLastCall();
        job.fragmentFailed("agentID", "fragmentId");
        EasyMock.expectLastCall();
        EasyMock.replay(job);

        final EmptyStreamObserver emptyStreamObserver = createMock(EmptyStreamObserver.class);

        streamObserver = new BackupDataStream(backupMetadataWriter, job, emptyStreamObserver, mockIdValidator());

        final BackupData message = BackupData
                .newBuilder()
                .setDataMessageType(DataMessageType.METADATA)
                .setMetadata(Metadata
                        .newBuilder()
                        .setAgentId("agentID")
                        .setFragment(Fragment.newBuilder().setFragmentId("fragmentId").setSizeInBytes("bytes").setVersion("version"))
                        .setBackupName("backupName")
                        .build()).build();

        streamObserver.onNext(message);
        streamObserver.onError(new RuntimeException());

        verify(job);
    }

    @Test
    public void onCompleted_withoutMetadata_failsStateWithoutUpdatingJobAndClosesStream() throws Exception {
        final CreateBackupJob job = EasyMock.createMock(CreateBackupJob.class);
        EasyMock.replay(job);

        final StreamObserverStub streamObserverStub = new StreamObserverStub();

        streamObserver = new BackupDataStream(null, job, streamObserverStub, mockIdValidator());

        streamObserver.onCompleted();

        verify(job);
        assertTrue(streamObserverStub.isNext());
        assertTrue(streamObserverStub.isCompleted());
    }

    @Test
    public void onCompleted_withMetadata_updatesJobWithFragmentSuccessAndClosesStream() throws Exception {
        final CreateBackupJob job = EasyMock.createMock(CreateBackupJob.class);
        expect(job.getAwsConfig()).andReturn(new S3Config()).anyTimes();
        expect(job.getFragmentFolder(EasyMock.anyObject(Metadata.class))).andReturn(new FragmentFolder(folder.getRoot().toPath()));
        job.receiveNewFragment("agentID", "fragmentId");
        EasyMock.expectLastCall();
        job.fragmentSucceeded("agentID", "fragmentId");
        EasyMock.expectLastCall();
        EasyMock.replay(job);

        final StreamObserverStub streamObserverStub = new StreamObserverStub();

        streamObserver = new BackupDataStream(backupMetadataWriter, job, streamObserverStub, mockIdValidator());

        final BackupData message = BackupData
                .newBuilder()
                .setDataMessageType(DataMessageType.METADATA)
                .setMetadata(Metadata
                        .newBuilder()
                        .setAgentId("agentID")
                        .setFragment(Fragment.newBuilder().setFragmentId("fragmentId").setSizeInBytes("bytes").setVersion("version"))
                        .setBackupName("backupName")
                        .build()).build();

        streamObserver.onNext(message);
        streamObserver.onCompleted();

        verify(job);
        assertTrue(streamObserverStub.isNext());
        assertTrue(streamObserverStub.isCompleted());
    }

    private IdValidator mockIdValidator() {
        final IdValidator idValidator = createMock(IdValidator.class);
        idValidator.validateId(EasyMock.anyObject());
        expectLastCall();
        replay(idValidator);
        return idValidator;
    }

    private class StreamObserverStub implements StreamObserver<Empty> {

        private boolean completed;
        private boolean next;

        @Override
        public void onCompleted() {
            completed = true;
        }

        @Override
        public void onError(final Throwable arg0) {

        }

        @Override
        public void onNext(final Empty arg0) {
            next = true;
        }

        public boolean isCompleted() {
            return completed;
        }

        public boolean isNext() {
            return next;
        }

    }

}
