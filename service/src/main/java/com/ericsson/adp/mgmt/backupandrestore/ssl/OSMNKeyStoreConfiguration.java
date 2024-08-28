/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.ssl;

import com.ericsson.adp.mgmt.backupandrestore.exception.KeyStoreGenerationException;
import com.ericsson.adp.mgmt.backupandrestore.persist.FileService;
import com.ericsson.adp.mgmt.backupandrestore.util.TLSCertUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.HashSet;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SIP_TLS_ROOT_CERT_ALIAS;

/**
 * Keystore configuration for OSMN communication
 *
 * */
@Configuration
public class OSMNKeyStoreConfiguration extends KeyStoreConfiguration {
    private static final Logger log = LogManager.getLogger(KeyStoreService.class);

    /**
     * Initialize a keystore config to configure the keystore used to communicate with OSMN
     * @param password - keystore password
     * @param keyStorePath - keystore path
     * @param _privateKeyPath - path to private osmn client key, if one exists
     * @param _certPath - path to osmn client certificate, signed by osmn client CA, if one exists
     * @param _rootCaPath - path to siptls root certificate authority, to verify osmn server cert
     * */
    public OSMNKeyStoreConfiguration(
            @Value("${security.osmn.keystore.path:}") final String keyStorePath,
            @Value("${security.osmn.keystore.password:}") final String password,
            @Value("${security.osmn.client.key.path:}") final String _privateKeyPath,
            @Value("${security.osmn.client.cert.path:}") final String _certPath,
            @Value("${security.siptls.root.ca.path:}") final String _rootCaPath
    ) {
        authorities = new HashSet<>();
        // pick tls.crt, tls.key or ca.crt if exists
        final String privateKeyPath = TLSCertUtils.checkCertsInPathAndTakePreference(_privateKeyPath, "priv");
        final String certPath = TLSCertUtils.checkCertsInPathAndTakePreference(_certPath, "cert");
        final String rootCaPath = TLSCertUtils.checkCertsInPathAndTakePreference(_rootCaPath, "ca");

        // load cert, key and ca paths into KeyStoreConfiguration.
        // if validReadable path, generate keyStores
        // otherwise just record the paths
        if (FileService.isPathValid(keyStorePath)) {
            keyStoreFile = Path.of(keyStorePath);
            keystorePassword = password;
            if (!FileService.isPathValidReadable(rootCaPath)) { // Assert root CA is present, only mandatory field
                final String message = "Invalid security configuration of osmn keystore - missing CA";
                throw new KeyStoreGenerationException(message);
            }
            if (FileService.isPathValidReadable(privateKeyPath) && FileService.isPathValidReadable(certPath)) {
                cert = new CertificateKeyEntry(getAlias().toString(), privateKeyPath, certPath);
                certDefinedInValues = new CertificateKeyEntry(getAlias().toString(), privateKeyPath, certPath);
                log.info("OSMN interface configured for mTLS");
                setValidConfiguration(true);
            } else if (FileService.isPathValid(privateKeyPath) &&
                       FileService.isPathValid(certPath)) {
                log.info("OSMN interface configured for TLS");
                cert = createCertificateKeyEntry(privateKeyPath, certPath);
                certDefinedInValues = createCertificateKeyEntry(privateKeyPath, certPath);
            }
            if (FileService.isPathValid(rootCaPath)) {
                authorities.add(new CertificateCA(SIP_TLS_ROOT_CERT_ALIAS, rootCaPath));
            }
        }
        setAuthoritiesDefinedInValues();
    }

    @Override
    public KeyStoreAlias getAlias() {
        return KeyStoreAlias.OSMN;
    }

    private CertificateKeyEntry createCertificateKeyEntry(final String privateKeyPath, final String serverCertPath) {
        final CertificateKeyEntry cert = new CertificateKeyEntry(privateKeyPath);
        cert.setAlias(getAlias().toString());
        cert.setCertificatePath(serverCertPath);
        return cert;
    }
}
