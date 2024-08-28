/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.restore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3MultipartClient;
import com.ericsson.adp.mgmt.backupandrestore.exception.RestoreDownloadException;

/**
 * ChecksumValidator is responsible for validating the calculated and stored checksum.
 */
public class ChecksumValidator {

    private final S3Config s3Config;
    private final S3MultipartClient s3MultipartClient;

    /**
     * @param s3Config
     *            - the configuration of OSMN
     * @param s3MultipartClient
     *            - the OSMN client
     */
    public ChecksumValidator(final S3Config s3Config, final S3MultipartClient s3MultipartClient) {
        this.s3Config = s3Config;
        this.s3MultipartClient = s3MultipartClient;
    }

    /**
     * If a stored checksum exists, validates if a calculated checksum matches the content of checksum object key.
     * If the stored checksum does not exist the comparison validation is successful.
     * @param calculatedChecksum
     *              - the calculated checksum
     * @param checksumObjectKey
     *              - the checksum object key
     * @throws ChecksumValidationException if the result of the checksums comparison fails
     */
    public void validateFromOSMN(final String calculatedChecksum, final String checksumObjectKey) {
        if (!isValidFromOSMN(calculatedChecksum, checksumObjectKey)) {
            throw new ChecksumValidationException("Checksum mismatch: <" + checksumObjectKey + ">");
        }
    }

     /**
     * If a stored checksum exists, validates if a calculated checksum matches the content of checksum file.
     * If the stored checksum does not exist the comparison validation is successful.
     * @param calculatedChecksum
     *              - the calculated checksum
     * @param checksumFilePath
     *              - the checksum file path
     * @throws ChecksumValidationException if the result of the checksums comparison fails
     */
    public void validate(final String calculatedChecksum, final Path checksumFilePath) {
        if (!isValid(calculatedChecksum, checksumFilePath)) {
            throw new ChecksumValidationException("Checksum mismatch: <" + checksumFilePath + ">");
        }
    }

    private boolean isValidFromOSMN(final String calculatedChecksum, final String checksumObjectKey) {
        if (s3MultipartClient.isObjectExist(s3Config.getDefaultBucketName(), checksumObjectKey)) {
            try (InputStream checksumInputStream = s3MultipartClient.downloadObject(s3Config.getDefaultBucketName(),
            checksumObjectKey)) {
                return isValid(calculatedChecksum, checksumInputStream);
            } catch (IOException e) {
                throw new RestoreDownloadException("Error validating checksum  <" + checksumObjectKey + ">", e);
            }
        }
        return true;
    }

    private boolean isValid(final String calculatedChecksum, final Path checksumFilePath) {
        if (Files.exists(checksumFilePath)) {
            try (InputStream checksumInputStream = Files.newInputStream(checksumFilePath)) {
                return isValid(calculatedChecksum, checksumInputStream);
            } catch (IOException e) {
                throw new RestoreDownloadException("Error validating checksum  <" + checksumFilePath + ">", e);
            }
        }
        return true;
    }

    private boolean isValid(final String calculatedChecksum, final InputStream checksumInputStream) throws IOException {
        final String savedChecksum = new String(checksumInputStream.readAllBytes());
        return calculatedChecksum.equals(savedChecksum);
    }

}
