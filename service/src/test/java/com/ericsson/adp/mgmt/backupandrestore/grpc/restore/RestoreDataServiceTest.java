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
package com.ericsson.adp.mgmt.backupandrestore.grpc.restore;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupMetadataWriter;
import com.ericsson.adp.mgmt.backupandrestore.exception.RestoreLocationDoesNotExistException;
import com.ericsson.adp.mgmt.backupandrestore.job.FragmentFolder;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProviderFactory;
import com.ericsson.adp.mgmt.backupandrestore.restore.ChecksumValidationException;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.data.RestoreData;
import com.ericsson.adp.mgmt.metadata.Fragment;
import io.grpc.stub.StreamObserver;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RestoreDataServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private RestoreStreamObserverTest restoreStreamObserverTest;
    private Metadata metadata;
    private RestoreJob job;
    private RestoreDataService restoreDataService;
    private BackupMetadataWriter backupMetadataWriter;
    private FragmentFolder fragmentFolder;

    @Before
    public void setup() throws IOException {
        fragmentFolder = new FragmentFolder(folder.getRoot().toPath());
        Files.createDirectories(fragmentFolder.getCustomMetadataFileFolder());
        Files.createDirectories(fragmentFolder.getDataFileFolder());
        restoreStreamObserverTest = new RestoreStreamObserverTest();
        backupMetadataWriter = new BackupMetadataWriter();
        job = EasyMock.createMock(RestoreJob.class);
        expect(job.getFragmentFolder(EasyMock.anyObject(Metadata.class))).andReturn(fragmentFolder).anyTimes();
        expect(job.getAwsConfig()).andReturn(new S3Config()).anyTimes();
        job.updateAgentChunkSize(anyString(), anyLong());
        expectLastCall().anyTimes();

        this.restoreDataService = new RestoreDataService();
        this.restoreDataService.setRestoreChunkSize(512);
        this.restoreDataService.setProvider(new PersistProviderFactory());
    }

    @Test
    public void processMessage_metadataSentWithBackupAndMetadataPresent_restoreDownloadComplete() throws Exception {
        Files.write(fragmentFolder.getDataFileFolder().resolve("BackupFile.txt"), "ABCDE-BackupFile".getBytes());
        Files.write(fragmentFolder.getCustomMetadataFileFolder().resolve("CustomMetadata.txt"), "ABCDE-CustomMetadata".getBytes());
        generateMetadataAndFragment();
        replay(job);
        this.restoreDataService.processMessage(metadata, job, restoreStreamObserverTest);
        assertTrue(restoreStreamObserverTest.didRestore());
    }

    @Test
    public void processMessage_metadataSentWithOnlyBackupPresent_restoreDownloadComplete() throws Exception {
        Files.write(fragmentFolder.getDataFileFolder().resolve("BackupFile.txt"), "ABCDE-BackupFile".getBytes());
        generateMetadataAndFragment();
        replay(job);
        this.restoreDataService.processMessage(metadata, job, restoreStreamObserverTest);
        assertTrue(restoreStreamObserverTest.didRestore());
    }

    @Test(expected = ChecksumValidationException.class)
    public void processMessage_metadataSentWithOnlyBackupPresentBackupChecksumMismatch_throwsError() throws Exception {
        Files.write(fragmentFolder.getDataFileFolder().resolve("BackupFile.txt"), "ABCDE-BackupFile".getBytes());
        Files.write(fragmentFolder.getDataFileFolder().resolve("BackupFile.txt.md5"), "CAFEBABE".getBytes());
        generateMetadataAndFragment();
        job.markBackupAsCorrupted();;
        expectLastCall().once();
        replay(job);
        this.restoreDataService.processMessage(metadata, job, restoreStreamObserverTest);
    }

    @Test(expected = RestoreLocationDoesNotExistException.class)
    public void processMessage_metadataButNoBackupPresent_throwsError() throws Exception {
        generateMetadataAndFragment();
        replay(job);
        this.restoreDataService.processMessage(metadata, job, restoreStreamObserverTest);
        assertFalse(restoreStreamObserverTest.didRestore());
    }

    private void generateMetadataAndFragment() {
        metadata = Metadata.newBuilder().setAgentId("agentID")
                .setFragment(Fragment.newBuilder().setFragmentId("fragmentId").setSizeInBytes("bytes").setVersion("version"))
                .setBackupName("backupName").build();
        backupMetadataWriter.setJsonService(new JsonService());
        backupMetadataWriter.storeFragment(fragmentFolder, metadata);
    }

    private class RestoreStreamObserverTest implements StreamObserver<RestoreData> {

        private boolean didRestore;

        @Override
        public void onNext(final RestoreData value) {
            didRestore = true;
        }

        @Override
        public void onError(final Throwable t) {
            didRestore = false;
        }

        @Override
        public void onCompleted() {

        }

        public boolean didRestore() {
            return didRestore;
        }

    }

}
