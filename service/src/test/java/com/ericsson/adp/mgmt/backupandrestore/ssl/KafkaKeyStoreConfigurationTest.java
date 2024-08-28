/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.ssl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:kafkaTest.properties")
public class KafkaKeyStoreConfigurationTest {
    private static final String CERTIFICATES_PATH = "src/test/resources/";

    private KafkaKeyStoreConfiguration kafkaConfig;
    @Value("${security.kafka.keystore.path:}")
    private String keyStorePath;
    @Value("${security.kafka.keystore.password:}")
    private String password;
    @Value("${security.kafka.client.key.path:}")
    private String privateKeyPath;
    @Value("${security.kafka.client.cert.path:}")
    private String certPath;
    @Value("${security.siptls.root.ca.path:}")
    private String rootCaPath;

    @Test
    public void configureTomcat_generateKeyStore_emptyPath_invalidConfiguration() {
        kafkaConfig = new KafkaKeyStoreConfiguration("", password,
                privateKeyPath, certPath, rootCaPath);
        assertFalse(kafkaConfig.isValidConfiguration());
    }

    @Test
    public void configureTomcat_generateKeyStore_invalidConfiguration() throws Exception {
        privateKeyPath = CERTIFICATES_PATH + "invalid/key.key";
        kafkaConfig = new KafkaKeyStoreConfiguration(keyStorePath, password,
                privateKeyPath, certPath, rootCaPath);
        assertFalse(kafkaConfig.isValidConfiguration());
    }

    @Test
    public void configureTomcat_generateKeyStore_invalidPath() throws Exception {
        privateKeyPath = "c:/invalidPath.key";
        kafkaConfig = new KafkaKeyStoreConfiguration(keyStorePath, password,
                privateKeyPath, certPath, rootCaPath);
        assertFalse(kafkaConfig.isValidConfiguration());
    }

    @Test
    public void configureTomcat_generateKeyStore_validConfiguration() throws Exception {
        kafkaConfig = new KafkaKeyStoreConfiguration(keyStorePath, password,
                privateKeyPath, certPath, rootCaPath);
        assertTrue(kafkaConfig.isValidConfiguration());
    }
}
