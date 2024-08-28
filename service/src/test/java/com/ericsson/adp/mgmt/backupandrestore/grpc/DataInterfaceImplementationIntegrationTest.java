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

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupMetadataWriter;
import com.ericsson.adp.mgmt.backupandrestore.grpc.backup.BackupDataStream;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.FragmentFolder;
import com.ericsson.adp.mgmt.backupandrestore.job.Job;
import com.ericsson.adp.mgmt.backupandrestore.job.JobExecutor;
import com.ericsson.adp.mgmt.backupandrestore.job.QueueingJobExecutor;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.test.IntegrationTest;
import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumCalculator;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.ericsson.adp.mgmt.bro.api.filetransfer.FileChunkServiceUtil;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.BackupFileChunk;
import com.ericsson.adp.mgmt.data.CustomMetadataFileChunk;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.data.RestoreData;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.google.protobuf.ByteString;

import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

public class DataInterfaceImplementationIntegrationTest extends IntegrationTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Autowired
    private DataInterfaceImplementation dataInterface;
    @Autowired
    private JobExecutor jobExecutor;

    private Path backupLocation;
    private ServerCallStreamObserver<RestoreData> stub;
    private FragmentFolder fragmentTestFolder;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        stub = mock(ServerCallStreamObserver.class);
        when(stub.isReady()).thenReturn(true);

        backupLocation = folder.getRoot().toPath();
    }

    @After
    public void tearDown() throws Exception {
        this.dataInterface.setJobExecutor(this.jobExecutor);
    }

    @Test
    public void backup_inputStream_backupData() throws Exception {
        final Fragment fragment = Fragment.newBuilder().setFragmentId("fragment").setSizeInBytes("sizeInByte").setVersion("version").build();
        final Metadata metadata = Metadata.newBuilder().setFragment(fragment).setAgentId("abc").setBackupName("backup").build();

        //Setup all of the messages for the data file
        final String testDataFileName = "TestingDataFileIntegration";
        final BackupData fileNameMessage = BackupData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setFileName(testDataFileName).build()).build();
        final BackupData backupFileMessage = BackupData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setContent(ByteString.copyFrom("ABCtre".getBytes())).build()).build();
        final BackupData backupFileChecksum = BackupData.newBuilder().setDataMessageType(DataMessageType.BACKUP_FILE)
                .setBackupFileChunk(BackupFileChunk.newBuilder().setChecksum(getChecksum("ABCtre")).build()).build();

        //Setup all of the messages for the custom metadata file
        final String testCustomMetadataFileName = "TestingCustomMetadataIntegration";
        final BackupData fileNameCustomDataMessage = BackupData.newBuilder().setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setFileName(testCustomMetadataFileName).build()).build();
        final BackupData customMetadataMessage2 = BackupData.newBuilder().setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setContent(ByteString.copyFrom("ABCtre".getBytes())).build())
                .build();
        final BackupData customMetadataChecksum = BackupData.newBuilder().setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE)
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setChecksum(getChecksum("ABCtre")).build()).build();

        prepareBackupJob(metadata);

        final StreamObserver<BackupData> backupStream = dataInterface.backup(new StreamObserverStub<>());

        final BackupData metadataMessage = BackupData.newBuilder().setDataMessageType(DataMessageType.METADATA).setMetadata(metadata).build();
        assertTrue(backupStream instanceof BackupDataStream);

        //send and confirm the metadata
        backupStream.onNext(metadataMessage);

        assertEquals(Arrays.asList("{\"fragmentId\":\"fragment\",\"version\":\"version\",\"sizeInBytes\":\"sizeInByte\",\"customInformation\":{}}"),
                Files.readAllLines(fragmentTestFolder.getRootFolder().resolve("Fragment.json")));

        //send and confirm the backup data
        backupStream.onNext(fileNameMessage);
        backupStream.onNext(backupFileMessage);
        backupStream.onNext(backupFileChecksum);

        assertEquals(Arrays.asList("ABCtre"), Files.readAllLines(fragmentTestFolder.getDataFileFolder().resolve(testDataFileName)));

        //send and confirm the custom metadata
        backupStream.onNext(fileNameCustomDataMessage);
        backupStream.onNext(customMetadataMessage2);

        backupStream.onNext(customMetadataChecksum);

        assertEquals(Arrays.asList("ABCtre"), Files.readAllLines(fragmentTestFolder.getCustomMetadataFileFolder().resolve(testCustomMetadataFileName)));

        backupStream.onCompleted();
    }

    @Test
    public void restore_metadata_sendsBackupFileToAgent() throws Exception {
        final Exception e = new RuntimeException("Exception occured");
        final Metadata metadata = Metadata.newBuilder().setAgentId("agentID")
                .setFragment(Fragment.newBuilder().setFragmentId("fragmentId").setSizeInBytes("bytes").setVersion("version"))
                .setBackupName("backupName").build();

        prepareRestoreJob(metadata);
        generateFragmentJson(metadata);

        final String testCustomMetadataFileName = "customMetadata.txt";
        final String testdataFileName = "BackupFile.txt";
        final String data = "ABCDE-BackupFile";
        final String customData = "ABCDE-CustomMetadata";

        Files.createDirectories(fragmentTestFolder.getCustomMetadataFileFolder());
        Files.createDirectories(fragmentTestFolder.getDataFileFolder());
        Files.write(fragmentTestFolder.getDataFileFolder().resolve(testdataFileName), data.getBytes());
        Files.write(fragmentTestFolder.getCustomMetadataFileFolder().resolve(testCustomMetadataFileName), customData.getBytes());

        dataInterface.restore(metadata, stub);
        verify(stub, times(0)).onError(e);
        verify(stub, times(1)).onNext(RestoreData.newBuilder().setBackupFileChunk(BackupFileChunk.newBuilder().setFileName(testdataFileName).build())
                .setDataMessageType(DataMessageType.BACKUP_FILE).build());
        FileChunkServiceUtil.processFileChunks(fragmentTestFolder.getDataFileFolder().resolve(testdataFileName).toString(),
                (chunk, bytesReadInChunk) -> {
                    verify(stub).onNext(RestoreData.newBuilder()
                            .setBackupFileChunk(BackupFileChunk.newBuilder().setContent(ByteString.copyFrom(chunk, 0, bytesReadInChunk)).build())
                            .setDataMessageType(DataMessageType.BACKUP_FILE).build());
                });
        verify(stub, times(1)).onNext(RestoreData.newBuilder().setBackupFileChunk(BackupFileChunk.newBuilder().setChecksum(getChecksum(data)).build())
                .setDataMessageType(DataMessageType.BACKUP_FILE).build());
        verify(stub, times(0)).onError(e);

        verify(stub, times(1)).onNext(RestoreData.newBuilder()
                .setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setFileName(testCustomMetadataFileName).build())
                .setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE).build());

        FileChunkServiceUtil.processFileChunks(fragmentTestFolder.getCustomMetadataFileFolder().resolve(testCustomMetadataFileName).toString(),
                (chunk, bytesReadInChunk) -> {
                    verify(stub).onNext(RestoreData.newBuilder()
                            .setCustomMetadataFileChunk(
                                    CustomMetadataFileChunk.newBuilder().setContent(ByteString.copyFrom(chunk, 0, bytesReadInChunk)).build())
                            .setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE).build());
                });

        verify(stub, times(1)).onNext(
                RestoreData.newBuilder().setCustomMetadataFileChunk(CustomMetadataFileChunk.newBuilder().setChecksum(getChecksum(customData)).build())
                        .setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE).build());

    }

    @Test
    public void restore_metadata_sendsOnlyBackupFileToAgent() throws Exception {
        final String testdataFileName = "sendOnlyTestData.txt";
        final Exception e = new RuntimeException("Exception occured");

        final Metadata metadata = Metadata.newBuilder().setAgentId("RestoreTestDummyAgent")
                .setFragment(Fragment.newBuilder().setFragmentId("fragmentId").setSizeInBytes("bytes").setVersion("version"))
                .setBackupName("backupName").build();

        prepareRestoreJob(metadata);
        generateFragmentJson(metadata);
        Files.createDirectories(fragmentTestFolder.getDataFileFolder());
        Files.write(fragmentTestFolder.getDataFileFolder().resolve(testdataFileName), "ABCDE-BackupFile".getBytes());
        dataInterface.restore(metadata, stub);
        verify(stub, times(0)).onError(e);
        verify(stub, times(1)).onNext(RestoreData.newBuilder().setBackupFileChunk(BackupFileChunk.newBuilder().setFileName(testdataFileName).build())
                .setDataMessageType(DataMessageType.BACKUP_FILE).build());
        FileChunkServiceUtil.processFileChunks(fragmentTestFolder.getDataFileFolder().resolve(testdataFileName).toString(),
                (chunk, bytesReadInChunk) -> {
                    verify(stub).onNext(RestoreData.newBuilder()
                            .setBackupFileChunk(BackupFileChunk.newBuilder().setContent(ByteString.copyFrom(chunk, 0, bytesReadInChunk)).build())
                            .setDataMessageType(DataMessageType.BACKUP_FILE).build());
                });
        verify(stub, times(1))
                .onNext(RestoreData.newBuilder().setBackupFileChunk(BackupFileChunk.newBuilder().setChecksum(getChecksum("ABCDE-BackupFile")).build())
                        .setDataMessageType(DataMessageType.BACKUP_FILE).build());

    }

    private String getChecksum(final String content) {
        final ChecksumCalculator checksumCalculator = new ChecksumCalculator();
        checksumCalculator.addBytes(content.getBytes());
        return checksumCalculator.getChecksum();
    }

    private void prepareBackupJob(final Metadata metadata) throws Exception {
        Files.createDirectories(backupLocation);

        fragmentTestFolder = new FragmentFolder(backupLocation);

        final CreateBackupJob job = createMock(CreateBackupJob.class);
        expect(job.getFragmentFolder(metadata)).andReturn(fragmentTestFolder);
        expect(job.getAwsConfig()).andReturn(new S3Config()).anyTimes();
        job.receiveNewFragment("abc", "fragment");
        expectLastCall();
        job.fragmentSucceeded("abc", "fragment");
        expectLastCall();
        job.updateAgentChunkSize(anyString(), anyLong());
        expectLastCall().anyTimes();

        final JobExecutor jobExecutor = createMock(QueueingJobExecutor.class);
        expect(jobExecutor.getRunningJobs()).andReturn(List.of((Job) job)).anyTimes();
        replay(job, jobExecutor);

        dataInterface.setJobExecutor(jobExecutor);
    }

    private void prepareRestoreJob(final Metadata metadata) throws Exception {
        Files.createDirectories(backupLocation);

        fragmentTestFolder = new FragmentFolder(backupLocation);

        final RestoreJob job = createMock(RestoreJob.class);
        expect(job.getFragmentFolder(metadata)).andReturn(fragmentTestFolder).anyTimes();
        expect(job.getAwsConfig()).andReturn(new S3Config()).anyTimes();
        job.updateAgentChunkSize(anyString(), anyLong());
        expectLastCall().anyTimes();

        final JobExecutor jobExecutor = createMock(QueueingJobExecutor.class);
        expect(jobExecutor.getRunningJobs()).andReturn(List.of((Job) job)).anyTimes();
        replay(job, jobExecutor);

        dataInterface.setJobExecutor(jobExecutor);
    }

    private void generateFragmentJson(final Metadata metadata) {
        final BackupMetadataWriter backupMetadataWriter = new BackupMetadataWriter();
        backupMetadataWriter.setJsonService(new JsonService());
        backupMetadataWriter.storeFragment(fragmentTestFolder, metadata);
    }

    private class StreamObserverStub<T> implements StreamObserver<T> {

        @Override
        public void onCompleted() {
            //Not needed;
        }

        @Override
        public void onError(final Throwable arg0) {
            //Not needed;
        }

        @Override
        public void onNext(final T arg0) {
            //Not needed;
        }

    }

}
