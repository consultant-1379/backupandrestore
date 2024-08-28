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
package com.ericsson.adp.mgmt.backupandrestore.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;

import jakarta.xml.bind.DatatypeConverter;

import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;

/**
 * Calculates checksum.
 */
public class ChecksumCalculator {

    private static final String CHECKSUM_ALGORITHM = "MD5";

    private final MessageDigest digest;

    /**
     * Creates a checksum calculator.
     */
    public ChecksumCalculator() {
        try {
            this.digest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
        } catch (Exception e) {
            throw new BackupServiceException("Couldn't start checksum algorithm", e);
        }
    }

    /**
     * Checksum algorithm
     * @return checksum algorithm
     */
    public String getChecksumAlgorithm() {
        return CHECKSUM_ALGORITHM;
    }

    /**
     * Reads bytes.
     * @param bytes to be read.
     */
    public void addBytes(final byte[] bytes) {
        digest.update(bytes);
    }

    /**
     * Calculates checksum.
     * @return checksum.
     */
    public String getChecksum() {
        return toHex(digest.digest());
    }

    /**
     * Calculates checksum given a file path.
     * @param filePath file to calculate checksum of.
     * @return checksum of file.
     */
    public static String getChecksum(final String filePath) {
        final ChecksumCalculator calculator = new ChecksumCalculator();
        try (BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(filePath))) {
            final byte[] chunk = new byte[1];

            while (fileStream.read(chunk, 0, 1) != -1) {
                calculator.addBytes(chunk);
            }
        } catch (final IOException e) {
            throw new BackupServiceException("Exception reading backed up file for checksum calculation", e);
        }
        return calculator.getChecksum();
    }

    private String toHex(final byte[] bytes) {
        return DatatypeConverter.printHexBinary(bytes);
    }

}
