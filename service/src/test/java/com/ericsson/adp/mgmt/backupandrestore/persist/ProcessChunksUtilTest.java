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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessChunksUtilTest {

    @Test
    public void processStreamChunks_inputStream_valid() throws IOException {
        byte[] bytes = new byte[15];
        InputStream inputStream = new ByteArrayInputStream(bytes);
        AtomicInteger bytesReadInChunk = new AtomicInteger();
        ProcessChunksUtil.processStreamChunks(inputStream, (chuck, c) -> {
            bytesReadInChunk.set(c);
        });
        assertEquals(15, bytesReadInChunk.get());
    }
}
