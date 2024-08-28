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

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3MultipartClient;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3Client;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;
import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Writes backup data to file
 * upload them to OSMN server when config is given
 */
public class BackupFileWriter {

    private static final Logger log = LogManager.getLogger(BackupFileWriter.class);

    private final ChecksumCalculator calculator;
    private final OutputStream fileWriter;
    private final OutputStream checksumFileWriter;
    private String calculatedChecksum;

    /**
     * Write the backup file to PVC or OSMN
     * @param folder the folder where the file is
     * @param fileName the name of the file
     * @param s3Config the configuration of OSMN
     */
    public BackupFileWriter(final Path folder, final String fileName, final S3Config s3Config) {
        this(folder, fileName, s3Config, 0);
    }

    /**
     * Write the backup file to PVC or OSMN
     * @param folder the folder where the file is
     * @param fileName the name of the file
     * @param s3Config the configuration of OSMN
     * @param fileSize the size of the file to be uploaded
     */
    public BackupFileWriter(final Path folder, final String fileName, final S3Config s3Config, final long fileSize) {
        this.calculator = new ChecksumCalculator();
        if (s3Config.isEnabled()) {
            final S3MultipartClient s3MultipartClient = new S3MultipartClient(s3Config);
            this.fileWriter = s3MultipartClient.getOutputStream(S3Client.toObjectKey(folder.resolve(fileName)), fileSize);
            this.checksumFileWriter = s3MultipartClient.getOutputStream(S3Client.toObjectKey(folder.resolve(fileName + "." +
                calculator.getChecksumAlgorithm().toLowerCase())), 1024);
        } else {
            this.fileWriter = createFileWriter(folder, fileName);
            this.checksumFileWriter = createFileWriter(folder, fileName + "." + calculator.getChecksumAlgorithm().toLowerCase());
        }
    }

    /**
     * Writes BackupFileChunk to file
     * @param chunk BackupFileChunk to be stored
     */
    public void addChunk(final byte[] chunk) {
        try {
            fileWriter.write(chunk);
            calculator.addBytes(chunk);
        } catch (final Exception e) {
            throw new BackupServiceException("Exception while saving backup file:", e);
        }
    }

    /**
     * validates if checksum matches
     * @param checksum sent by agent
     */
    public void validateChecksum(final String checksum) {
        if (getCalculatedChecksum().equals(checksum)) {
            log.debug("BRO calculated and Agent sent checksums match");
        } else {
            throw new BackupServiceException("BRO calculated and Agent sent checksums mismatch");
        }
    }

    private String getCalculatedChecksum() {
        if (this.calculatedChecksum == null) {
            this.calculatedChecksum = calculator.getChecksum();
        }
        return this.calculatedChecksum;
    }

    /**
     * Finishes building a backup.
     */
    public void build() {
        closeFile();
    }

    /**
     * Writes the checksum file.
     */
    public void writeChecksumFile() {
        try {
            log.debug("Saving backup checksum");
            checksumFileWriter.write(this.getCalculatedChecksum().getBytes());
        } catch (final IOException e) {
            throw new BackupServiceException("Exception while saving backup checksum file:", e);
        } finally {
            try {
                checksumFileWriter.close();
            } catch (final IOException e) {
                log.error("Exception while closing file:", e);
            }
        }
    }

    private OutputStream createFileWriter(final Path folder, final String fileName) {
        try {
            Files.createDirectories(folder);
            return Files
                .newOutputStream(folder.resolve(fileName), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (final IOException e) {
            throw new BackupServiceException("Exception while creating output stream:", e);
        }
    }

    private void closeFile() {
        try {
            fileWriter.close();
        } catch (final IOException e) {
            throw new BackupServiceException("Exception while closing file:", e);
        }
    }
}
