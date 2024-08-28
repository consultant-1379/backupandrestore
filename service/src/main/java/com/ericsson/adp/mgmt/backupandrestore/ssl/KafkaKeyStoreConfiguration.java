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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.ericsson.adp.mgmt.backupandrestore.persist.FileService;
import com.ericsson.adp.mgmt.backupandrestore.util.TLSCertUtils;
import com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils;

import java.nio.file.Path;
import java.util.HashSet;
import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.KAFKA;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SIP_TLS_ROOT_CERT_ALIAS;

/**
 * Class to configure the kafka key/trust store
 * */
@Configuration
public class KafkaKeyStoreConfiguration extends KeyStoreConfiguration {

    /**
     * Initialize a keystore config to configure the keystore used to push message to kafka
     * @param password - keystore password
     * @param keyStorePath - keystore path
     * @param _privateKeyPath - path to private kafka client key
     * @param _certPath - path to kafka client certificate, signed by kafka client ca
     * @param _rootCaPath - path to siptls root certificate authority, to verify kafka server cert
     * */
    public KafkaKeyStoreConfiguration(
            @Value("${security.kafka.keystore.path:}") final String keyStorePath,
            @Value("${security.kafka.keystore.password:}") final String password,
            @Value("${security.kafka.client.key.path:}") final String _privateKeyPath,
            @Value("${security.kafka.client.cert.path:}") final String _certPath,
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
            if (FileService.isPathValidReadable(privateKeyPath)
                    && FileService.isPathValidReadable(certPath)) {
                cert = new CertificateKeyEntry(getAlias().toString(), privateKeyPath, certPath);
                certDefinedInValues = new CertificateKeyEntry(getAlias().toString(), privateKeyPath, certPath);
                if (FileService.isPathValidReadable(rootCaPath)) {
                    authorities.add(new CertificateCA(SIP_TLS_ROOT_CERT_ALIAS, rootCaPath));
                }
                setValidConfiguration(true);
            } else if (FileService.isPathValid(privateKeyPath) &&
                       FileService.isPathValid(certPath) &&
                       FileService.isPathValid(rootCaPath)) {
                cert = createCertificateKeyEntry(privateKeyPath, certPath);
                certDefinedInValues = createCertificateKeyEntry(privateKeyPath, certPath);
                authorities.add(new CertificateCA(ApplicationConstantsUtils.SIP_TLS_ROOT_CERT_ALIAS, rootCaPath));
            }
        }
        setAuthoritiesDefinedInValues();
    }

    @Override
    public KeyStoreAlias getAlias() {
        return KAFKA;
    }

    private CertificateKeyEntry createCertificateKeyEntry(final String privateKeyPath, final String serverCertPath) {
        final CertificateKeyEntry cert = new CertificateKeyEntry(privateKeyPath);
        cert.setAlias(getAlias().toString());
        cert.setCertificatePath(serverCertPath);
        return cert;
    }
}
