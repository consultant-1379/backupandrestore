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
package com.ericsson.adp.mgmt.backupandrestore.grpc;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.grpc.backup.BackupDataStream;
import com.ericsson.adp.mgmt.backupandrestore.grpc.backup.UnexpectedBackupDataStream;
import com.ericsson.adp.mgmt.backupandrestore.grpc.restore.RestoreDataService;
import com.ericsson.adp.mgmt.backupandrestore.grpc.restore.RestoreFragmentStream;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.ExportBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.Job;
import com.ericsson.adp.mgmt.backupandrestore.job.JobExecutor;
import com.ericsson.adp.mgmt.backupandrestore.job.QueueingJobExecutor;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.data.RestoreData;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.google.protobuf.Empty;

import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

public class DataInterfaceImplementationTest {

    private DataInterfaceImplementation dataInterfaceImplementation;
    private JobExecutor jobExecutor;

    @Before
    public void setup() {
        jobExecutor = createMock(QueueingJobExecutor.class);

        this.dataInterfaceImplementation = new DataInterfaceImplementation();
        this.dataInterfaceImplementation.setJobExecutor(jobExecutor);
    }

    @Test
    public void backupDataStreamIdRemoved() {
        DataInterfaceImplementation.backupDataStreamIds.add("dummyStreamId");

        DataInterfaceImplementation.removeStreamId("dummyStreamId");

        assertTrue(DataInterfaceImplementation.getBackupDataStreamIds().isEmpty());
    }

    @Test
    public void backup_orchestratorHasJobRunning_acceptsBackup() throws Exception {
        final CreateBackupJob job = createMock(CreateBackupJob.class);
        expect(jobExecutor.getRunningJobs()).andReturn(List.of((Job) job)).anyTimes();
        replay(jobExecutor);

        final StreamObserver<BackupData> observer = dataInterfaceImplementation.backup(null);

        assertTrue(observer instanceof BackupDataStream);
    }

    @Test
    public void backup_orchestratorHasAnyJobOtherThanCreateBackupRunning_doesNotAcceptBackup() throws Exception {
        final RestoreJob job = createMock(RestoreJob.class);
        expect(jobExecutor.getRunningJobs()).andReturn(List.of((Job) job)).anyTimes();
        replay(jobExecutor);

        final OrchestratorStream<Empty> orchestratorStream = new OrchestratorStream<>();
        final StreamObserver<BackupData> observer = dataInterfaceImplementation.backup(orchestratorStream);

        assertTrue(observer instanceof UnexpectedBackupDataStream);
        assertTrue(orchestratorStream.receivedException());
    }

    @Test
    public void backup_orchestratorHasNoJobRunning_doesNotAcceptBackup() throws Exception {
        expect(jobExecutor.getRunningJobs()).andReturn(new ArrayList<Job>()).anyTimes();
        replay(jobExecutor);

        final OrchestratorStream<Empty> orchestratorStream = new OrchestratorStream<>();
        final StreamObserver<BackupData> observer = dataInterfaceImplementation.backup(orchestratorStream);

        assertTrue(observer instanceof UnexpectedBackupDataStream);
        assertTrue(orchestratorStream.receivedException());
    }

    @Test
    public void restore_orchestratorHasJobRunning_acceptsRestore() throws Exception {
        final Metadata metadata = Metadata.getDefaultInstance();
        final OrchestratorStream<RestoreData> orchestratorRestoreStream = new OrchestratorStream<>();

        final RestoreJob job = createMock(RestoreJob.class);
        expect(jobExecutor.getRunningJobs()).andReturn(List.of((Job) job)).anyTimes();
        replay(jobExecutor);

        final RestoreDataServiceStub restoreDataService = new RestoreDataServiceStub(false);

        dataInterfaceImplementation.setRestoreDataService(restoreDataService);

        dataInterfaceImplementation.restore(metadata, orchestratorRestoreStream);

        assertTrue(orchestratorRestoreStream.completed());
        assertEquals(metadata, restoreDataService.getMetadata());
        assertEquals(job, restoreDataService.getJob());
        assertTrue(restoreDataService.getStream() instanceof RestoreFragmentStream);
    }

    @Test
    public void restore_orchestratorHasNoJobRunning_doesNotAcceptRestore() throws Exception {
        expect(jobExecutor.getRunningJobs()).andReturn(new ArrayList<Job>()).anyTimes();
        replay(jobExecutor);

        final Metadata metadata = Metadata.newBuilder().setAgentId("agentID")
                .setFragment(Fragment.newBuilder().setFragmentId("fragmentId").setSizeInBytes("bytes").setVersion("version"))
                .setBackupName("backupName").build();
        final OrchestratorStream<RestoreData> orchestratorRestoreStream = new OrchestratorStream<>();
        dataInterfaceImplementation.restore(metadata, orchestratorRestoreStream);

        assertTrue(orchestratorRestoreStream.receivedException());
        assertFalse(orchestratorRestoreStream.completed());
    }

    @Test
    public void restore_orchestratorHasAnyJobOtherThanRestoreRunning_doesNotAcceptRestore() throws Exception {
        final Metadata metadata = Metadata.getDefaultInstance();
        final OrchestratorStream<RestoreData> orchestratorRestoreStream = new OrchestratorStream<>();

        final CreateBackupJob job = createMock(CreateBackupJob.class);
        job.handleUnexpectedDataChannel(metadata);
        expectLastCall();
        expect(jobExecutor.getRunningJobs()).andReturn(List.of((Job) job)).anyTimes();

        replay(jobExecutor, job);

        dataInterfaceImplementation.restore(metadata, orchestratorRestoreStream);

        verify(job);
    }

    @Test
    public void restore_orchestratorHasParallelJobsOtherThanRestoreRunning_doesNotAcceptRestore() throws Exception {
        final Metadata metadata = Metadata.getDefaultInstance();
        final OrchestratorStream<RestoreData> orchestratorRestoreStream = new OrchestratorStream<>();

        final ExportBackupJob exportJob = createMock(ExportBackupJob.class);

        final CreateBackupJob backupJob = createMock(CreateBackupJob.class);
        backupJob.handleUnexpectedDataChannel(metadata);
        expectLastCall();
        expect(jobExecutor.getRunningJobs()).andReturn(List.of((Job) exportJob, (Job) backupJob)).anyTimes();

        replay(jobExecutor, backupJob, exportJob);

        dataInterfaceImplementation.restore(metadata, orchestratorRestoreStream);

        verify(backupJob, exportJob);
    }

    @Test
    public void restore_restoreThrowsException_closesChannel() throws Exception {
        final Metadata metadata = Metadata.getDefaultInstance();
        final OrchestratorStream<RestoreData> orchestratorRestoreStream = new OrchestratorStream<>();

        final RestoreJob job = createMock(RestoreJob.class);
        expect(jobExecutor.getRunningJobs()).andReturn(List.of((Job) job)).anyTimes();
        replay(jobExecutor);

        dataInterfaceImplementation.setRestoreDataService(new RestoreDataServiceStub(true));

        dataInterfaceImplementation.restore(metadata, orchestratorRestoreStream);

        assertTrue(orchestratorRestoreStream.receivedException());
        assertFalse(orchestratorRestoreStream.completed());
    }

    private class OrchestratorStream<T> extends ServerCallStreamObserver<T> {

        private boolean receivedException;
        private boolean completed;

        public boolean receivedException() {
            return receivedException;
        }

        public boolean completed() {
            return completed;
        }

        @Override
        public void onNext(final T value) {

        }

        @Override
        public void onError(final Throwable args) {
            this.receivedException = true;

        }

        @Override
        public void onCompleted() {
            this.completed = true;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public void setCompression(final String arg0) {

        }

        @Override
        public void setOnCancelHandler(final Runnable arg0) {

        }

        @Override
        public void disableAutoInboundFlowControl() {

        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void request(final int arg0) {

        }

        @Override
        public void setMessageCompression(final boolean arg0) {

        }

        @Override
        public void setOnReadyHandler(final Runnable arg0) {

        }

    }

    private class RestoreDataServiceStub extends RestoreDataService {

        private Metadata metadata;
        private RestoreJob job;
        private StreamObserver<RestoreData> stream;
        private final boolean shouldThrowException;

        public RestoreDataServiceStub(final boolean shouldThrowException) {
            this.shouldThrowException = shouldThrowException;
        }

        @Override
        public void processMessage(final Metadata metadata, final RestoreJob job, final StreamObserver<RestoreData> stream) {
            if(shouldThrowException) {
                throw new RuntimeException("Error");
            }
            this.metadata = metadata;
            this.job = job;
            this.stream = stream;
        }

        public Metadata getMetadata() {
            return metadata;
        }

        public RestoreJob getJob() {
            return job;
        }

        public StreamObserver<RestoreData> getStream() {
            return stream;
        }

    }
}
