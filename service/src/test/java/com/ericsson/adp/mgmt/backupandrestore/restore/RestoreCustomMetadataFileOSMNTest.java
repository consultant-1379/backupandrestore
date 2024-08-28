/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.restore;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3Client;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3MultipartClient;

import org.easymock.EasyMock;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.backupandrestore.exception.RestoreDownloadException;
import com.ericsson.adp.mgmt.backupandrestore.job.FragmentFolder;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;
import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumCalculator;
import com.ericsson.adp.mgmt.data.RestoreData;

import io.findify.s3mock.S3Mock;
import io.grpc.stub.StreamObserver;

public class RestoreCustomMetadataFileOSMNTest {

    private static final S3Mock api = new S3Mock.Builder().withPort(28001).withInMemoryBackend().build();

    private static S3MultipartClient s3MultipartClient;
    private static String defaultBucketName;
    private static S3Config s3Config;

    private RestoreCustomMetadataFile restoreCustomMetadataFile;
    private RestoreStreamObserverTest restoreStreamObserverTest;
    private Path customMetadata = null;
    private FragmentFolder fragmentFolder;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void setupClass() {
        //set up the mock server
        api.start();
        defaultBucketName = "bro";
        s3Config = new S3Config();
        s3Config.setEnabled(true);
        s3Config.setDefaultBucketName(defaultBucketName);
        s3Config.setHost("http://0.0.0.0");
        s3Config.setPort("28001");
        s3Config.setAccessKeyName("ak");
        s3Config.setSecretKeyName("sk");
        s3Config.setConnectionTimeout(1000);
        s3Config.setRetriesStartUp(0);
        s3Config.setRetriesOperation(0);

        final KeyStoreService service = EasyMock.createMock(KeyStoreService.class);
        EasyMock.expect(service.getKeyStore(KeyStoreAlias.OSMN)).andReturn(null).anyTimes();
        EasyMock.replay(service);
        s3Config.setKeyStoreService(service);

        s3MultipartClient = new S3MultipartClient(s3Config);
        s3MultipartClient.createBucket();
    }

    @AfterClass
    public static void tearDownClass() {
        api.shutdown();
    }

    @Before
    public void setup() throws IOException {
        fragmentFolder = new FragmentFolder(folder.getRoot().toPath());
        customMetadata = fragmentFolder.getCustomMetadataFileFolder().resolve("CustomMetadata.txt");
        restoreStreamObserverTest = new RestoreStreamObserverTest();
        restoreCustomMetadataFile = new RestoreCustomMetadataFile(restoreStreamObserverTest, s3Config);
        s3MultipartClient.emptyBucket(defaultBucketName);
    }

    @Test
    public void sendCustomMetadataFile_getCustomMetadataPath_sendsCustomMetadata() throws Exception {
        s3MultipartClient.uploadObject(S3Client.toObjectKey(customMetadata), "ABCDEF-CustomMetadata".getBytes());
        restoreCustomMetadataFile.sendCustomMetadataFile(customMetadata);
        assertTrue(restoreStreamObserverTest.receivedMessage());
    }

    @Test
    public void sendCustomMetadataFile_getCustomMetadataPathChecksumFileMatches_sendsCustomMetadata() throws Exception {
        s3MultipartClient.uploadObject(S3Client.toObjectKey(customMetadata), "ABCDEF-CustomMetadata".getBytes());
        final ChecksumCalculator calculator = new ChecksumCalculator();
        calculator.addBytes("ABCDEF-CustomMetadata".getBytes());
        s3MultipartClient.uploadObject(S3Client.toObjectKey(fragmentFolder.getCustomMetadataFileFolder().resolve("CustomMetadata.txt.md5")), calculator.getChecksum().getBytes());
        restoreCustomMetadataFile.sendCustomMetadataFile(customMetadata);
        assertTrue(restoreStreamObserverTest.receivedMessage());
    }

    @Test(expected = ChecksumValidationException.class)
    public void sendCustomMetadataFile_sendBackupPathChecksumFileMismatch_throwsError() throws Exception {
        s3MultipartClient.uploadObject(S3Client.toObjectKey(customMetadata), "ABCDEF-CustomMetadata".getBytes());
        s3MultipartClient.uploadObject(S3Client.toObjectKey(fragmentFolder.getCustomMetadataFileFolder().resolve("CustomMetadata.txt.md5")), "CAFEBABE".getBytes());
        restoreCustomMetadataFile.sendCustomMetadataFile(customMetadata);
        assertTrue(restoreStreamObserverTest.receivedMessage());
    }

    @Test(expected = RestoreDownloadException.class)
    public void sendCustomMetadataFile_sendIncorrectPath_throwsError() throws Exception {
        restoreCustomMetadataFile.sendCustomMetadataFile(customMetadata);
    }

    private class RestoreStreamObserverTest implements StreamObserver<RestoreData> {

        private boolean receivedMessage;

        @Override
        public void onNext(final RestoreData value) {
            receivedMessage = true;
        }

        @Override
        public void onError(final Throwable t) {
            receivedMessage = false;
        }

        @Override
        public void onCompleted() {
            //not needed.
        }

        public boolean receivedMessage() {
            return receivedMessage;
        }

    }
}
