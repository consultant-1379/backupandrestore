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
package com.ericsson.adp.mgmt.backupandrestore.persist;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.ObjIntConsumer;

/**
 * It's a util class similar to the FileChunkServiceUtil in bro-agent-api.
 * It is responsible to chop the inputStream into a series of chunks and feed them to consumer.
 */
public class ProcessChunksUtil {
    private static final int NO_BYTES_READ = -1;
    private static final int DEFAULT_FILE_CHUNK_SIZE = 512 * 1024;

    private ProcessChunksUtil() {
    }

    /**
     * Gives chunks of bytes from the given inputStream
     *
     * @param inputStream
     *            the inputStream as the content to be consumed
     * @param chunkConsumer
     *            Consume file in chunks
     * @param fileChunkSize
     *            file chunk size, in bytes, to be consumed in each call
     * @throws IOException
     *             check file exists
     * @return the number of bytes processed
     */
    public static long processStreamChunks(final InputStream inputStream,
                                           final ObjIntConsumer<byte[]> chunkConsumer,
                                           final int fileChunkSize) throws IOException {
        long processed = 0;
        try (BufferedInputStream fileStream = new BufferedInputStream(inputStream)) {
            final byte[] chunk = new byte[fileChunkSize];
            int bytesReadInChunk;

            while ((bytesReadInChunk = fileStream.read(chunk, 0, fileChunkSize)) != NO_BYTES_READ) {
                chunkConsumer.accept(chunk, bytesReadInChunk);
                processed += bytesReadInChunk;
            }
        }
        return processed;
    }

    /**
     * Gives chunks of bytes from the given inputStream
     *
     * @param inputStream
     *            the inputStream as the content to be consumed
     * @param chunkConsumer
     *            Consume file in chunks
     * @throws IOException
     *             check file exists
     * @return the number of bytes processed
     */
    public static long processStreamChunks(final InputStream inputStream, final ObjIntConsumer<byte[]> chunkConsumer) throws IOException {
        return processStreamChunks(inputStream, chunkConsumer, DEFAULT_FILE_CHUNK_SIZE);
    }
}
