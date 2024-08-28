/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
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

/**
 * A custom InputStream used to calculate the checksum
 * Current use is to ensure that the all bytes of the InputStream is read
 */
public class CustomInputStream extends InputStream {
    private ChecksumHash64 hash64;
    private final InputStream originalInputStream;
    private final int blockSize;
    private final byte[] buffer;
    private int bufferPos;
    private int bufferLength;
    private boolean isClosed;


    /**
     * Constructor
     * @param inputStream original Input Stream
     * @param blockSize block size used to keep a byte cache when reading from original input stream.
     */
    public CustomInputStream(final InputStream inputStream, final int blockSize) {
        super();
        hash64 = new ChecksumHash64();
        originalInputStream = inputStream;
        this.blockSize = blockSize;
        buffer = new byte[blockSize];
        bufferPos = 0;
        bufferLength = 0;
    }

    @Override
    public synchronized int read(final byte[] buffered, final int offset, final int numToRead) throws IOException {
        if (bufferPos >= bufferLength) {
            bufferLength = originalInputStream.read(buffer, 0, blockSize);
            if (bufferLength > 0) {
                hash64.updateHash64(buffer, bufferLength);
                bufferPos = 0;
            } else {
                return -1; // EOF
            }
        }

        final int bytesToCopy = Math.min(numToRead, bufferLength - bufferPos);
        System.arraycopy(buffer, bufferPos, buffered, offset, bytesToCopy);
        bufferPos += bytesToCopy;
        return bytesToCopy;
    }

    @Override
    public int read() throws IOException {
        if (isClosed) {
            throw new IOException("Stream closed");
        }
        if (bufferPos >= bufferLength) {
            bufferLength = originalInputStream.read(buffer, 0, blockSize);
            if (bufferLength > 0) {
                hash64.updateHash64(buffer, bufferLength);
                bufferPos = 0;
            } else {
                return -1; // EOF
            }
        }

        return buffer[bufferPos++];
    }

    @Override
    public void close() throws IOException {
        originalInputStream.close();
        isClosed = true;
    }

    public ChecksumHash64 getHash64() {
        return hash64;
    }

    public void setHash64(final ChecksumHash64 hash64) {
        this.hash64 = hash64;
    }
}
