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
package com.ericsson.adp.mgmt.backupandrestore.aws.service;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.S3_CREATION_TIME;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.S3_DATE_FORMAT;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.unit.DataSize;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.ericsson.adp.mgmt.backupandrestore.aws.BufferedS3OutputStream;
import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.exception.AWSException;

/**
 * AWS client to handle multipart upload and download
 */
public class S3MultipartClient implements S3Client {
    public static final int PART_SIZE_LOWER_BOUND = (int) DataSize.ofMegabytes(5).toBytes();

    private static final Logger log = LogManager.getLogger(S3MultipartClient.class);

    private static final int MAX_PART_NUMBER = 10 * 1000;
    private static final long MAX_FILE_SIZE = DataSize.ofTerabytes(5).toBytes();

    private final S3Config s3Config;

    /**
     * Construct the client based on the AWS config and the file size
     * @param s3Config the AWS configuration class
     */
    public S3MultipartClient(final S3Config s3Config) {
        this.s3Config = s3Config;
        createBucket(); // Construct the default bucket, if it isn't already present
    }

    @Override
    public S3Config getS3Config() {
        return s3Config;
    }

    /**
     * Upload an object to the OSMN server
     * @param bucketName the name of a bucket
     * @param objectKey  the key of an S3 object. S3Object uses the term.
     * @param file       the file to upload
     */
    @Override
    public void uploadObject(final String bucketName, final String objectKey, final File file) throws InterruptedException {
        createBucket(bucketName);
        final TransferManager manager = TransferManagerBuilder.standard()
                .withS3Client(getS3Client())
                .build();
        final Upload upload = manager.upload(bucketName, objectKey, file);
        log.debug("Object multi-upload {} started", objectKey);
        upload.waitForCompletion();
        log.debug("Object multi-upload {} completed", objectKey);
    }

    /**
     * Upload an object to the OSMN server
     * @param bucketName bucket where the file should be saved
     * @param objectKey the key of object
     * @param stream input stream is the content of object
     * @throws InterruptedException the exception interrupts the upload
     * @throws IOException the exception breaks the stream
     */
    @Override
    public void uploadObject(final String bucketName, final String objectKey, final InputStream stream) throws InterruptedException, IOException {
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(stream.available());
        uploadObject(bucketName, objectKey, stream, metadata);
    }

    /**
     * Upload an object to the OSMN server
     * @param bucketName bucket where the file should be saved
     * @param objectKey  the key of object
     * @param stream     input stream is the content of object
     * @param metadata   metadata of the content
     */
    @Override
    public void uploadObject(final String bucketName, final String objectKey,
                             final InputStream stream, final ObjectMetadata metadata) throws InterruptedException {
        createBucket(bucketName);
        // Set the custom creation time metadata
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(S3_DATE_FORMAT)
                .withZone(ZoneId.systemDefault());
        final String creationTime = dtf.format(Instant.now());

        final TransferManager manager = TransferManagerBuilder.standard()
                .withS3Client(getS3Client())
                .build();
        metadata.addUserMetadata(S3_CREATION_TIME, creationTime);
        final Upload upload = manager.upload(bucketName, objectKey, stream, metadata);
        log.debug("Object multi-upload {} started", objectKey);
        upload.waitForCompletion();
        log.debug("Object multi-upload {} completed", objectKey);
    }

    /**
     * Upload an object to the OSMN server
     *
     * @param bucketName Bucket Name
     * @param objectKey  Object name to be saved
     * @param bytes      Byte array input stream
     */
    @Override
    public void uploadObject(final String bucketName, final String objectKey, final byte[] bytes) throws InterruptedException {
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        uploadObject(bucketName, objectKey, new ByteArrayInputStream(bytes), metadata);
    }

    /**
     * Download an object from a bucket
     *
     * @param bucketName bucket Name
     * @param objectKey  Object full path from S3
     * @param fileFolder   the destination folder
     * @return the Path of the file created
     * @throws InterruptedException an exception interrupts download
     */
    @Override
    public Path downloadObject(final String bucketName, final String objectKey, final Path fileFolder) throws InterruptedException {
        final Path pathToFile = fileFolder.resolve(objectKey);
        final TransferManager manager = TransferManagerBuilder.standard()
                .withS3Client(getS3Client())
                .build();
        final Download download = manager.download(bucketName, objectKey, pathToFile.toFile());
        log.debug("Object multi-part {} download started", objectKey);
        download.waitForCompletion();
        log.debug("Object multi-part {} download completed", objectKey);
        return pathToFile;
    }

    /**
     * Construct an output steam to upload data to the object store
     * @param objectKey - the key to store the data at
     * @throws IOException - if construction of the output stream fails
     * @return an output stream, whose output destination in an object of key objectKey in the object store
     * */
    public OutputStream getOutputStream(final String objectKey) throws IOException {
        return getOutputStream(objectKey, 0);
    }

    /**
     * Construct an output steam to upload data to object store
     * @param objectKey - the key to store the data at
     * @param fileSize - the size of the expected upload (used to configure the size of chunks sent to the object store)
     * @return an output stream, whose output destination in an object of key objectKey in object store
     * */
    public OutputStream getOutputStream(final String objectKey, final long fileSize) {
        return new BufferedS3OutputStream(this, objectKey, getChunkSize(fileSize));
    }

    /**
     * Download an object into an InputStream
     * @param bucketName the name of the bucket
     * @param objectKey the object key
     * @return the inputStream
     */
    public InputStream downloadObject(final String bucketName, final String objectKey) {
        return new SequenceInputStream(getEnumeration(bucketName, objectKey));
    }

    /**
     * Calculate chunk size to be sent to S3 endpoint for a given filesize
     * @param fileSize - the file size of the file to be uploaded to s3
     * @return the chunk size in bytes
     * */
    public static int getChunkSize(final long fileSize) {
        if (fileSize > MAX_FILE_SIZE) {
            throw new AWSException("The file size is great than 5T, which can't be stored in OSMN");
        }
        final long partSizeNeeded = fileSize / MAX_PART_NUMBER;
        if (partSizeNeeded < PART_SIZE_LOWER_BOUND) {
            // minPartSize is set to 5MB
            return PART_SIZE_LOWER_BOUND;
        } else if (partSizeNeeded < 10 * PART_SIZE_LOWER_BOUND) {
            // minPartSize is set to 50MB
            return PART_SIZE_LOWER_BOUND * 10;
        } else {
            // minPartSize is set to 500MB
            return PART_SIZE_LOWER_BOUND * 100;
        }
    }

    /**
     * Invalidate Cached clients
     */
    public void invalidateCachedClient() {
        this.getS3Config().invalidateCachedClient();
    }

    private Enumeration<InputStream> getEnumeration(final String bucketName, final String objectKey) {
        final long totalSize = getObjectSize(bucketName, objectKey);
        return new Enumeration<>() {
            private long currentPosition;

            @Override
            public boolean hasMoreElements() {
                return currentPosition < totalSize;
            }

            @Override
            public InputStream nextElement() {
                final GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, objectKey)
                        .withRange(currentPosition, currentPosition + PART_SIZE_LOWER_BOUND - 1);
                currentPosition += PART_SIZE_LOWER_BOUND;
                return getS3Client().getObject(getObjectRequest).getObjectContent();
            }
        };
    }
}