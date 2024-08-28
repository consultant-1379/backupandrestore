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
 * Class to configure the CMM rest interface key/trust store. Contains
 * the siptls root ca, to allow us to verify the CMM rest endpoint server
 * cert, and the cmm rest endpoint client cert/key pair, to allow us to
 * authenticate with the cmm rest endpoint.
 * */
@Configuration
public class CMMRestKeyStoreConfiguration extends KeyStoreConfiguration {

    /**
     * Initialize a keystore config to configure the keystore used to make requests to CMM
     * @param password - keystore password
     * @param keyStorePath - keystore path
     * @param _cmmPrivateKeyPath - path to private client key
     * @param _cmmCertPath - path to client certificate, signed by CMM client ca
     * @param _siptlsCaPath - path to siptls root certificate authority, to verify CMM server cert
     * */
    public CMMRestKeyStoreConfiguration(
        @Value("${security.cmm.keystore.password:}") final String password,
        @Value("${security.cmm.keystore.path:}") final String keyStorePath,
        @Value("${security.cmm.client.key.path:}") final String _cmmPrivateKeyPath,
        @Value("${security.cmm.client.cert.path:}") final String _cmmCertPath,
        @Value("${security.siptls.root.ca.path:}") final String _siptlsCaPath
    ) {
        authorities = new HashSet<>();
        // pick tls.crt, tls.key or ca.crt if exists
        final String cmmPrivateKeyPath = TLSCertUtils.checkCertsInPathAndTakePreference(_cmmPrivateKeyPath, "priv");
        final String cmmCertPath = TLSCertUtils.checkCertsInPathAndTakePreference(_cmmCertPath, "cert");
        final String siptlsCaPath = TLSCertUtils.checkCertsInPathAndTakePreference(_siptlsCaPath, "ca");

        // load cert, key and ca paths into KeyStoreConfiguration.
        // if validReadable path, generate keyStores
        // otherwise just record the paths
        if (FileService.isPathValid(keyStorePath)) {
            keystorePassword = password;
            keyStoreFile = Path.of(keyStorePath);
            if (FileService.isPathValidReadable(cmmPrivateKeyPath)
                    && FileService.isPathValidReadable(cmmCertPath)) {
                cert = new CertificateKeyEntry(getAlias().toString(), cmmPrivateKeyPath, cmmCertPath);
                certDefinedInValues = new CertificateKeyEntry(getAlias().toString(), cmmPrivateKeyPath, cmmCertPath);
                if (FileService.isPathValidReadable(siptlsCaPath)) {
                    authorities.add(new CertificateCA(ApplicationConstantsUtils.SIP_TLS_ROOT_CERT_ALIAS,
                            siptlsCaPath));
                }
                setValidConfiguration(true);
            } else if (FileService.isPathValid(cmmPrivateKeyPath) &&
                       FileService.isPathValid(cmmCertPath) &&
                       FileService.isPathValid(siptlsCaPath)) {
                cert = createCertificateKeyEntry(cmmPrivateKeyPath, cmmCertPath);
                certDefinedInValues = createCertificateKeyEntry(cmmPrivateKeyPath, cmmCertPath);
                authorities.add(new CertificateCA(ApplicationConstantsUtils.SIP_TLS_ROOT_CERT_ALIAS,
                                siptlsCaPath));
            }
        }
        setAuthoritiesDefinedInValues();
    }

    @Override
    public KeyStoreAlias getAlias() {
        return KeyStoreAlias.CMM_REST;
    }

    private CertificateKeyEntry createCertificateKeyEntry(final String privateKeyPath, final String serverCertPath) {
        final CertificateKeyEntry cert = new CertificateKeyEntry(privateKeyPath);
        cert.setAlias(getAlias().toString());
        cert.setCertificatePath(serverCertPath);
        return cert;
    }
}
