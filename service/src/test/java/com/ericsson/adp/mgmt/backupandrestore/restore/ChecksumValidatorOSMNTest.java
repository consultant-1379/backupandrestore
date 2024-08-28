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

import org.easymock.EasyMock;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3MultipartClient;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;

import io.findify.s3mock.S3Mock;

public class ChecksumValidatorOSMNTest {
    private static final S3Mock api = new S3Mock.Builder().withPort(28001).withInMemoryBackend().build();

    private static S3MultipartClient s3MultipartClient;
    private static String defaultBucketName;
    private static S3Config s3Config;

    private ChecksumValidator checksumValidator;

    @BeforeClass
    public static void setupClass() {
        //set up the mock server
        api.start();
        defaultBucketName = "bro";
        s3Config = new S3Config();
        s3Config.setEnabled(true);
        s3Config.setDefaultBucketName(defaultBucketName);
        s3Config.setHost("http://0.0.0.0");
        s3Config.setPort("28001");
        s3Config.setAccessKeyName("ak");
        s3Config.setSecretKeyName("sk");
        s3Config.setConnectionTimeout(1000);
        s3Config.setRetriesStartUp(0);
        s3Config.setRetriesOperation(0);

        final KeyStoreService service = EasyMock.createMock(KeyStoreService.class);
        EasyMock.expect(service.getKeyStore(KeyStoreAlias.OSMN)).andReturn(null).anyTimes();
        EasyMock.replay(service);
        s3Config.setKeyStoreService(service);

        s3MultipartClient = new S3MultipartClient(s3Config);
        s3MultipartClient.createBucket();
    }

    @AfterClass
    public static void tearDownClass() {
        api.shutdown();
    }

    @Before
    public void setup() throws Exception {
        this.checksumValidator = new ChecksumValidator(s3Config, s3MultipartClient);
    }

    @Test
    public void validateFromOSMN_objectKeyExists_checksumMatches() throws Exception {
        s3MultipartClient.uploadObject("backup.txt.md5", "CAFEBABE".getBytes());
        checksumValidator.validateFromOSMN("CAFEBABE", "backup.txt.md5");
    }

    @Test(expected = ChecksumValidationException.class)
    public void validateFromOSMN_objectKeyExists_checksumDoesNotMatch() throws Exception {
        s3MultipartClient.uploadObject("backup.txt.md5", "BABECAFE".getBytes());
        checksumValidator.validateFromOSMN("CAFEBABE", "backup.txt.md5");
    }

    @Test
    public void validateFromOSMN_objectKeyDoesNotExist() {
        checksumValidator.validateFromOSMN("CAFEBABE", "backup.md5");
    }

}
