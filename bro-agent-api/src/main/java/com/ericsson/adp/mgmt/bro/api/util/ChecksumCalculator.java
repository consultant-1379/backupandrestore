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
package com.ericsson.adp.mgmt.bro.api.util;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jakarta.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ericsson.adp.mgmt.bro.api.filetransfer.FileChunkServiceUtil;

/**
 * Calculates Checksum
 */
public class ChecksumCalculator {

    private static final Logger log = LogManager.getLogger(ChecksumCalculator.class);

    private MessageDigest digest;

    /**
     * Creates a checksum calculator.
     */
    public ChecksumCalculator() {
        try {
            this.digest = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException e) {
            log.error("Checksum algorithm does not exists", e);
        }
    }

    /**
     * Reads bytes sent during restore.
     * @param bytes to be read.
     */
    public void addBytes(final byte[] bytes) {
        digest.update(bytes);
    }

    /**
     * Calculates checksum from byte array
     * @param bytes chunk
     * @param offset offset
     * @param length bytes read in chunk
     */
    public void addBytes(final byte[] bytes, final int offset, final int length) {
        digest.update(bytes, offset, length);
    }

    /**
     * Calculates checksum.
     * @return checksum.
     */
    public String getChecksum() {
        return toHex(digest.digest());
    }

    /**
     * Calculates checksum
     * @param path to file
     * @return checksum value
     * @throws IOException check file exists
     */
    public String calculateChecksum(final String path) throws IOException {
        FileChunkServiceUtil.processFileChunks(path, (chunk, bytesReadInChunk) -> digest.update(chunk, 0, bytesReadInChunk));
        return toHex(digest.digest());
    }

    /**
     * Converts array of bytes to string
     * @param bytes value of checksum
     * @return string value of checksum
     */
    private String toHex(final byte[] bytes) {
        return DatatypeConverter.printHexBinary(bytes);
    }
}
