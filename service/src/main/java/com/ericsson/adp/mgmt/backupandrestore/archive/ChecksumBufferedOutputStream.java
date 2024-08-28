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
package com.ericsson.adp.mgmt.backupandrestore.archive;

import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumHash64;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrap the BufferedOutputStream to include the checksum when output
 */
public class ChecksumBufferedOutputStream extends BufferedOutputStream {
    private ChecksumHash64 hash64;

    /**
     * Constructor
     * @param out Output stream to be wrapped
     * @param size Block size to keep for cache
     */
    public ChecksumBufferedOutputStream(final OutputStream out, final int size) {
        super(out, size);
        hash64 = new ChecksumHash64();
    }

    @Override
    public synchronized void write(final byte[] buffered, final int off, final int len) throws IOException {
        super.write(buffered, off, len);
        hash64.updateHash64 (buffered, len);
    }

    /**
     * Get the hash64 object
     * @return hash64 object
     */
    public ChecksumHash64 getHash64() {
        return hash64;
    }

    public void setHash64 (final ChecksumHash64 hash64 ) {
        this.hash64 = hash64;
    }
}
