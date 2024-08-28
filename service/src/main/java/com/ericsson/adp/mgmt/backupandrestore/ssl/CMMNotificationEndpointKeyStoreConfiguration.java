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
package com.ericsson.adp.mgmt.backupandrestore.ssl;

import com.ericsson.adp.mgmt.backupandrestore.persist.FileService;
import com.ericsson.adp.mgmt.backupandrestore.util.TLSCertUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.HashSet;

import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.CMM_NOTIF;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.CMM_CLIENT_CA_ALIAS;

/**
 * Keystore Configuration for the tomcat connector that acts as a server for CMM notifications.
 * Contains the BRO server cert and key, so CMM can verify BRO, and the CMM client CA, so BRO can
 * verify CMM client.
 * */
@Configuration
public class CMMNotificationEndpointKeyStoreConfiguration extends KeyStoreConfiguration {

    /**
     * Initialize a keystore config to configure the keystore used by the cmm notifications tomcat endpoint
     * @param password - keystore password
     * @param keyStorePath - keystore path
     * @param _broPrivateKeyPath - path to private server key
     * @param _broServerCertPath - path to BRO server certificate, signed by SIPTLS root ca
     * @param _cmmNotificationsClientCaPath - path to CMM        client certificate authority
     * */
    public CMMNotificationEndpointKeyStoreConfiguration(
            @Value("${security.cmm.notifications.keystore.password:}") final String password,
            @Value("${security.cmm.notifications.keystore.path:}") final String keyStorePath,
            @Value("${security.bro.server.key.path:}") final String _broPrivateKeyPath,
            @Value("${security.bro.server.cert.path:}") final String _broServerCertPath,
            @Value("${security.cmm.notifications.client.ca.path:}") final String _cmmNotificationsClientCaPath
    ) {
        authorities = new HashSet<>();
        // pick tls.crt, tls.key or ca.crt if exists
        final String broPrivateKeyPath = TLSCertUtils.checkCertsInPathAndTakePreference(_broPrivateKeyPath, "priv");
        final String broServerCertPath = TLSCertUtils.checkCertsInPathAndTakePreference(_broServerCertPath, "cert");
        final String cmmNotificationsClientCaPath = TLSCertUtils.checkCertsInPathAndTakePreference(_cmmNotificationsClientCaPath, "ca");
        // load cert, key and ca paths into KeyStoreConfiguration.
        // if validReadable path, generate keyStores
        // otherwise just record the paths
        if (FileService.isPathValid(keyStorePath)) {
            keyStoreFile = Path.of(keyStorePath);
            keystorePassword = password;
            if (FileService.isPathValidReadable(broPrivateKeyPath)
                    && FileService.isPathValidReadable(broServerCertPath) ) {
                cert = new CertificateKeyEntry(getAlias().toString(), broPrivateKeyPath, broServerCertPath);
                certDefinedInValues = new CertificateKeyEntry(getAlias().toString(), broPrivateKeyPath, broServerCertPath);
            } else if (FileService.isPathValid(broPrivateKeyPath) &&
                       FileService.isPathValid(broServerCertPath)) {
                cert = createCertificateKeyEntry(broPrivateKeyPath, broServerCertPath);
                certDefinedInValues = createCertificateKeyEntry(broPrivateKeyPath, broServerCertPath);
            }
            if (FileService.isPathValidReadable(cmmNotificationsClientCaPath)) {
                authorities.add(new CertificateCA(CMM_CLIENT_CA_ALIAS, cmmNotificationsClientCaPath));
                setValidConfiguration(true);
            } else if (FileService.isPathValid(cmmNotificationsClientCaPath)) {
                authorities.add(new CertificateCA(CMM_CLIENT_CA_ALIAS, cmmNotificationsClientCaPath));
            }
        }
        setAuthoritiesDefinedInValues();
    }

    @Override
    public KeyStoreAlias getAlias() {
        return CMM_NOTIF;
    }

    private CertificateKeyEntry createCertificateKeyEntry(final String privateKeyPath, final String serverCertPath) {
        final CertificateKeyEntry cert = new CertificateKeyEntry(privateKeyPath);
        cert.setAlias(getAlias().toString());
        cert.setCertificatePath(serverCertPath);
        return cert;
    }
}
