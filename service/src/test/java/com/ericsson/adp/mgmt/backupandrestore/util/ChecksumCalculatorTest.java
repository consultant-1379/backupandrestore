/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Test;

public class ChecksumCalculatorTest {

    @Test
    public void getChecksum_feedBytesAndCalculateChecksum_actualChecksum() throws Exception {
        final ChecksumCalculator checksumCalculator = new ChecksumCalculator();

        sendBytes(checksumCalculator);

        assertEquals("E7F1384EDF7893A51D331F51B478CB12", checksumCalculator.getChecksum());
    }

    @Test
    public void getChecksum_filePath_actualChecksum() throws Exception {
        assertEquals(getExpectedChecksum(), ChecksumCalculator.getChecksum("src/test/resources/test.txt"));
    }

    private String getExpectedChecksum() {
        final ChecksumCalculator checksumCalculator = new ChecksumCalculator();
        try (final BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream("src/test/resources/test.txt"))) {
            final byte[] chunk = new byte[1];

            while (fileStream.read(chunk, 0, 1) != -1) {
                checksumCalculator.addBytes(chunk);
            }
        } catch (final IOException e) {
            fail();
        }
        return checksumCalculator.getChecksum();
    }

    private void sendBytes(ChecksumCalculator checksumCalculator) {
        final String contentToSend = "abcdefghijklmnopqrstuvyxwz";

        for(byte singleByte : contentToSend.getBytes()) {
            checksumCalculator.addBytes(new byte[] { singleByte });
        }
    }

}
