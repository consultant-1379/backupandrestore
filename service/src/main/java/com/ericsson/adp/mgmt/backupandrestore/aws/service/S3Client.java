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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;

/**
 * Define the basic behavior of aws client expected to handle S3 resources
 */
public interface S3Client {
    Logger LOGGER = LogManager.getLogger(S3Client.class);
    Lock lock = new ReentrantLock(); //NOPMD

    /**
     * Retrieve the AWS Configuration
     * @return the S3 Configuration object
     */
    S3Config getS3Config();

    /**
     * Retrieve the AWS Client
     * @return AmazonS3 AWS Client
     */
    default AmazonS3 getS3Client() {
        return getS3Config().getAWSClient();
    }

    /**
     * Get the default bucketName;
     * @return the name of default bucket
     */
    default String getDefaultBucketName() {
        return getS3Config().getDefaultBucketName();
    }

    /**
     * Create a bucket with default name
     */
    default void createBucket() {
        createBucket(getDefaultBucketName());
    }

    /**
     * Create a bucket
     * @param bucketName Bucket Name
     */
    default void createBucket(final String bucketName) {
        Owner bucketOwner = null;
        final AtomicBoolean created = new AtomicBoolean();
        final AmazonS3 client = getS3Client();
        lock.lock();
        try {
            if (!bucketExist(bucketName)) {
                LOGGER.info("Creating bucket:{}", bucketName);
                final Bucket bucket = client.createBucket(bucketName);
                bucketOwner = bucket.getOwner();
                created.set(true);
            }
        } finally {
            lock.unlock();
        }
        if (created.get()) {
            // NOTE: The owner of the created bucket is expected to be null.
            LOGGER.info("Created bucket:{} owner:{} ", bucketName, bucketOwner);
        }
    }

    /**
     * Validates if the bucket exist or not
     * @param bucketName to be validated
     * @return true if bucketName exist
     */
    private boolean bucketExist(final String bucketName) {
        final List<Bucket> response = getS3Client().listBuckets();
        return response.stream().anyMatch(bucket -> bucket.getName().equals(bucketName));
    }

    /**
     * Get the Bucket object using the bucket name
     * @param bucketName the bucket name
     * @return Optional Bucket Object
     */
    default Optional<Bucket> getBucket(final String bucketName) {
        return getS3Client().listBuckets().stream().filter(b -> b.getName().equals(bucketName)).findFirst();
    }

    /**
     * Removes a Bucket if it's empty
     * @param bucketName Bucket Name to be removed
     */
    default void removeBucket(final String bucketName) {
        emptyBucket(bucketName);
        getS3Client().deleteBucket(bucketName);
    }

    /**
     * Look for bucket location
     * @param bucketName Bucket Name
     * @return String returning the Region
     */
    default String getBucketLocation(final String bucketName) {
        return getS3Client().getBucketLocation(bucketName);
    }

    /**
     * Check if the bucket is empty
     * @param bucketName The name of the bucket
     * @return boolean
     */
    default boolean isBucketEmpty(final String bucketName) {
        final List<String> list = getObjectList(bucketName, "", 1);
        return list.isEmpty();
    }

    /**
     * Empty a Bucket if it isn't
     * @param bucketName Bucket Name to be removed
     */
    default void emptyBucket(final String bucketName) {
        ObjectListing objectListing = getS3Client().listObjects(bucketName);
        List<S3ObjectSummary> objectList = objectListing.getObjectSummaries();
        if (objectList.isEmpty()) {
            return;
        }
        while (true) {
            objectList
                    .forEach(object -> getS3Client().deleteObject(bucketName, object.getKey()));
            if (objectListing.isTruncated()) {
                objectListing = getS3Client().listNextBatchOfObjects(objectListing);
                objectList = objectListing.getObjectSummaries();
            } else {
                break;
            }
        }
    }

    /**
     * Return a list of object keys from a prefix
     * @param bucketName bucket Name to look for
     * @param prefix String representing the path
     * @param maxReturn the max number of the returned item
     * @return ArrayList of object keys
     */
    default List<String> getObjectList(final String bucketName, final String prefix, final int maxReturn) {
        ListObjectsV2Result v2result;
        final List<String> result = new ArrayList<>();
        int count = 0;
        final ListObjectsV2Request req = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefix)
                .withMaxKeys(100);
        do {
            v2result = getS3Client().listObjectsV2(req);
            for (final S3ObjectSummary objectSummary : v2result.getObjectSummaries()) {
                result.add(objectSummary.getKey());
            }
            final String token = v2result.getNextContinuationToken();
            req.setContinuationToken(token);
            count += 100;
        } while (v2result.isTruncated() && count < maxReturn);
        return result;
    }

    /**
     * Return a list of object keys from a prefix, order by creation time
     * @param bucketName bucket Name to look for
     * @param prefix String representing the path
     * @param maxReturn the max number of the returned item
     * @return ArrayList of object keys order by creation time
     */
    default List<String> getObjectListOrder(final String bucketName, final String prefix, final int maxReturn) {
        ListObjectsV2Result v2result;
        final List<S3ObjectInfo> result = new ArrayList<>();

        int count = 0;
        final ListObjectsV2Request req = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefix)
                .withMaxKeys(100);
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(S3_DATE_FORMAT)
                .withZone(ZoneId.systemDefault());
        do {
            v2result = getS3Client().listObjectsV2(req);
            for (final S3ObjectSummary objectSummary : v2result.getObjectSummaries()) {
                Date objectDate;
                final ObjectMetadata metadata = getS3Client()
                        .getObjectMetadata(bucketName, objectSummary.getKey());
                // Validate user data creation date field
                final Optional<String> creationTime = Optional.ofNullable(
                        metadata.getUserMetadata().get(S3_CREATION_TIME));
                // for compatibility, if not present creation_time, took the lastModified time
                if (creationTime.isPresent()) {
                    try {
                        final String creationTimeString = metadata.getUserMetadata().get(S3_CREATION_TIME);
                        objectDate = Date.from(ZonedDateTime.parse(creationTimeString, dtf).toInstant());
                    } catch (DateTimeParseException e) {
                        objectDate = objectSummary.getLastModified();
                    }
                } else {
                    objectDate = objectSummary.getLastModified();
                }
                result.add(new S3ObjectInfo (objectSummary.getKey(), objectDate));
            }
            final String token = v2result.getNextContinuationToken();
            req.setContinuationToken(token);
            count += 100;
        } while (v2result.isTruncated() && count < maxReturn);
        // Once all the pagination is finalized, order the complete list by creation time
        final List<S3ObjectInfo> sortedList = result.stream()
                .sorted((o1, o2) -> o1.getCreationTime().compareTo(o2.getCreationTime()))
                .collect(Collectors.toList());

        return sortedList.stream()
                .map(S3ObjectInfo::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Return a list of object keys from a prefix
     * @param bucketName bucket Name to look for
     * @param prefix String representing the path
     * @return ArrayList of object keys
     */
    default List<String> getObjectListOrder(final String bucketName, final String prefix) {
        return getObjectListOrder(bucketName, prefix, Integer.MAX_VALUE);
    }

    /**
     * Return a list of object keys from a prefix
     * @param bucketName bucket Name to look for
     * @param prefix String representing the path
     * @return ArrayList of object keys
     */
    default List<String> getObjectList(final String bucketName, final String prefix) {
        return getObjectList(bucketName, prefix, Integer.MAX_VALUE);
    }

    /**
     * Remove an object from the bucket
     * @param bucketName bucket where the file should be removed
     * @param objectKey object name
     */
    default void removeObject(final String bucketName, final String objectKey) {
        getS3Client().deleteObject(bucketName, objectKey);
    }

    /**
     * Check if the object exists
     * @param bucketName the name of a bucket
     * @param objectKey the key of an object
     * @return boolean exists or not
     */
    default boolean isObjectExist(final String bucketName, final String objectKey) {
        try {
            getS3Client().getObjectMetadata(bucketName, objectKey);
            return true;
        } catch (AmazonS3Exception e) {
            return false;
        }
    }

    /**
     * Upload an object to the OSMN server
     * @param bucketName the name of a bucket
     * @param objectKey the name of an S3 object. S3Object use the term.
     * @param file the file to store
     * @throws InterruptedException an exception interrupt download
     */
    void uploadObject(final String bucketName, final String objectKey,
                      final File file) throws InterruptedException;

    /**
     * Upload an object to the OSMN server
     * @param bucketName bucket where the file should be saved
     * @param objectKey the key of object
     * @param stream input stream is the content of object
     * @throws InterruptedException an exception interrupts download
     * @throws IOException an exception breaks stream
     */
    void uploadObject(final String bucketName, final String objectKey,
                      final InputStream stream
    ) throws InterruptedException, IOException;

    /**
     * Upload an object to the default bucket
     *
     * @param objectKey the key of object
     * @param stream    input stream is the content of object
     * @throws InterruptedException the exception interrupts the upload
     * @throws IOException the exception breaks the stream
     */
    default void uploadObject(final String objectKey, final InputStream stream) throws InterruptedException, IOException {
        uploadObject(getDefaultBucketName(), objectKey, stream);
    }

    /**
     * Upload an object to the OSMN server
     * @param bucketName bucket where the file should be saved
     * @param objectKey object name
     * @param stream stream
     * @param metadata data
     * @throws InterruptedException an exception interrupts uploading
     */
    void uploadObject(final String bucketName, final String objectKey,
                      final InputStream stream, final ObjectMetadata metadata
    ) throws InterruptedException;

    /**
     * Upload an object to the OSMN server
     * @param objectKey the key of object
     * @param bytes the content of the object
     * @throws InterruptedException an exception interrupts uploading
     */
    default void uploadObject(final String objectKey,
                              final byte[] bytes) throws InterruptedException {
        uploadObject(getDefaultBucketName(), objectKey, bytes);
    }

    /**
     * Upload an object to the OSMN server
     * @param bucketName Bucket Name
     * @param objectKey Object name to be saved
     * @param bytes Byte array input stream
     * @throws InterruptedException an exception interrupt download
     */
    void uploadObject(final String bucketName, final String objectKey,
                      final byte[] bytes
    ) throws InterruptedException;

    /**
     * Download an object from a bucket
     * @param bucketName bucket Name
     * @param objectKey Object full path from S3
     * @param filePath Path to be created
     * @throws InterruptedException an exception interrupt download
     * @throws IOException the exception of writing file
     * @return the Path of the file created
     */
    Path downloadObject(final String bucketName,
                        final String objectKey,
                        final Path filePath) throws InterruptedException, IOException;

    /**
     * Download an object from the default bucket
     * @param objectKey the key of the object
     * @param fileFolder the destination folder
     * @return the Path of the file created
     * @throws InterruptedException an exception interrupts download
     * @throws IOException an exception break the stream
     */
    default Path downloadObject(final String objectKey, final Path fileFolder) throws InterruptedException, IOException {
        return downloadObject(getDefaultBucketName(), objectKey, fileFolder);
    }

    /**
     * Get the size of the object
     * @param bucketName the name of the bucket
     * @param objectKey the key of the object
     * @return the size of the object
     */
    default long getObjectSize(final String bucketName, final String objectKey) {
        return getS3Client().getObjectMetadata(bucketName, objectKey)
                .getContentLength();
    }

    /**
     * Convert a path into a key ready for use in OSMN
     * @param path - the path to be converted
     * @return a string which is safe for use as a key in OSMN
     * */
    static String toObjectKey(Path path) {
        path = path.toAbsolutePath().normalize();
        return path.getRoot().relativize(path).toString();
    }

    /**
     * Inverse of toObjectKey, converts an object key derived from a path back into an absolute normalized path
     * @param name - the key to be converted into a path
     * @return an absolute, normalized path
     * */
    static Path fromObjectKey(final String name) {
        return Path.of("/").getRoot() // get FS root
                .resolve(name) // resolve path against root
                .toAbsolutePath().normalize(); // make sure to deal in absolute normalized paths
    }

    /**
     * Used to order the object information after the pagination
     */
    class S3ObjectInfo {
        private final String key;
        private final Date creationDate;

        /**
         * Constructor
         * @param key Object key
         * @param creationDate to be used for order by.
         */
        public S3ObjectInfo(final String key, final Date creationDate) {
            super();
            this.key = key;
            this.creationDate = creationDate;
        }

        /**
         * Get the key object
         * @return object key
         */
        public String getKey() {
            return key;
        }

        /**
         * Get the creation date used to order by creation date
         * @return object creation date
         */
        public Date getCreationTime() {
            return creationDate;
        }
    }

}
