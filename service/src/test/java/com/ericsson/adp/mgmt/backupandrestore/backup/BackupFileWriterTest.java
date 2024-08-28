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
package com.ericsson.adp.mgmt.backupandrestore.backup;

import static org.junit.Assert.assertEquals;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;
import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumCalculator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class BackupFileWriterTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path backupFile;
    private Path backupChecksumFile;
    private BackupFileWriter backupFileWriter;

    @Before
    public void setUp() {
        backupFile = folder.getRoot().toPath().resolve("backupFile");
        backupChecksumFile = folder.getRoot().toPath().resolve("backupFile.md5");
        backupFileWriter = new BackupFileWriter(folder.getRoot().toPath(), "backupFile", new S3Config());
    }

    @After
    public void teardown() throws Exception {
        backupFileWriter.build();
    }

    @Test
    public void addChunk_sendBytes_bytesAreWritten() throws Exception {
        backupFileWriter.addChunk("ABC".getBytes());
        assertEquals(Arrays.asList("ABC"), Files.readAllLines(backupFile));

        backupFileWriter.addChunk("\nqwe".getBytes());
        assertEquals(Arrays.asList("ABC", "qwe"), Files.readAllLines(backupFile));
    }

    @Test
    public void addChunk_sendByteAndMatchingChecksum_verifyChecksumMatchesContent() throws Exception {
        backupFileWriter.addChunk("ABCtre".getBytes());
        assertEquals(Arrays.asList("ABCtre"), Files.readAllLines(backupFile));

        backupFileWriter.validateChecksum(getChecksum());
    }

    @Test(expected = BackupServiceException.class)
    public void addChunk_sendByteAndMismatchingChecksum_throwException() throws Exception {
        backupFileWriter.addChunk("ABC".getBytes());
        assertEquals(Arrays.asList("ABC"), Files.readAllLines(backupFile));

        backupFileWriter.validateChecksum(getChecksum());
    }

    @Test
    public void addChunk_overwriteExistingFile_firstFileOverwritten() throws Exception {
        final String firstFileContent = "Ericsson";
        final String secondFileContent = "nosscirE";
        final List<String> expectedLinesForFirstFile = Arrays.asList(firstFileContent);
        final List<String> expectedLinesForSecondFile = Arrays.asList(secondFileContent);

        /* Write first file */
        backupFileWriter.addChunk(firstFileContent.getBytes());

        /* Verify first file */
        assertEquals(expectedLinesForFirstFile, Files.readAllLines(backupFile));

        /* Close first file. */
        backupFileWriter.build();

        /* Write second file */
        backupFileWriter = new BackupFileWriter(folder.getRoot().toPath(), "backupFile", new S3Config());
        backupFileWriter.addChunk(secondFileContent.getBytes());

        assertEquals(expectedLinesForSecondFile, Files.readAllLines(backupFile));
    }

    @Test
    public void writeChecksumFile_checksumFileIsWritten() throws Exception {
        backupFileWriter.addChunk("ABCtre".getBytes());
        assertEquals(Arrays.asList("ABCtre"), Files.readAllLines(backupFile));

        backupFileWriter.writeChecksumFile();
        assertEquals(Arrays.asList(getChecksum()), Files.readAllLines(backupChecksumFile));
    }

    private String getChecksum() {
        final ChecksumCalculator checksumCalculator = new ChecksumCalculator();
        checksumCalculator.addBytes("ABCtre".getBytes());
        return checksumCalculator.getChecksum();
    }

}