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
package com.ericsson.adp.mgmt.brotestagent.agent.behavior;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.bro.api.fragment.BackupFragmentInformation;
import com.ericsson.adp.mgmt.brotestagent.exception.FailedToCreateBackupException;
import com.ericsson.adp.mgmt.brotestagent.util.PropertiesHelper;

/**
 * Represents agent that will create a file with configurable size to be backed up.
 */
public class LargeBackupFileAgentBehavior extends TestAgentBehavior {

    private static final Logger log = LogManager.getLogger(LargeBackupFileAgentBehavior.class);

    private static final String SIZE_OF_BACKUP_FILE = "large.backup.file.agent.backup.size";
    private static final String DEFAULT_SIZE_OF_BACKUP_FILE = "1";

    private static final String SIZE_OF_CUSTOM_METADATA_FILE = "large.backup.file.agent.custom.metadata.size";
    private static final String DEFAULT_SIZE_OF_CUSTOM_METADATA_FILE = "0";

    private static final Path ROOT_LOCATION = Paths.get(System.getProperty("java.io.tmpdir"), "large-backup-files");
    private static final Path BACKUP_FILE = ROOT_LOCATION.resolve("backup.txt");
    private static final Path CUSTOM_METADATA_FILE = ROOT_LOCATION.resolve("customMetadata.txt");

    private static final int NUMBER_OF_BYTES_IN_MEGABYTE = 1024 * 1024;

    private final Random randomNumberGenerator = new Random();

    @Override
    protected List<BackupFragmentInformation> doSomethingToCreateBackup(final String backupType) {
        if (isBackupEmpty()) {
            return new ArrayList<>();
        }

        try {
            createFiles();
            return Arrays.asList(createFragment());
        } catch (final Exception e) {
            throw new FailedToCreateBackupException("Failed to generate large files", e);
        }
    }

    private BackupFragmentInformation createFragment() {
        final BackupFragmentInformation backupFragmentInformation = new BackupFragmentInformation();
        backupFragmentInformation.setFragmentId(UUID.randomUUID().toString());
        backupFragmentInformation.setVersion("aaaaaaaaaas");
        backupFragmentInformation.setSizeInBytes(String.valueOf((long) getBackupSize() * NUMBER_OF_BYTES_IN_MEGABYTE));
        backupFragmentInformation.setBackupFilePath(BACKUP_FILE.toString());

        if (hasCustomMetadata()) {
            backupFragmentInformation.setCustomMetadataFilePath(Optional.of(CUSTOM_METADATA_FILE.toString()));
        }

        return backupFragmentInformation;
    }

    private void createFiles() throws IOException {
        Files.createDirectories(ROOT_LOCATION);

        writeFile(BACKUP_FILE, getBackupSize());

        if (hasCustomMetadata()) {
            writeFile(CUSTOM_METADATA_FILE, getCustomMetadataSize());
        }
    }

    private void writeFile(final Path path, final int size) throws IOException {
        final Instant startInstant = Instant.now();
        try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            final String oneMegabyteLongString = generateOneMegabyteLongString();
            for (int i = 0; i < size; i++) {
                outputStream.write(oneMegabyteLongString.getBytes());
                outputStream.flush();
            }
        }
        log.info("Time spent creating {}mb file: {}s", size, ChronoUnit.SECONDS.between(startInstant, Instant.now()));
    }

    private boolean isBackupEmpty() {
        return getBackupSize() == 0;
    }

    private boolean hasCustomMetadata() {
        return getCustomMetadataSize() != 0;
    }

    private int getBackupSize() {
        return getSize(SIZE_OF_BACKUP_FILE, DEFAULT_SIZE_OF_BACKUP_FILE);
    }

    private int getCustomMetadataSize() {
        return getSize(SIZE_OF_CUSTOM_METADATA_FILE, DEFAULT_SIZE_OF_CUSTOM_METADATA_FILE);
    }

    private int getSize(final String file, final String defaultSize) {
        return Integer.valueOf(PropertiesHelper.getProperty(file, defaultSize));
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
