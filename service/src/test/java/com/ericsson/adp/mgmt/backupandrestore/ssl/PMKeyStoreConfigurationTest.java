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
package com.ericsson.adp.mgmt.backupandrestore.ssl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

public class PMKeyStoreConfigurationTest {
    private static final String CERTIFICATES_PATH = "src/test/resources/";

    private PMKeyStoreConfiguration config;
    @Value("${security.cmm.keystore.password:}")
    private String password = "";
    @Value("${security.cmm.keystore.path:}")
    private String keyStorePath = "";
    @Value("${security.bro.server.key.path:}")
    private String broServerKeyPath = "";
    @Value("${security.bro.server.cert.path:}")
    private String broServerCertPath = "";
    @Value("${security.pm.client.ca.path:}")
    private String pmClientAuthorityPath = "";

    @Test
    public void CMM_generateKeyStore_emptyPath_invalidConfiguration() {
        config = new PMKeyStoreConfiguration(
                password,
                keyStorePath,
                broServerKeyPath,
                broServerCertPath,
                pmClientAuthorityPath
        );
        assertFalse(config.isValidConfiguration());
    }

    @Test
    public void CMM_generateKeyStore_invalidConfiguration() throws Exception {
        keyStorePath = CERTIFICATES_PATH + "kafkakeystore.p12";
        password = "changeit";
        broServerKeyPath = CERTIFICATES_PATH + "invalid/key.key";
        broServerCertPath = CERTIFICATES_PATH + "clientcert1.pem";
        pmClientAuthorityPath = CERTIFICATES_PATH + "/cmmserver/ca/client1.pem";
        config = new PMKeyStoreConfiguration(
                password,
                keyStorePath,
                broServerKeyPath,
                broServerCertPath,
                pmClientAuthorityPath
        );
        assertFalse(config.isValidConfiguration());
    }

    @Test
    public void CMM_generateKeyStore_invalidPath() throws Exception {
        keyStorePath = CERTIFICATES_PATH + "kafkakeystore.p12";
        password = "changeit";
        broServerKeyPath = "c:/invalidPath.key";
        broServerCertPath = CERTIFICATES_PATH + "clientcert.pem";
        config = new PMKeyStoreConfiguration(
                password,
                keyStorePath,
                broServerKeyPath,
                broServerCertPath,
                pmClientAuthorityPath
        );
        assertFalse(config.isValidConfiguration());
    }

    @Test
    public void CMM_generateKeyStore_validConfiguration() throws Exception {
        keyStorePath = CERTIFICATES_PATH + "kafkakeystore.p12";
        password = "changeit";
        broServerKeyPath = CERTIFICATES_PATH + "clientprivkey.key";
        broServerCertPath = CERTIFICATES_PATH + "clientcert.pem";
        pmClientAuthorityPath = CERTIFICATES_PATH + "/cmmserver/ca/client-cacertbundle.pem";
        config = new PMKeyStoreConfiguration(
                password,
                keyStorePath,
                broServerKeyPath,
                broServerCertPath,
                pmClientAuthorityPath
        );
        assertTrue(config.isValidConfiguration());
    }
}
