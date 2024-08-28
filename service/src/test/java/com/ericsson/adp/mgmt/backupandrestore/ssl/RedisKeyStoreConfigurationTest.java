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
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.ssl;

import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.REDIS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RedisKeyStoreConfigurationTest {
    private static final String CERTIFICATES_PATH = "src/test/resources/";
    private RedisKeyStoreConfiguration redisKeyStoreConfiguration;

    private String keyStorePath="";
    private String password="";
    private String privateKeyPath="";
    private String certPath="";
    private final String rootCaPath="";

    @Test
    public void constructor_invalidPath_invalid() {
        redisKeyStoreConfiguration = new RedisKeyStoreConfiguration(keyStorePath, password, privateKeyPath, certPath, rootCaPath);
        assertFalse(redisKeyStoreConfiguration.isValidConfiguration());
    }

    @Test
    public void constructor_invalidKey_invalid() {
        keyStorePath = CERTIFICATES_PATH + "rediskeystore.p12";
        password = "changeit";
        privateKeyPath = CERTIFICATES_PATH + "invalid/key.key";
        certPath = CERTIFICATES_PATH + "clientcert.pem";
        redisKeyStoreConfiguration = new RedisKeyStoreConfiguration(keyStorePath, password, privateKeyPath, certPath, rootCaPath);
        assertFalse(redisKeyStoreConfiguration.isValidConfiguration());
    }


    @Test
    public void getAlias_valid() {
        keyStorePath = CERTIFICATES_PATH + "kafkakeystore.p12";
        password = "changeit";
        privateKeyPath = CERTIFICATES_PATH + "clientprivkey.key";
        certPath = CERTIFICATES_PATH + "clientcert.pem";
        redisKeyStoreConfiguration = new RedisKeyStoreConfiguration(keyStorePath, password, privateKeyPath, certPath, rootCaPath);
        assertTrue(redisKeyStoreConfiguration.isValidConfiguration());
        assertEquals(REDIS,redisKeyStoreConfiguration.getAlias());
    }

    @Test(expected = Test.None.class)
    public void postRefreshHook_mock_valid() {
        keyStorePath = CERTIFICATES_PATH + "kafkakeystore.p12";
        password = "changeit";
        privateKeyPath = CERTIFICATES_PATH + "clientprivkey.key";
        certPath = CERTIFICATES_PATH + "clientcert.pem";
        redisKeyStoreConfiguration = new RedisKeyStoreConfiguration(keyStorePath, password, privateKeyPath, certPath, rootCaPath);

        redisKeyStoreConfiguration.postRefreshHook(true);
    }
}
