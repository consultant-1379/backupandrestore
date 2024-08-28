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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CustomInputStreamTest {
    private static final int BUFFER_BLOCK_SIZE = 512;
    private static final String TESTDATA = "Text string used to perform a test of an inputStream";
    private static final int TESTDATA_SIZE = 52;
    private CustomInputStream customInputStream;
    
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setup() {
        byte[] testData = TESTDATA.getBytes();
        InputStream inputStream = new ByteArrayInputStream(testData);
        customInputStream = new CustomInputStream(inputStream, BUFFER_BLOCK_SIZE);
    }

    @Test
    public void testRead() throws IOException {
        // Test the read() method of CustomInputStream
        int expectedFirstByte = (int) 'T';
        int firstByte = customInputStream.read();
        assertEquals(expectedFirstByte, firstByte);
    }

    @Test
    public void testReadArray() throws IOException {
        // Test the read(byte[] b) method of CustomInputStream
        byte[] buffer = new byte[TESTDATA_SIZE];
        int bytesRead = customInputStream.read(buffer);

        byte[] expectedBytes = TESTDATA.getBytes();
        assertEquals(expectedBytes.length, bytesRead);
        assertArrayEquals(expectedBytes, buffer);
    }

    @Test
    public void testReadArrayWithOffsetAndLength() throws IOException {
        // Test the read(byte[] b, int off, int len) method of CustomInputStream
        byte[] buffer = new byte[TESTDATA_SIZE];
        int offset = 0;
        int length = TESTDATA_SIZE;
        int bytesRead = customInputStream.read(buffer, offset, length);

        byte[] expectedBytes = TESTDATA.getBytes();
        assertEquals(expectedBytes.length, bytesRead);
        assertArrayEquals(expectedBytes, buffer);
    }

    @Test
    public void testSkip() throws IOException {
        // Test the skip(long n) method of CustomInputStream
        long expectedSkippedBytes = 7;
        long skippedBytes = customInputStream.skip(expectedSkippedBytes);
        assertEquals(expectedSkippedBytes, skippedBytes);
    }

    @Test
    public void testClose() throws IOException {
        // Test the close() method of CustomInputStream
        customInputStream.close();
        // Check if the InputStream is closed by calling read()
        exceptionRule.expect(IOException.class);
        exceptionRule.expectMessage("Stream closed");
        customInputStream.read();
    }

    @After
    public void cleanup() throws IOException {
        customInputStream.close();
    }
}
