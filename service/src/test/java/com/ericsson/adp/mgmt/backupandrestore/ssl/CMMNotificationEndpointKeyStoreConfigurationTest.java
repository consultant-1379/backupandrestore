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

public class CMMNotificationEndpointKeyStoreConfigurationTest {
    private static final String CERTIFICATES_PATH = "src/test/resources/";

    private CMMNotificationEndpointKeyStoreConfiguration config;
    @Value("${security.cmm.notifications.keystore.password:}")
    private String password = "";
    @Value("${security.cmm.notifications.keystore.path:}")
    private String keyStorePath = "";
    @Value("${security.bro.server.key.path:}")
    private String broPrivateKeyPath = "";
    @Value("${security.bro.server.cert.path:}")
    private String broServerCertPath = "";
    @Value("${security.cmm.notifications.client.ca.path:}")
    private String cmmNotificationsClientCaPath = "";

    @Test
    public void CMMNotificationEndpoint_generateKeyStore_emptyPath_invalidConfiguration() {
        config = new CMMNotificationEndpointKeyStoreConfiguration(
                password,
                keyStorePath,
                broPrivateKeyPath,
                broServerCertPath,
                cmmNotificationsClientCaPath
        );
        assertFalse(config.isValidConfiguration());
    }

    @Test
    public void CMMNotificationEndpoint_generateKeyStore_invalidConfiguration() throws Exception {
        keyStorePath = CERTIFICATES_PATH + "kafkakeystore.p12";
        password = "changeit";
        broPrivateKeyPath = CERTIFICATES_PATH + "invalid/key.key";
        broServerCertPath = CERTIFICATES_PATH + "clientcert.pem";
        config = new CMMNotificationEndpointKeyStoreConfiguration(
                password,
                keyStorePath,
                broPrivateKeyPath,
                broServerCertPath,
                cmmNotificationsClientCaPath
        );
        assertFalse(config.isValidConfiguration());
    }

    @Test
    public void CMMNotificationEndpoint_generateKeyStore_invalidPath() throws Exception {
        keyStorePath = CERTIFICATES_PATH + "kafkakeystore.p12";
        password = "changeit";
        broPrivateKeyPath = "c:/invalidPath.key";
        broServerCertPath = CERTIFICATES_PATH + "clientcert.pem";
        config = new CMMNotificationEndpointKeyStoreConfiguration(
                password,
                keyStorePath,
                broPrivateKeyPath,
                broServerCertPath,
                cmmNotificationsClientCaPath
        );
        assertFalse(config.isValidConfiguration());
    }

    @Test
    public void CMMNotificationEndpoint_generateKeyStore_validConfiguration_withLegacyCA() throws Exception {
        keyStorePath = CERTIFICATES_PATH + "kafkakeystore.p12";
        password = "changeit";
        broPrivateKeyPath = CERTIFICATES_PATH + "clientprivkey.key";
        broServerCertPath = CERTIFICATES_PATH + "clientcert.pem";
        cmmNotificationsClientCaPath = CERTIFICATES_PATH + "/cmmserver/ca/client-cacertbundle.pem";
        config = new CMMNotificationEndpointKeyStoreConfiguration(
                password,
                keyStorePath,
                broPrivateKeyPath,
                broServerCertPath,
                cmmNotificationsClientCaPath
        );
        assertTrue(config.isValidConfiguration());
    }

    @Test
    public void CMMNotificationEndpoint_generateKeyStore_validConfiguration_withCmeiaCA() throws Exception {
        keyStorePath = CERTIFICATES_PATH + "kafkakeystore.p12";
        password = "changeit";
        broPrivateKeyPath = CERTIFICATES_PATH + "clientprivkey.key";
        broServerCertPath = CERTIFICATES_PATH + "clientcert.pem";
        cmmNotificationsClientCaPath = CERTIFICATES_PATH + "/cmmserver/ca/client-cacertbundle.pem";
        config = new CMMNotificationEndpointKeyStoreConfiguration(
                password,
                keyStorePath,
                broPrivateKeyPath,
                broServerCertPath,
                cmmNotificationsClientCaPath
        );
        assertTrue(config.isValidConfiguration());
    }
}
