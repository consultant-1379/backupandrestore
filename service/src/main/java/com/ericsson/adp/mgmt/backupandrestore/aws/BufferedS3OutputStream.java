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
package com.ericsson.adp.mgmt.backupandrestore.aws;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.S3_CREATION_TIME;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.S3_DATE_FORMAT;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3MultipartClient;
import com.ericsson.adp.mgmt.backupandrestore.exception.AWSException;

/**
 * A buffered output stream for uploading large object to BRO in a streaming manner
 * */
public class BufferedS3OutputStream extends OutputStream {

    private static final Logger log = LogManager.getLogger(BufferedS3OutputStream.class);


    private final List<PartETag> partETags;
    private final byte[] buffer;
    private final S3MultipartClient client;
    private final String key;

    private int bufferPos;

    // Default minimum part size is 5 MB
    private final int chunkSize;
    private long objectSize;
    private Optional<InitiateMultipartUploadResult> initResponse = Optional.empty();
    private int partNumber;

    /**
     * Construct a BufferedAWSOutputStream
     * @param client - the client used to communicate with AWS
     * @param key - the object key to store the written data to
     * @param chunkSize - the minimum size of each chunk to be uploaded
     * */
    public BufferedS3OutputStream(final S3MultipartClient client, final String key, final int chunkSize) {
        this.client = client;
        client.createBucket();
        partNumber = 1;
        objectSize = 0;
        partETags = new ArrayList<>();
        this.key = key;
        this.chunkSize = Math.max(chunkSize, S3MultipartClient.PART_SIZE_LOWER_BOUND);
        this.buffer = new byte[chunkSize];
        this.bufferPos = 0;
    }

    @Override
    public void write(final byte[] input, final int off, final int len) {
        final int space = chunkSize - bufferPos;
        final int toCopy = Math.min(space, len);
        System.arraycopy(input, off, buffer, bufferPos, toCopy);
        bufferPos += toCopy;
        if (bufferPos == chunkSize) {
            uploadPart();
        }
        if (toCopy < len) {
            write(input, off + toCopy, len - toCopy);
        }
    }

    @Override
    public void write(final int inputByte) {
        buffer[bufferPos] = (byte) inputByte; // safe cast as interface specifies upper 24 bits ignored
        bufferPos += 1;
        if (bufferPos >= chunkSize) {
            uploadPart();
        }
    }

    @Override
    public void close() {
        if (initResponse.isEmpty()) {
            log.debug("The object size was less than minimum multipart upload, uploading all at once.");
            final InputStream inputStream = new ByteArrayInputStream(buffer, 0, bufferPos);
            client.getS3Client().putObject(client.getDefaultBucketName(), key, inputStream, null);
        } else {
            if (bufferPos > 0) {
                uploadPart();
            } else {
                log.debug("Skipping final part upload as no bytes to send");
            }

            log.debug("The object size is {}", objectSize);
            if (initResponse.isPresent()) {
                final CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(client.getDefaultBucketName(), key,
                        initResponse.get().getUploadId(), partETags);
                client.getS3Client().completeMultipartUpload(compRequest);
            } else {
                throw new AWSException("Upload re-initialized during stream closing");
            }
            initResponse = Optional.empty();
        }
    }

    private void initUpload() {
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(S3_DATE_FORMAT)
                .withZone(ZoneId.systemDefault());
        final String creationTime = dtf.format(Instant.now());

        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.addUserMetadata(S3_CREATION_TIME, creationTime);
        final InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(client.getDefaultBucketName(), key);
        initRequest.withObjectMetadata(metadata);
        initResponse = Optional.of(client.getS3Client().initiateMultipartUpload(initRequest));
    }

    private void uploadPart() {
        if (initResponse.isEmpty()) {
            initUpload();
            uploadPart(); // Once we've started the upload, circle back and do the below.
        } else { // This else is painfully required or else sonarqube throws a fit about the Optional.get in this block
            final UploadPartRequest uploadRequest = new UploadPartRequest()
                    .withBucketName(client.getDefaultBucketName())
                    .withKey(key)
                    .withUploadId(initResponse.get().getUploadId())
                    .withPartNumber(partNumber++)
                    .withInputStream(new ByteArrayInputStream(buffer, 0, bufferPos))
                    .withPartSize(bufferPos);

            objectSize += bufferPos;
            bufferPos = 0;
            final PartETag eTag = client.getS3Client().uploadPart(uploadRequest).getPartETag();
            partETags.add(eTag);
        }
    }
}
