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

public class CMMRestKeyStoreConfigurationTest {
    private static final String CERTIFICATES_PATH = "src/test/resources/";

    private CMMRestKeyStoreConfiguration config;
    @Value("${security.cmm.keystore.password:}")
    private String password = "";
    @Value("${security.cmm.keystore.path:}")
    private String keyStorePath = "";
    @Value("${security.cmm.client.key.path:}")
    private String cmmPrivateKeyPath = "";
    @Value("${security.cmm.client.cert.path:}")
    private String cmmCertPath = "";
    @Value("${security.siptls.root.ca.path:}")
    private String siptlsCaPath = "";

    @Test
    public void CMM_generateKeyStore_emptyPath_invalidConfiguration() {
        config = new CMMRestKeyStoreConfiguration(
                password,
                keyStorePath,
                cmmPrivateKeyPath,
                cmmCertPath,
                siptlsCaPath
        );
        assertFalse(config.isValidConfiguration());
    }

    @Test
    public void CMM_generateKeyStore_invalidConfiguration() throws Exception {
        keyStorePath = CERTIFICATES_PATH + "kafkakeystore.p12";
        password = "changeit";
        cmmPrivateKeyPath = CERTIFICATES_PATH + "invalid/key.key";
        cmmCertPath = CERTIFICATES_PATH + "clientcert1.pem";
        siptlsCaPath = CERTIFICATES_PATH + "/cmmserver/ca/client1.pem";
        config = new CMMRestKeyStoreConfiguration(
                password,
                keyStorePath,
                cmmPrivateKeyPath,
                cmmCertPath,
                siptlsCaPath
        );
        assertFalse(config.isValidConfiguration());
    }

    @Test
    public void CMM_generateKeyStore_invalidPath() throws Exception {
        keyStorePath = CERTIFICATES_PATH + "kafkakeystore.p12";
        password = "changeit";
        cmmPrivateKeyPath = "c:/invalidPath.key";
        cmmCertPath = CERTIFICATES_PATH + "clientcert.pem";
        config = new CMMRestKeyStoreConfiguration(
                password,
                keyStorePath,
                cmmPrivateKeyPath,
                cmmCertPath,
                siptlsCaPath
        );
        assertFalse(config.isValidConfiguration());
    }

    @Test
    public void CMM_generateKeyStore_validConfiguration() throws Exception {
        keyStorePath = CERTIFICATES_PATH + "kafkakeystore.p12";
        password = "changeit";
        cmmPrivateKeyPath = CERTIFICATES_PATH + "clientprivkey.key";
        cmmCertPath = CERTIFICATES_PATH + "clientcert.pem";
        siptlsCaPath = CERTIFICATES_PATH + "/cmmserver/ca/client-cacertbundle.pem";
        config = new CMMRestKeyStoreConfiguration(
                password,
                keyStorePath,
                cmmPrivateKeyPath,
                cmmCertPath,
                siptlsCaPath
        );
        assertTrue(config.isValidConfiguration());
    }
}
