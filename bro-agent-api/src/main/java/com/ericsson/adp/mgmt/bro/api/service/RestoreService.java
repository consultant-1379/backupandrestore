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
package com.ericsson.adp.mgmt.bro.api.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.bro.api.exception.FailedToDownloadException;
import com.ericsson.adp.mgmt.bro.api.util.ChecksumCalculator;
import com.ericsson.adp.mgmt.data.BackupFileChunk;
import com.ericsson.adp.mgmt.data.CustomMetadataFileChunk;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.RestoreData;
import com.google.protobuf.ByteString;

/**
 * Stores fragment data sent in restore data stream
 */
public class RestoreService {

    private static final Logger log = LogManager.getLogger(RestoreService.class);

    private final ChecksumCalculator calculator;
    private final Path restoreLocation;
    private OutputStream backupFileStream;
    private OutputStream customMetadataFileStream;
    private String fileName;

    /**
     * Stores fragment data sent in restore data stream
     *
     * @param restoreLocation
     *            the directory to download fragments to
     * @throws FailedToDownloadException
     *             exception
     */
    public RestoreService(final String restoreLocation) throws FailedToDownloadException {
        this.calculator = new ChecksumCalculator();
        this.restoreLocation = Paths.get(restoreLocation);
    }

    /**
     * Download files comprising each fragment
     *
     * @param restoreDataIterator
     *            Iterator for RestoreData list
     * @throws FailedToDownloadException
     *             exception
     */
    public void download(final Iterator<RestoreData> restoreDataIterator) throws FailedToDownloadException {
        while (iteratorHasNext(restoreDataIterator)) {
            final RestoreData chunk = restoreDataIterator.next();
            if (DataMessageType.BACKUP_FILE.equals(chunk.getDataMessageType())) {
                writeBackupFile(chunk.getBackupFileChunk());
            }
            if (DataMessageType.CUSTOM_METADATA_FILE.equals(chunk.getDataMessageType())) {
                writeCustomMetadataFile(chunk.getCustomMetadataFileChunk());
            }
        }
    }

    /**
     * Throws exception if onError message is received.
     *
     * @param restoreDataIterator
     *            iterates over RestoreData messages
     * @return true if iterator has next
     * @throws FailedToDownloadException
     */
    private boolean iteratorHasNext(final Iterator<RestoreData> restoreDataIterator) throws FailedToDownloadException {
        try {
            return restoreDataIterator.hasNext();
        } catch (final Exception e) {
            closeFile(backupFileStream);
            closeFile(customMetadataFileStream);
            throw new FailedToDownloadException("Error at Orchestrator while downloading data during restore");
        }
    }

    private void writeBackupFile(final BackupFileChunk chunk) throws FailedToDownloadException {
        if (isFileName(chunk.getChecksum(), chunk.getContent())) {
            this.backupFileStream = getFileStream(chunk.getFileName());
            this.fileName = chunk.getFileName();
        } else if (isFileContent(chunk.getChecksum())) {
            writeFileChunk(chunk.getContent().toByteArray(), this.backupFileStream);
        } else {
            closeFile(this.backupFileStream);
            validateChecksum(chunk.getChecksum());
        }
    }

    private void writeCustomMetadataFile(final CustomMetadataFileChunk chunk) throws FailedToDownloadException {
        if (isFileName(chunk.getChecksum(), chunk.getContent())) {
            setCustomMetadataFileStream(chunk.getFileName());
            this.fileName = chunk.getFileName();
        } else if (isFileContent(chunk.getChecksum())) {
            writeFileChunk(chunk.getContent().toByteArray(), this.customMetadataFileStream);
        } else {
            closeFile(this.customMetadataFileStream);
            validateChecksum(chunk.getChecksum());
        }
    }

    private boolean isFileName(final String checksum, final ByteString content) {
        return (checksum == null || checksum.isEmpty()) && (content == null || content.isEmpty());

    }

    private boolean isFileContent(final String checksum) {
        return (checksum == null) || checksum.isEmpty();
    }

    private void validateChecksum(final String checksum) throws FailedToDownloadException {
        final String calculatedChecksum = this.calculator.getChecksum();
        if (calculatedChecksum.equals(checksum)) {
            log.debug("Checksum Matches for {}", fileName);
        } else {
            throw new FailedToDownloadException("The checksum for the file:" + fileName + " did not match the received value from the orchestrator");
        }

        this.fileName = "";
    }

    private void writeFileChunk(final byte[] byteArray, final OutputStream fileStream) throws FailedToDownloadException {
        try {
            fileStream.write(byteArray);
            this.calculator.addBytes(byteArray);
        } catch (final Exception e) {
            closeFile(fileStream);
            throw new FailedToDownloadException("Error while downloading fragment", e);
        }
    }

    private OutputStream getFileStream(final String fileName) throws FailedToDownloadException {
        try {
            Files.createDirectories(restoreLocation);
            return Files.newOutputStream(restoreLocation.resolve(fileName), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (final IOException e) {
            throw new FailedToDownloadException("Error while creating file stream", e);
        }
    }

    private void closeFile(final OutputStream fileStream) throws FailedToDownloadException {
        try {
            if (fileStream != null) {
                fileStream.close();
            }
        } catch (final IOException e) {
            throw new FailedToDownloadException("Exception while closing file:", e);
        }
    }

    private void setCustomMetadataFileStream(final String fileName) throws FailedToDownloadException {
        this.customMetadataFileStream = getFileStream(fileName);
    }
}
