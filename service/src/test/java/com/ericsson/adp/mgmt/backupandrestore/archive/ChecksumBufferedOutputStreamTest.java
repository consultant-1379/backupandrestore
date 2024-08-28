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
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.archive;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.ericsson.adp.mgmt.backupandrestore.archive.ChecksumBufferedOutputStream;
import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumHash64;
import org.junit.Before;

import org.junit.Test;

public class ChecksumBufferedOutputStreamTest {

    private static int BLOCK_SIZE = 512;

    ChecksumBufferedOutputStream checksumOutputStream;
    ByteArrayOutputStream outputStream;

    @Before
    public void setUp() throws Exception {
        outputStream = new ByteArrayOutputStream();
        checksumOutputStream = new ChecksumBufferedOutputStream(outputStream , BLOCK_SIZE);
    }

    @Test
    public void createChecksum_setStream_getChecksum() throws IOException {
        ChecksumHash64 getHash64 = checksumOutputStream.getHash64();
        String checkSumValue="f05bddc3e6a543cf";
        byte[] data = "HelloWorld".getBytes();
        checksumOutputStream.write(data, 0, data.length);
        checksumOutputStream.close();
        assertTrue (checkSumValue.equalsIgnoreCase(getHash64.getStringValue()));
    }
}
