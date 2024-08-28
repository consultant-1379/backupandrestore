/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.restore;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;

public class ChecksumValidatorTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private ChecksumValidator checksumValidator;

    @Before
    public void setup() throws Exception {
        final S3Config s3Config = new S3Config();
        s3Config.setEnabled(false);
        this.checksumValidator = new ChecksumValidator(s3Config, null);
    }

    @Test
    public void validate_fileExists_checksumMatches() throws Exception {
        Path testFilePath = folder.getRoot().toPath().resolve("test.md5");
        Files.write(testFilePath, "CAFEBABE".getBytes());
        checksumValidator.validate("CAFEBABE", testFilePath);
    }

    @Test(expected = ChecksumValidationException.class)
    public void validate_fileExists_checksumDoesNotMatch() throws Exception {
        Path testFilePath = folder.getRoot().toPath().resolve("test.md5");
        Files.write(testFilePath, "BABECAFE".getBytes());
        checksumValidator.validate("CAFEBABE", testFilePath);
    }

    @Test
    public void validate_fileDoesNotExist() {
        checksumValidator.validate("CAFEBABE", folder.getRoot().toPath().resolve("none.md5"));
    }

}
