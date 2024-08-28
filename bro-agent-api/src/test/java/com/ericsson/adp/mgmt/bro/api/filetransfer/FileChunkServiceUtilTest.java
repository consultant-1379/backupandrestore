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
package com.ericsson.adp.mgmt.bro.api.filetransfer;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.ByteString;

public class FileChunkServiceUtilTest {

    private static final Path ROOT_LOCATION = Paths.get(System.getProperty("java.io.tmpdir"), "large-backup-files");
    private static final Path BACKUP_FILE = ROOT_LOCATION.resolve("backup.txt");
    private static final int NUMBER_OF_BYTES_IN_MEGABYTE = 1024 * 1024;
    private static final int SIZE_OF_BACKUP_FILE_IN_MB = 1;

    private Random randomNumberGenerator;

    private final String path = BACKUP_FILE.toString();

    private class Counter {
        private int value;

        public int getValue() {
            return value;
        }

        public void increment() {
            this.value++;
        }
    }

    @Before
    public void setUp() throws IOException {
        randomNumberGenerator = new Random();
        createBackupFile();
    }

    @After
    public void cleanUp() throws IOException {
        Files.delete(BACKUP_FILE);
    }

    @Test
    public void processFileChunks_largeFile_readsAllExpectedChunks() throws Exception {
        final Counter counter = new Counter();

        FileChunkServiceUtil.processFileChunks(path, (chunk, numberOfBytesRead) -> {
            counter.increment();
        });

        assertEquals(2, counter.getValue());
    }

    @Test
    public void processFileChunks_largeFile_readsWholeFile() throws Exception {
        final List<ByteString> chunks = new ArrayList<>();

        FileChunkServiceUtil.processFileChunks(path, (chunk, numberOfBytesRead) -> {
            chunks.add(ByteString.copyFrom(chunk, 0, numberOfBytesRead));
        });

        final ByteString fullContent = chunks.stream().reduce(ByteString.EMPTY, (firstChunk, secondChunk) -> firstChunk.concat(secondChunk));

        final String fileContent = new String(fullContent.toByteArray());
        final String expectedContent = new String(Files.readAllBytes(Paths.get(path)));

        assertEquals(expectedContent, fileContent);
    }

    private void createBackupFile() throws IOException {
        Files.createDirectories(ROOT_LOCATION);

        writeFile(BACKUP_FILE, SIZE_OF_BACKUP_FILE_IN_MB);

    }

    private void writeFile(final Path path, final int size) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            final String oneMegabyteLongString = generateOneMegabyteLongString();
            for (int i = 0; i < size; i++) {
                outputStream.write(oneMegabyteLongString.getBytes());
                outputStream.flush();
            }
        }
    }

    private String generateOneMegabyteLongString() {
        final StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < NUMBER_OF_BYTES_IN_MEGABYTE; i++) {
            stringBuilder.append(getRandomChar());
        }

        return stringBuilder.toString();
    }

    private char getRandomChar() {
        return (char) ('a' + randomNumberGenerator.nextInt(26));
    }

}
