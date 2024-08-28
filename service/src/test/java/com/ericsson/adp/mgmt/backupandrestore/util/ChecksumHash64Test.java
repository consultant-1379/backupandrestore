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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Optional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;
public class ChecksumHash64Test {
    private ChecksumHash64 hash64;

    @Before
    public void setup() {
        hash64 = new ChecksumHash64();
    }

    /**
     * TODO: Alter line endings to suit windows or unix, current checksum calculated for unix line endings
     */
    @Test
    public void testExpectedChecksum() {
        String checksum = hash64.createCheckSum(new File("src/test/resources/test.txt"));
        assertEquals("9cb5fb0c4d9d7cca", checksum);
    }

    @Test (expected = BackupServiceException.class)
    public void testIsValidChecksum_invalidFile() {
        File checksumFile = new File("src/test/resources/test2.txt");
        hash64.isValidChecksum(new File("src/test/resources/test.txt"), checksumFile);
    }
}
