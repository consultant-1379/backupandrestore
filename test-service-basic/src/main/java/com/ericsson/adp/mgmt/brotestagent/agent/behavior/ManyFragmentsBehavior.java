/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.brotestagent.agent.behavior;

import com.ericsson.adp.mgmt.bro.api.fragment.BackupFragmentInformation;
import com.ericsson.adp.mgmt.brotestagent.exception.FailedToCreateBackupException;
import com.ericsson.adp.mgmt.brotestagent.util.PropertiesHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * This class holds custom agent behavior for BRO testing purposes.
 * It is used to simulate an agent which sends a large number of fragments during a backup.
 */
public class ManyFragmentsBehavior extends TestAgentBehavior {
    private static final Logger log = LogManager.getLogger(ManyFragmentsBehavior.class);

    private static final String BACKUP_FRAGMENT_COUNT = "many.fragment.agent.fragment.count";
    private static final String DEFAULT_FRAGMENT_COUNT = "1000";

    private static final Path ROOT_LOCATION = Paths.get(System.getProperty("java.io.tmpdir"), "many-backup-files");

    private static final int FRAGMENT_FILE_SIZE_BYTES = 32;

    private final Random randomNumberGenerator = new Random();

    @Override
    protected List<BackupFragmentInformation> doSomethingToCreateBackup(final String backupType) {
        if (isBackupEmpty()) {
            return new ArrayList<>();
        }

        try {
            return createFiles().map(this::createFragment).collect(Collectors.toList());
        } catch (final Exception e) {
            throw new FailedToCreateBackupException("Failed to generate files", e);
        }
    }

    private BackupFragmentInformation createFragment(final Path fragmentLocation) {
        final BackupFragmentInformation backupFragmentInformation = new BackupFragmentInformation();
        backupFragmentInformation.setFragmentId(UUID.randomUUID().toString());
        backupFragmentInformation.setVersion("aaaaaaaaaas");
        backupFragmentInformation.setSizeInBytes(String.valueOf((long) FRAGMENT_FILE_SIZE_BYTES));
        backupFragmentInformation.setBackupFilePath(fragmentLocation.toString());

        return backupFragmentInformation;
    }

    private Stream<Path> createFiles() throws IOException {
        Files.createDirectories(ROOT_LOCATION);
        for (int i = 0; i < getFragmentCount(); i++) {
            writeFile(ROOT_LOCATION.resolve(i + ".data"), FRAGMENT_FILE_SIZE_BYTES);
        }

        return IntStream.range(0, getFragmentCount()).mapToObj(i -> ROOT_LOCATION.resolve(i + ".data"));
    }

    private void writeFile(final Path path, final int size) throws IOException {
        final Instant startInstant = Instant.now();
        try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            final String oneMegabyteLongString = generateFileContent();
            for (int i = 0; i < size; i++) {
                outputStream.write(oneMegabyteLongString.getBytes());
                outputStream.flush();
            }
        }
        log.info("Time spent creating {}mb file: {}s", size, ChronoUnit.SECONDS.between(startInstant, Instant.now()));
    }

    private boolean isBackupEmpty() {
        return getFragmentCount() == 0;
    }

    private int getFragmentCount() {
        return Integer.parseInt(getProperty(BACKUP_FRAGMENT_COUNT, DEFAULT_FRAGMENT_COUNT));
    }

    private String getProperty(final String property, final String defaultValue) {
        return PropertiesHelper.getProperty(property, defaultValue);
    }

    private String generateFileContent() {
        final StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < FRAGMENT_FILE_SIZE_BYTES; i++) {
            stringBuilder.append(getRandomChar());
        }

        return stringBuilder.toString();
    }

    private char getRandomChar() {
        return (char) ('a' + randomNumberGenerator.nextInt(26));
    }
}
