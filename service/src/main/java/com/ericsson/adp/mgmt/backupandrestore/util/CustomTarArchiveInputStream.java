/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * A custom TarArchiveInputStream used to keep a reference of the internal InputStream
 * Current use is to ensure that the all the bytes of the InputStream is read
 */
public class CustomTarArchiveInputStream extends TarArchiveInputStream{

    private final InputStream originalStream;

    /**
     * Construct a custom TarArchiveInputStream
     * @param inputStream InputStream
     * @param blockSize block size used for cache
     */
    public CustomTarArchiveInputStream(final InputStream inputStream, final int blockSize) {
        super(inputStream, blockSize);
        originalStream = inputStream;
    }

    /**
     * When unpacking an uncompressed archive stream, byte skipping in the inputStream occurs.
     * This causes a checksum mismatch eventually leading to import operation failure.
     * Calling this method prevents this issue by ensuring that the internal inputstream's readAllBytes method
     * is executed during the unpacking process.
     * @throws IOException the I/O exception thrown
     */
    public void readRemainingBytes() throws IOException {
        try {
            super.readAllBytes();
            originalStream.readAllBytes();
        } catch (IOException e) {
            throw new IOException("Error processing TarArchiveInputStream", e);
        }
    }
}
