/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
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

public class BroKeyStoreConfigurationTest {
    private static final String CERTIFICATES_PATH = "src/test/resources/";

    private BroKeyStoreConfiguration config;
    @Value("${security.bro.keystore.password:}")
    private String password = "";
    @Value("${security.bro.keystore.path:}")
    private String keyStorePath = "";
    @Value("${security.bro.server.key.path:}")
    private String broPrivateKeyPath = "";
    @Value("${security.bro.server.cert.path:}")
    private String broServerCertPath = "";
    @Value("${security.cmyp.client.ca.path:}")
    private String cmypClientCaPath = "";
    @Value("${security.bro.ca.path:}")
    private String broCaPath = "";
    @Value("${security.cmm.client.ca.path.action:}")
    private String cmmActionsClientCaPath = "";
    @Value("${security.cmm.client.ca.path.state:}")
    private String cmmStateClientCaPath = "";
    @Value("${security.cmm.client.ca.path.validator:}")
    private String cmmValidatorClientCaPath = "";

    @Test
    public void CMMNotificationEndpoint_generateKeyStore_emptyPath_invalidConfiguration() {
        config = new BroKeyStoreConfiguration(
                password,
                keyStorePath,
                broPrivateKeyPath,
                broServerCertPath,
                cmypClientCaPath,
                broCaPath,
                cmmActionsClientCaPath,
                cmmStateClientCaPath,
                cmmValidatorClientCaPath
        );
        assertFalse(config.isValidConfiguration());
    }

    @Test
    public void CMMNotificationEndpoint_generateKeyStore_invalidConfiguration() throws Exception {
        keyStorePath = CERTIFICATES_PATH + "kafkakeystore.p12";
        password = "changeit";
        broPrivateKeyPath = CERTIFICATES_PATH + "invalid/key.key";
        broServerCertPath = CERTIFICATES_PATH + "clientcert.pem";
        config = new BroKeyStoreConfiguration(
                password,
                keyStorePath,
                broPrivateKeyPath,
                broServerCertPath,
                cmypClientCaPath,
                broCaPath,
                cmmActionsClientCaPath,
                cmmStateClientCaPath,
                cmmValidatorClientCaPath
        );
        assertFalse(config.isValidConfiguration());
    }

    @Test
    public void CMMNotificationEndpoint_generateKeyStore_invalidPath() throws Exception {
        keyStorePath = CERTIFICATES_PATH + "kafkakeystore.p12";
        password = "changeit";
        broPrivateKeyPath = "c:/invalidPath.key";
        broServerCertPath = CERTIFICATES_PATH + "clientcert.pem";
        config = new BroKeyStoreConfiguration(
                password,
                keyStorePath,
                broPrivateKeyPath,
                broServerCertPath,
                cmypClientCaPath,
                broCaPath,
                cmmActionsClientCaPath,
                cmmStateClientCaPath,
                cmmValidatorClientCaPath
        );
        assertFalse(config.isValidConfiguration());
    }

    @Test
    public void CMMNotificationEndpoint_generateKeyStore_validConfiguration_withLegacyCA() throws Exception {
        keyStorePath = CERTIFICATES_PATH + "kafkakeystore.p12";
        password = "changeit";
        broPrivateKeyPath = CERTIFICATES_PATH + "clientprivkey.key";
        broServerCertPath = CERTIFICATES_PATH + "clientcert.pem";
        cmypClientCaPath = CERTIFICATES_PATH + "/cmmserver/ca/client-cacertbundle.pem";
        config = new BroKeyStoreConfiguration(
                password,
                keyStorePath,
                broPrivateKeyPath,
                broServerCertPath,
                cmypClientCaPath,
                broCaPath,
                cmmActionsClientCaPath,
                cmmStateClientCaPath,
                cmmValidatorClientCaPath
        );
        assertTrue(config.isValidConfiguration());
    }

    @Test
    public void CMMNotificationEndpoint_generateKeyStore_validConfiguration_withCmeiaCA() throws Exception {
        keyStorePath = CERTIFICATES_PATH + "kafkakeystore.p12";
        password = "changeit";
        broPrivateKeyPath = CERTIFICATES_PATH + "clientprivkey.key";
        broServerCertPath = CERTIFICATES_PATH + "clientcert.pem";
        cmypClientCaPath = CERTIFICATES_PATH + "NOT_USED";
        cmmActionsClientCaPath = CERTIFICATES_PATH + "/action-client-cacert/client-cacert.pem";
        cmmStateClientCaPath = CERTIFICATES_PATH + "/statedata-client-cacert/client-cacert.pem";
        cmmValidatorClientCaPath = CERTIFICATES_PATH + "/validator-client-cacert/client-cacert.pem";
        config = new BroKeyStoreConfiguration(
                password,
                keyStorePath,
                broPrivateKeyPath,
                broServerCertPath,
                cmypClientCaPath,
                broCaPath,
                cmmActionsClientCaPath,
                cmmStateClientCaPath,
                cmmValidatorClientCaPath
        );
        assertTrue(config.isValidConfiguration());
    }
}