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

public class RestoreBackupFileTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private RestoreBackupFile restoreBackupFile;
    private RestoreStreamObserverTest restoreStreamObserverTest;
    private Path backup;

    @Before
    public void setup() throws IOException {
        final FragmentFolder fragmentFolder = new FragmentFolder(folder.getRoot().toPath());
        Files.createDirectories(fragmentFolder.getDataFileFolder());
        backup = fragmentFolder.getDataFileFolder();
        restoreStreamObserverTest = new RestoreStreamObserverTest();
        restoreBackupFile = new RestoreBackupFile(restoreStreamObserverTest, 512 * 1024, new S3Config());
    }

    @Test
    public void sendFile_sendBackupPath_sendsBackup() throws Exception {
        Files.write(backup.resolve("BackupFile.txt"), "ABCDEF-BackupFile".getBytes());
        restoreBackupFile.sendFile(backup.resolve("BackupFile.txt"));
        assertTrue(restoreStreamObserverTest.receivedMessage());
    }

    @Test
    public void sendFile_sendBackupPathChecksumFileMatches_sendsBackup() throws Exception {
        Files.write(backup.resolve("BackupFile.txt"), "ABCDEF-BackupFile".getBytes());
        final ChecksumCalculator calculator = new ChecksumCalculator();
        calculator.addBytes("ABCDEF-BackupFile".getBytes());
        Files.write(backup.resolve("BackupFile.txt.md5"), calculator.getChecksum().getBytes());
        restoreBackupFile.sendFile(backup.resolve("BackupFile.txt"));
        assertTrue(restoreStreamObserverTest.receivedMessage());
    }

    @Test(expected = ChecksumValidationException.class)
    public void sendFile_sendBackupPathChecksumFileMismatch_throwsError() throws Exception {
        Files.write(backup.resolve("BackupFile.txt"), "ABCDEF-BackupFile".getBytes());
        Files.write(backup.resolve("BackupFile.txt.md5"), "CAFEBABE".getBytes());
        restoreBackupFile.sendFile(backup.resolve("BackupFile.txt"));
        assertTrue(restoreStreamObserverTest.receivedMessage());
    }

    @Test(expected = RestoreDownloadException.class)
    public void sendFile_sendIncorrectBackupPath_throwsError() throws Exception {
        restoreBackupFile.sendFile(backup.resolve("BackupFile.txt"));
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
