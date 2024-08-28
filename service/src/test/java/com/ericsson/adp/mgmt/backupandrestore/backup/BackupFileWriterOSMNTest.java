/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup;

import static org.junit.Assert.assertEquals;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3Client;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3MultipartClient;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;
import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumCalculator;

import io.findify.s3mock.S3Mock;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class BackupFileWriterOSMNTest {

    private static final S3Mock api = new S3Mock.Builder().withPort(28001).withInMemoryBackend().build();

    private static S3MultipartClient s3MultipartClient;
    private static String defaultBucketName;
    private static S3Config s3Config;

    private String backupObjectKey;
    private String backupChecksumObjectKey;
    private BackupFileWriter backupFileWriter;

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
    public void setUp() {
        backupObjectKey = S3Client.toObjectKey(folder.getRoot().toPath().resolve("backupFile"));
        backupChecksumObjectKey = S3Client.toObjectKey(folder.getRoot().toPath().resolve("backupFile.md5"));
        backupFileWriter = new BackupFileWriter(folder.getRoot().toPath(), "backupFile", s3Config);
    }

    @After
    public void teardown() throws Exception {
        backupFileWriter.build();
    }

    @Test
    public void addChunk_sendBytes_bytesAreWritten() throws Exception {
        backupFileWriter.addChunk("ABC".getBytes());
        backupFileWriter.addChunk("\nqwe".getBytes());
        backupFileWriter.build();
        final Path filePath = s3MultipartClient.downloadObject(backupObjectKey, folder.getRoot().toPath());
        assertEquals(Arrays.asList("ABC", "qwe"), Files.readAllLines(filePath));
    }

    @Test
    public void addChunk_sendByteAndMatchingChecksum_verifyChecksumMatchesContent() throws Exception {
        backupFileWriter.addChunk("ABCtre".getBytes());
        backupFileWriter.build();
        final Path filePath = s3MultipartClient.downloadObject(backupObjectKey, folder.getRoot().toPath());
        assertEquals(Arrays.asList("ABCtre"), Files.readAllLines(filePath));

        backupFileWriter.validateChecksum(getChecksum());
    }

    @Test(expected = BackupServiceException.class)
    public void addChunk_sendByteAndMismatchingChecksum_throwException() throws Exception {
        backupFileWriter.addChunk("ABC".getBytes());
        backupFileWriter.build();
        final Path filePath = s3MultipartClient.downloadObject(backupObjectKey, folder.getRoot().toPath());
        assertEquals(Arrays.asList("ABC"), Files.readAllLines(filePath));

        backupFileWriter.validateChecksum(getChecksum());
    }

    @Test
    public void writeChecksumFile_checksumFileIsWritten() throws Exception {
        backupFileWriter.addChunk("ABCtre".getBytes());
        backupFileWriter.writeChecksumFile();
        backupFileWriter.build();

        Path filePath = s3MultipartClient.downloadObject(backupObjectKey, folder.getRoot().toPath());
        assertEquals(Arrays.asList("ABCtre"), Files.readAllLines(filePath));

        filePath = s3MultipartClient.downloadObject(backupChecksumObjectKey, folder.getRoot().toPath());
        assertEquals(Arrays.asList(getChecksum()), Files.readAllLines(filePath));
    }

    private String getChecksum() {
        final ChecksumCalculator checksumCalculator = new ChecksumCalculator();
        checksumCalculator.addBytes("ABCtre".getBytes());
        return checksumCalculator.getChecksum();
    }

}