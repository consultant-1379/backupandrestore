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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3MultipartClient;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3Client;
import com.ericsson.adp.mgmt.backupandrestore.persist.ProcessChunksUtil;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ericsson.adp.mgmt.backupandrestore.exception.RestoreDownloadException;
import com.ericsson.adp.mgmt.bro.api.filetransfer.FileChunkServiceUtil;
import com.ericsson.adp.mgmt.bro.api.util.ChecksumCalculator;
import com.ericsson.adp.mgmt.data.CustomMetadataFileChunk;
import com.ericsson.adp.mgmt.data.DataMessageType;
import com.ericsson.adp.mgmt.data.RestoreData;
import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;

/**
 * sends custom metadata and checksum chunks to agent.
 */
public class RestoreCustomMetadataFile {

    private static final Logger log = LogManager.getLogger(RestoreCustomMetadataFile.class);
    private final StreamObserver<RestoreData> responseObserver;
    private final S3Config s3Config;
    private final S3MultipartClient s3MultipartClient;
    private final  ChecksumValidator checksumValidator;

    /**
     * @param responseObserver
     *            - Stream Observer.
     * @param s3Config
     *            - the configuration of OSMN
     */
    public RestoreCustomMetadataFile(final StreamObserver<RestoreData> responseObserver,  final S3Config s3Config) {
        this.responseObserver = responseObserver;
        this.s3Config = s3Config;
        if (s3Config.isEnabled()) {
            s3MultipartClient = new S3MultipartClient(s3Config);
        } else {
            s3MultipartClient = null;
        }
        checksumValidator = new ChecksumValidator(s3Config, s3MultipartClient);
    }

    /**
     * sends custom metadata and checksum chunks.
     * @param customMetadataPath the file to be sent
     */
    public void sendCustomMetadataFile(final Path customMetadataPath) {
        log.debug("Transferring data for: {}", customMetadataPath);
        if (s3Config.isEnabled()) {
            sendCustomMetadataChunksFromOSMN(S3Client.toObjectKey(customMetadataPath));
        } else {
            sendCustomMetadataChunks(customMetadataPath);
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    private void sendCustomMetadataChunksFromOSMN(final String objectKey) {
        final ChecksumCalculator calculator = new ChecksumCalculator();
        sendFileName(new File(objectKey).getName());
        try {
            final InputStream inputStream = s3MultipartClient.downloadObject(s3Config.getDefaultBucketName(), objectKey);
            ProcessChunksUtil.processStreamChunks(inputStream, (chunk, bytesReadInChunk) -> {
                sendChunkCustomData(ByteString.copyFrom(chunk, 0, bytesReadInChunk));
                calculator.addBytes(chunk, 0, bytesReadInChunk);
            });
        } catch (final Exception e) {
            throw new RestoreDownloadException("Error sending restore custom metadata file <" + objectKey + ">", e);
        }

        final String calculatedChecksum = calculator.getChecksum();

        log.debug("Validating stored checksum for: {}", objectKey);
        final String checksumObjectKey = objectKey + ".md5" ;
        checksumValidator.validateFromOSMN(calculatedChecksum, checksumObjectKey);

        sendCustomMetadataChecksum(calculatedChecksum, objectKey);
    }


    private void sendCustomMetadataChunks(final Path customMetadata) {
        final ChecksumCalculator calculator = new ChecksumCalculator();
        sendFileName(customMetadata.getFileName().toString());
        try {
            FileChunkServiceUtil.processFileChunks(customMetadata.toString(), (chunk, bytesReadInChunk) -> {
                sendChunkCustomData(ByteString.copyFrom(chunk, 0, bytesReadInChunk));
                calculator.addBytes(chunk, 0, bytesReadInChunk);
            });
        } catch (final IOException e) {
            throw new RestoreDownloadException("Error sending restore custom metadata file <" + customMetadata + ">", e);
        }

        final String calculatedChecksum = calculator.getChecksum();

        log.debug("Validating stored checksum for: {}", customMetadata);
        final Path checksumPath = Paths.get(customMetadata.toString() + ".md5");
        checksumValidator.validate(calculatedChecksum, checksumPath);

        sendCustomMetadataChecksum(calculatedChecksum, customMetadata.toString());
    }

    private void sendCustomMetadataChecksum(final String checksum, final String path) {
        log.debug("Sending the checksum: {}, for: {}", checksum, path);
        final CustomMetadataFileChunk customMetadataFileChunk = CustomMetadataFileChunk.newBuilder().setChecksum(checksum).build();
        final RestoreData message = RestoreData.newBuilder().setCustomMetadataFileChunk(customMetadataFileChunk)
                .setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE).build();
        responseObserver.onNext(message);
    }

    private void sendChunkCustomData(final ByteString chunkToSend) {
        responseObserver.onNext(createMessageForCustomMetadata(chunkToSend));
    }

    private RestoreData createMessageForCustomMetadata(final ByteString chunkToSend) {
        final CustomMetadataFileChunk backupFileChunk = CustomMetadataFileChunk.newBuilder().setContent(chunkToSend).build();
        return RestoreData.newBuilder().setCustomMetadataFileChunk(backupFileChunk).setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE).build();
    }

    private void sendFileName(final String customMetadataFile) {
        log.debug("Sending the file name: {}", customMetadataFile);
        final CustomMetadataFileChunk backupFileChunk = CustomMetadataFileChunk.newBuilder().setFileName(customMetadataFile).build();
        final RestoreData message = RestoreData.newBuilder().setCustomMetadataFileChunk(backupFileChunk)
                .setDataMessageType(DataMessageType.CUSTOM_METADATA_FILE).build();
        responseObserver.onNext(message);
    }

}
