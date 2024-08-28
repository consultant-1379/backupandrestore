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

import com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils;
import com.ericsson.adp.mgmt.backupandrestore.util.TLSCertUtils;
import com.ericsson.adp.mgmt.backupandrestore.persist.FileService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import java.nio.file.Path;
import java.util.HashSet;

/**
 * Class to configure the keystore used for the BRO PM
 * endpoint. This keystore contains the BRO server cert/key
 * pair, so the PM microservice can validate the BRO management
 * endpoint using the SIPTLS root CA, and the PM client
 * CA, so that the BRO management endpoint can validate the
 * client cert sent by the PM service when using mTLS
 * */
@Configuration
public class PMKeyStoreConfiguration extends KeyStoreConfiguration {

    /**
     * Initialize a keystore config to configure the PM keystore
     * @param password - keystore password
     * @param keyStorePath - path where the keystore file will be stored
     * @param _broServerKeyPath - path to BRO server private key file
     * @param _broServerCertPath - path to BRO server cert file
     * @param _pmClientAuthorityPath - path to pm client CA file
     * */
    public PMKeyStoreConfiguration(
            @Value("${security.pm.keystore.password:}") final String password,
            @Value("${security.pm.keystore.path:}") final String keyStorePath,
            @Value("${security.bro.server.key.path:}") final String _broServerKeyPath,
            @Value("${security.bro.server.cert.path:}") final String _broServerCertPath,
            @Value("${security.pm.client.ca.path:}") final String _pmClientAuthorityPath
    ) {
        authorities = new HashSet<>();
        // pick tls.crt, tls.key or ca.crt if exists
        final String broServerKeyPath = TLSCertUtils.checkCertsInPathAndTakePreference(_broServerKeyPath, "priv");
        final String broServerCertPath = TLSCertUtils.checkCertsInPathAndTakePreference(_broServerCertPath, "cert");
        final String pmClientAuthorityPath = TLSCertUtils.checkCertsInPathAndTakePreference(_pmClientAuthorityPath, "ca");
        // load cert, key and ca paths into KeyStoreConfiguration.
        // if validReadable path, generate keyStores
        // otherwise just record the paths
        if (FileService.isPathValid(keyStorePath)) {
            this.keyStoreFile = Path.of(keyStorePath);
            this.keystorePassword = password;
            if (FileService.isPathValidReadable(broServerKeyPath)
                    && FileService.isPathValidReadable(broServerCertPath) ) {
                cert = new CertificateKeyEntry(getAlias().toString(), broServerKeyPath, broServerCertPath);
                certDefinedInValues = new CertificateKeyEntry(getAlias().toString(), broServerKeyPath, broServerCertPath);
                setValidConfiguration(true);
            } else if (FileService.isPathValid(broServerKeyPath) &&
                       FileService.isPathValid(broServerCertPath)) {
                cert = createCertificateKeyEntry(broServerKeyPath, broServerCertPath);
                certDefinedInValues = createCertificateKeyEntry(broServerKeyPath, broServerCertPath);
            }
            if (FileService.isPathValid(pmClientAuthorityPath)) {
                authorities.add(new CertificateCA(ApplicationConstantsUtils.PM_CLIENT_CA_ALIAS, pmClientAuthorityPath));
            }
        }
        setAuthoritiesDefinedInValues();
    }

    @Override
    public KeyStoreAlias getAlias() {
        return KeyStoreAlias.PM;
    }

    private CertificateKeyEntry createCertificateKeyEntry(final String privateKeyPath, final String serverCertPath) {
        final CertificateKeyEntry cert = new CertificateKeyEntry(privateKeyPath);
        cert.setAlias(getAlias().toString());
        cert.setCertificatePath(serverCertPath);
        return cert;
    }
}
