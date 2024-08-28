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
package com.ericsson.adp.mgmt.bro.api.filetransfer;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.function.ObjIntConsumer;

/**
 * Consumes file as chunks and lets the implementor do something with the chunks
 */

public class FileChunkServiceUtil {

    private static final int NO_BYTES_READ = -1;
    private static final int FILE_CHUNK_SIZE = 512 * 1024;

    /**
     * Prevents external instantiation.
     */
    private FileChunkServiceUtil() {
    }

    /**
     * Gives chunks of file from the given path
     *
     * @param path
     *            to the file to be consumed
     * @param chunkConsumer
     *            Consume file in chunks
     * @param fileChunkSize
     *            file chunk size, in bytes, to be consumed in each call
     * @throws IOException
     *             check file exists
     */
    public static void processFileChunks(final String path, final ObjIntConsumer<byte[]> chunkConsumer,
                                         final int fileChunkSize) throws IOException {
        try (BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(path))) {
            final byte[] chunk = new byte[fileChunkSize];
            int bytesReadInChunk;

            while ((bytesReadInChunk = fileStream.read(chunk, 0, fileChunkSize)) != NO_BYTES_READ) {
                chunkConsumer.accept(chunk, bytesReadInChunk);
            }
        }
    }

    /**
     * Gives chunks of file from the given path, using default file chunk size
     *
     * @param path
     *            to the file to be consumed
     * @param chunkConsumer
     *            Consume file in chunks
     * @throws IOException
     *             check file exists
     */
    public static void processFileChunks(final String path, final ObjIntConsumer<byte[]> chunkConsumer) throws IOException {
        processFileChunks(path, chunkConsumer, FILE_CHUNK_SIZE);
    }

}
