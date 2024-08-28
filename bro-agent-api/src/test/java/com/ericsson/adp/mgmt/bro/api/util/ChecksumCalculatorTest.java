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
package com.ericsson.adp.mgmt.bro.api.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.bro.api.filetransfer.FileChunkServiceUtil;
import com.google.protobuf.ByteString;

public class ChecksumCalculatorTest {

    private static final Path FILE_PATH = Paths.get("./src/test/resources/backup.txt");
    private static final String NEW_CONTENT = "Modified Content of the file by adding this text";
    private static final List<String> EXPECTED_CHECKSUMS = Arrays.asList("849BA9527370C9AC7EEE862D20E18BFE","CF5E42FC131271EC1D8236E46B837030");
    private static final List<String> EXPECTED_CHECKSUMS_FOR_MODIFIED_FILE = Arrays.asList("27CF54454A9525FCBF5F12A1AD7779EC","350C27CD6C5BF0E49617A176578FFB8C");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path temporaryFile;
    private ChecksumCalculator calculator;

    @Before
    public void setUp() throws Exception {
        calculator = new ChecksumCalculator();
        temporaryFile = createTemporaryFile();
    }

    @Test
    public void checksumCalculator_originalFile_calculateChecksum() throws Exception {
        Files.copy(FILE_PATH, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
        final String actualChecksum = calculator.calculateChecksum(temporaryFile.toString());

        assertTrue(EXPECTED_CHECKSUMS.contains(actualChecksum));
    }

    @Test
    public void checksumCalculator_modifiedFile_calculateChecksum() throws Exception {
        Files.copy(FILE_PATH, temporaryFile,StandardCopyOption.REPLACE_EXISTING);
        Files.write(temporaryFile, NEW_CONTENT.getBytes(), StandardOpenOption.APPEND);
        final String actualChecksum = calculator.calculateChecksum(temporaryFile.toString());

        assertTrue(EXPECTED_CHECKSUMS_FOR_MODIFIED_FILE.contains(actualChecksum));
    }

    @Test
    public void checksumCalculator_originalFileChunks_calculateChecksumAtRestore() throws Exception {
        FileChunkServiceUtil.processFileChunks(FILE_PATH.toString(), (chunk, numberOfBytesRead) -> {
            calculator.addBytes(ByteString.copyFrom(chunk, 0, numberOfBytesRead).toByteArray());
        });

        assertTrue(EXPECTED_CHECKSUMS.contains(calculator.getChecksum()));
    }

    @Test
    public void checksumCalculator_originalFileChunks_calculateChecksumAtBackup() throws Exception {
        FileChunkServiceUtil.processFileChunks(FILE_PATH.toString(), (chunk, numberOfBytesRead) -> {
            calculator.addBytes(chunk, 0, numberOfBytesRead);
        });

        assertTrue(EXPECTED_CHECKSUMS.contains(calculator.getChecksum()));
    }

    private Path createTemporaryFile() throws IOException {
        return Files.createTempFile(this.folder.getRoot().toPath(), "modified", "tmp");
    }
}
