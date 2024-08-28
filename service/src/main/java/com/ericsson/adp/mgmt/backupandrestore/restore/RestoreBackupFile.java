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
package com.ericsson.adp.mgmt.backupandrestore.restore;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3MultipartClient;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3Client;
import com.ericsson.adp.mgmt.backupandrestore.persist.ProcessChunksUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.exception.RestoreDownloadException;
import com.ericsson.adp.mgmt.bro.api.filetransfer.FileChunkServiceUtil;
import com.ericsson.adp.mgmt.bro.api.util.ChecksumCalculator;
import com.ericsson.adp.mgmt.data.BackupFileChunk;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.RestoreData;
import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;

/**
 * sends backup file & checksum chunks to the agent.
 */
public class RestoreBackupFile {

    private static final String CHECKSUM_ALGORITHM_EXTENSION = ".md5";
    private static final Logger log = LogManager.getLogger(RestoreBackupFile.class);
    private final StreamObserver<RestoreData> responseObserver;
    private final int fragmentChunkSize;
    private final S3Config s3Config;
    private final S3MultipartClient s3MultipartClient;
    private final  ChecksumValidator checksumValidator;

    /**
     * @param responseObserver
     *            - Stream Observer.
     * @param fragmentChunkSize
     *            - Maximum fragment chunk size
     * @param s3Config
     *            - the configuration of OSMN
     */
    public RestoreBackupFile(final StreamObserver<RestoreData> responseObserver, final int fragmentChunkSize, final S3Config s3Config) {
        this.responseObserver = responseObserver;
        this.fragmentChunkSize = fragmentChunkSize;
        this.s3Config = s3Config;
        if (s3Config.isEnabled()) {
            s3MultipartClient = new S3MultipartClient(s3Config);
        } else {
            s3MultipartClient = null;
        }
        checksumValidator = new ChecksumValidator(s3Config, s3MultipartClient);
    }

    /**
     * sends backup and checksum chunks
     *
     * @param file
     *            - location of the backup data file.
     * @return the number of bytes transferred
     */
    public long sendFile(final Path file) {
        log.debug("Transferring data for: {}", file);
        if (s3Config.isEnabled()) {
            return sendBackupChunksFromOSNM(S3Client.toObjectKey(file));
        } else {
            return sendBackupChunks(file);
        }
    }

    /**
     * Send the object from OSMN to a series of chunks.
     * @param objectKey the ObjectKey
     * @return the number of bytes sent
     */
    @SuppressWarnings("PMD.CloseResource")
    private long sendBackupChunksFromOSNM(final String objectKey) {
        final ChecksumCalculator calculator = new ChecksumCalculator();
        sendFileName(new File(objectKey).getName());
        long sent = 0;
        try {
            final InputStream inputStream = s3MultipartClient.downloadObject(s3Config.getDefaultBucketName(), objectKey);
            sent = ProcessChunksUtil.processStreamChunks(inputStream, (chunk, bytesReadInChunk) -> {
                sendChunk(ByteString.copyFrom(chunk, 0, bytesReadInChunk));
                calculator.addBytes(chunk, 0, bytesReadInChunk);
            }, fragmentChunkSize);
        } catch (final Exception e) {
            throw new RestoreDownloadException("Error sending restore objectKey <" + objectKey + ">", e);
        }

        final String calculatedChecksum = calculator.getChecksum();

        log.debug("Validating stored checksum for: {}", objectKey);
        final String checksumObjectKey = objectKey + CHECKSUM_ALGORITHM_EXTENSION;
        checksumValidator.validateFromOSMN(calculatedChecksum, checksumObjectKey);

        sendChecksum(calculatedChecksum, objectKey);
        return sent;
    }

    private long sendBackupChunks(final Path file) {
        final ChecksumCalculator calculator = new ChecksumCalculator();
        final AtomicLong transferredBytes = new AtomicLong(0);
        sendFileName(file.getFileName().toString());
        try {
            FileChunkServiceUtil.processFileChunks(file.toString(), (chunk, bytesReadInChunk) -> {
                sendChunk(ByteString.copyFrom(chunk, 0, bytesReadInChunk));
                transferredBytes.addAndGet(bytesReadInChunk);
                calculator.addBytes(chunk, 0, bytesReadInChunk);
            }, fragmentChunkSize);
        } catch (final Exception e) {
            throw new RestoreDownloadException("Error sending restore file <" + file + ">", e);
        }

        final String calculatedChecksum = calculator.getChecksum();

        log.debug("Validating stored checksum for: {}", file);
        final Path checksumPath = Paths.get(file.toString() + CHECKSUM_ALGORITHM_EXTENSION);
        checksumValidator.validate(calculatedChecksum, checksumPath);

        sendChecksum(calculatedChecksum, file.toString());
        return transferredBytes.get();
    }

    private void sendChecksum(final String checksum, final String path) {
        log.debug("Sending the checksum: {}, for: {}", checksum, path);
        final BackupFileChunk backupFileChunk = BackupFileChunk.newBuilder().setChecksum(checksum).build();
        final RestoreData restoreDataMessage = RestoreData.newBuilder().setBackupFileChunk(backupFileChunk)
                .setDataMessageType(DataMessageType.BACKUP_FILE).build();
        responseObserver.onNext(restoreDataMessage);
    }

    private void sendChunk(final ByteString chunkToSend) {
        responseObserver.onNext(createMessageForRestoreData(chunkToSend));
    }

    private RestoreData createMessageForRestoreData(final ByteString chunkToSend) {
        final BackupFileChunk backupFileChunk = BackupFileChunk.newBuilder().setContent(chunkToSend).build();
        return RestoreData.newBuilder().setBackupFileChunk(backupFileChunk).setDataMessageType(DataMessageType.BACKUP_FILE).build();
    }

    private void sendFileName(final String name) {
        log.debug("Sending the file name: {}", name);
        final BackupFileChunk backupFileChunk = BackupFileChunk.newBuilder().setFileName(name).build();
        final RestoreData restoreDataMessage = RestoreData.newBuilder().setBackupFileChunk(backupFileChunk)
                .setDataMessageType(DataMessageType.BACKUP_FILE).build();
        responseObserver.onNext(restoreDataMessage);
    }

}
