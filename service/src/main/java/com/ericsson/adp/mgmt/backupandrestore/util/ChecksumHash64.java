/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;

import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;

/**
 * xxHash64 creates xxHash64 checksum for given file.
 *
 * https://gist.github.com/jpountz/4489305 for original example.
 */
public class ChecksumHash64 {
    private final XXHashFactory factory;
    private final StreamingXXHash64 hash64;

    /**
     * Constructor sets xxHash64
     */
    public ChecksumHash64() {
        this.factory = XXHashFactory.fastestInstance();
        this.hash64 = factory.newStreamingHash64(0);
    }

    /**
     * This method createsChecksum for given file
     *
     * @param file
     *            file
     *
     * @return checksum
     */
    public String createCheckSum(final File file) {
        final int BLOCK_SIZE = 32 * 1024;

        long hash = 0;
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            final byte[] bufferBlock = new byte[BLOCK_SIZE];
            int read;
            while ((read = fileInputStream.read(bufferBlock)) != -1) {
                updateHash64(bufferBlock, read);
            }
            hash = hash64.getValue();
        } catch (IOException e) {
            throw new BackupServiceException("Exception reading backed up file for checksum calculation", e);
        } finally {
            this.hash64.reset();
        }
        return String.format("%016x", hash);
    }

    /**
     * Updates the value of the hash with char buf[off:off+len].
     *
     * @param bufferBlock Byte array to use as input data
     * @param len the number of bytes to hash
   */
    public void updateHash64(final byte[] bufferBlock, final int len) {
        hash64.update(bufferBlock, 0, len);
    }

    public long getNumericValue() {
        return hash64.getValue();
    }

    public String getStringValue() {
        return String.format("%016x", hash64.getValue());
    }

    /**
     * For a given compressedBackup and checksumFile will validate if for the correct checksum
     *
     * @param compressedBackup
     *            compressedBackup
     * @param checksum
     *            checksum
     * @return boolean
     */
    public boolean isValidChecksum(final File compressedBackup, final File checksum) {
        String actualChecksum = "";
        final String expectedChecksum = createCheckSum(compressedBackup);
        try {
            actualChecksum = new String(Files.readAllBytes(Paths.get(checksum.getAbsolutePath())));
        } catch (Exception e) {
            throw new BackupServiceException("Failed to read from checksum file", e);
        }
        return actualChecksum.contentEquals(expectedChecksum);
    }

}
