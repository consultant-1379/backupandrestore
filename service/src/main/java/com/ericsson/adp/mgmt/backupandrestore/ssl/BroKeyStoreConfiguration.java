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
 * Class to configure the BRO key/trust store, containing the BRO CA and
 * BRO server cert/key pair. This allows REST TLS with client that have
 * the SIPTLS root CA in their trust store, and mTLS with clients that have
 * the SIPTLS root CA in their trust store and a cert/key pair signed by the
 * BRO CA. It also contains the CMM client CA and the CMYP client CA, as the
 * same tomcat context is used to receive requests from cmm notifications and
 * cmyp
 * */
@Configuration
public class BroKeyStoreConfiguration extends KeyStoreConfiguration {

    /**
     * Initialize a keystore config to configure the keystore used by tomcat
     * @param password - keystore password
     * @param keyStorePath - keystore path
     * @param _broPrivateKeyPath - path to private server key
     * @param _broServerCertPath - path to BRO server certificate, signed by SIPTLS root ca
     * @param _cmypClientCaPath - path to CMYP client certificate authority
     * @param _broCaPath - path to BRO client certificate authority
     * @param _cmmActionsClientCaPath - path to CMM              client certificate authority for actions
     * @param _cmmStateClientCaPath - path to CMM        client certificate authority for state
     * @param _cmmValidatorClientCaPath - path to CMM        client certificate authority for Validator
     * */
    public BroKeyStoreConfiguration(
            @Value("${security.bro.keystore.password:}") final String password,
            @Value("${security.bro.keystore.path:}") final String keyStorePath,
            @Value("${security.bro.server.key.path:}") final String _broPrivateKeyPath,
            @Value("${security.bro.server.cert.path:}") final String _broServerCertPath,
            @Value("${security.cmyp.client.ca.path:}") final String _cmypClientCaPath,
            @Value("${security.bro.ca.path:}") final String _broCaPath,
            @Value("${security.cmm.client.ca.path.action:}") final String _cmmActionsClientCaPath,
            @Value("${security.cmm.client.ca.path.state:}") final String _cmmStateClientCaPath,
            @Value("${security.cmm.client.ca.path.validator:}") final String _cmmValidatorClientCaPath
    ) {
        authorities = new HashSet<>();
        // pick tls.crt, tls.key or ca.crt if exists
        final String broPrivateKeyPath = TLSCertUtils.checkCertsInPathAndTakePreference(_broPrivateKeyPath, "priv");
        final String broServerCertPath = TLSCertUtils.checkCertsInPathAndTakePreference(_broServerCertPath, "cert");
        final String cmypClientCaPath = TLSCertUtils.checkCertsInPathAndTakePreference(_cmypClientCaPath, "ca");
        final String broCaPath = TLSCertUtils.checkCertsInPathAndTakePreference(_broCaPath, "ca");
        final String cmmActionsClientCaPath = TLSCertUtils.checkCertsInPathAndTakePreference(_cmmActionsClientCaPath, "ca");
        final String cmmStateClientCaPath = TLSCertUtils.checkCertsInPathAndTakePreference(_cmmStateClientCaPath, "ca");
        final String cmmValidatorClientCaPath = TLSCertUtils.checkCertsInPathAndTakePreference(_cmmValidatorClientCaPath, "ca");

        if (FileService.isPathValid(keyStorePath)) {
            keyStoreFile = Path.of(keyStorePath);
            keystorePassword = password;

            loadBroServerCertsAndCertificateAuthorities(broPrivateKeyPath, broServerCertPath, broCaPath);
            loadCmmCertificateAuthorities(cmypClientCaPath, cmmActionsClientCaPath, cmmStateClientCaPath, cmmValidatorClientCaPath);
            setAuthoritiesDefinedInValues();
        }
    }

    // load cert, key and ca paths into KeyStoreConfiguration.
    // if validReadable path, generate keyStores
    // otherwise just record the paths
    private void loadBroServerCertsAndCertificateAuthorities(final String broPrivateKeyPath, final String broServerCertPath, final String broCaPath) {
        if (FileService.isPathValidReadable(broPrivateKeyPath)
                && FileService.isPathValidReadable(broServerCertPath) ) {
            cert = new CertificateKeyEntry(getAlias().toString(), broPrivateKeyPath, broServerCertPath);
            certDefinedInValues = new CertificateKeyEntry(getAlias().toString(), broPrivateKeyPath, broServerCertPath);
            setValidConfiguration(true);
        } else if (FileService.isPathValid(broPrivateKeyPath) &&
                   FileService.isPathValid(broServerCertPath)) {
            cert = createCertificateKeyEntry(broPrivateKeyPath, broServerCertPath);
            certDefinedInValues = createCertificateKeyEntry(broPrivateKeyPath, broServerCertPath);
        }
        if (FileService.isPathValid(broCaPath)) {
            authorities.add(new CertificateCA(ApplicationConstantsUtils.BRO_CA_ALIAS, broCaPath));
        }
    }

    private CertificateKeyEntry createCertificateKeyEntry(final String privateKeyPath, final String serverCertPath) {
        final CertificateKeyEntry cert = new CertificateKeyEntry(privateKeyPath);
        cert.setAlias(getAlias().toString());
        cert.setCertificatePath(serverCertPath);
        return cert;
    }

    private void loadCmmCertificateAuthorities(final String cmypClientCaPath, final String cmmActionsClientCaPath,
            final String cmmStateClientCaPath, final String cmmValidatorClientCaPath) {
        if (isLegacyCA(cmypClientCaPath)) {
            if (FileService.isPathValid(cmypClientCaPath)) {
                authorities.add(new CertificateCA(ApplicationConstantsUtils.CMYP_CLIENT_CA_ALIAS, cmypClientCaPath));
            }
        } else {
            if (FileService.isPathValidReadable(cmmActionsClientCaPath)
                    && FileService.isPathValidReadable(cmmStateClientCaPath)
                    && FileService.isPathValidReadable(cmmValidatorClientCaPath)) {
                setValidConfiguration(true);
            }
            if (FileService.isPathValid(cmmActionsClientCaPath) &&
                FileService.isPathValid(cmmStateClientCaPath) &&
                FileService.isPathValid(cmmValidatorClientCaPath)) {
                authorities.add(new CertificateCA(ApplicationConstantsUtils.CMM_CLIENT_ACTION_CA_ALIAS, cmmActionsClientCaPath));
                authorities.add(new CertificateCA(ApplicationConstantsUtils.CMM_CLIENT_STATE_CA_ALIAS, cmmStateClientCaPath));
                authorities.add(new CertificateCA(ApplicationConstantsUtils.CMM_CLIENT_VALIDATOR_CA_ALIAS, cmmValidatorClientCaPath));
            }
        }
    }

    private boolean isLegacyCA(final String legacyPath) {
        return !(legacyPath.contains("NOT_USED"));
    }

    @Override
    public KeyStoreAlias getAlias() {
        return KeyStoreAlias.BRO;
    }
}
