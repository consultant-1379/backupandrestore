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
package com.ericsson.adp.mgmt.backupandrestore.restore;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.backupandrestore.exception.RestoreDownloadException;
import com.ericsson.adp.mgmt.backupandrestore.job.FragmentFolder;
import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumCalculator;
import com.ericsson.adp.mgmt.data.RestoreData;

import io.grpc.stub.StreamObserver;

public class RestoreCustomMetadataFileTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private RestoreCustomMetadataFile restoreCustomMetadataFile;
    private RestoreStreamObserverTest restoreStreamObserverTest;
    private Path customMetadata = null;
    private FragmentFolder fragmentFolder;

    @Before
    public void setup() throws IOException {
        fragmentFolder = new FragmentFolder(folder.getRoot().toPath());
        Files.createDirectories(fragmentFolder.getCustomMetadataFileFolder());
        customMetadata = fragmentFolder.getCustomMetadataFileFolder().resolve("CustomMetadata.txt");
        restoreStreamObserverTest = new RestoreStreamObserverTest();
        restoreCustomMetadataFile = new RestoreCustomMetadataFile(restoreStreamObserverTest, new S3Config());
    }

    @Test
    public void sendCustomMetadataFile_getCustomMetadataPath_sendsCustomMetadata() throws Exception {
        Files.write(fragmentFolder.getCustomMetadataFileFolder().resolve("CustomMetadata.txt"), "ABCDEF-CustomMetadata".getBytes());
        restoreCustomMetadataFile.sendCustomMetadataFile(customMetadata);
        assertTrue(restoreStreamObserverTest.receivedMessage());
    }

    @Test
    public void sendCustomMetadataFile_getCustomMetadataPathChecksumFileMatches_sendsCustomMetadata() throws Exception {
        Files.write(fragmentFolder.getCustomMetadataFileFolder().resolve("CustomMetadata.txt"), "ABCDEF-CustomMetadata".getBytes());
        final ChecksumCalculator calculator = new ChecksumCalculator();
        calculator.addBytes("ABCDEF-CustomMetadata".getBytes());
        Files.write(fragmentFolder.getCustomMetadataFileFolder().resolve("CustomMetadata.txt.md5"), calculator.getChecksum().getBytes());
        restoreCustomMetadataFile.sendCustomMetadataFile(customMetadata);
        assertTrue(restoreStreamObserverTest.receivedMessage());
    }

    @Test(expected = ChecksumValidationException.class)
    public void sendCustomMetadataFile_sendBackupPathChecksumFileMismatch_throwsError() throws Exception {
        Files.write(fragmentFolder.getCustomMetadataFileFolder().resolve("CustomMetadata.txt"), "ABCDEF-CustomMetadata".getBytes());
        Files.write(fragmentFolder.getCustomMetadataFileFolder().resolve("CustomMetadata.txt.md5"), "CAFEBABE".getBytes());
        restoreCustomMetadataFile.sendCustomMetadataFile(customMetadata);
        assertTrue(restoreStreamObserverTest.receivedMessage());
    }

    @Test(expected = RestoreDownloadException.class)
    public void sendCustomMetadataFile_sendIncorrectPath_throwsError() throws Exception {
        restoreCustomMetadataFile = new RestoreCustomMetadataFile(restoreStreamObserverTest, new S3Config());
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
