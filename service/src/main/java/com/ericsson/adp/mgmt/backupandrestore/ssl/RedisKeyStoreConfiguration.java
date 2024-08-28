/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.ssl;

import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.REDIS;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SIP_TLS_ROOT_CERT_ALIAS;


import com.ericsson.adp.mgmt.backupandrestore.persist.FileService;
import com.ericsson.adp.mgmt.backupandrestore.util.TLSCertUtils;
import com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.HashSet;

/**
 * Class to configure the Redis key/trust store
 */
@Configuration
public class RedisKeyStoreConfiguration extends KeyStoreConfiguration {

    /**
     * Initialize a keystore config to configure the keystore used to push message to Redis
     *
     * @param password            - keystore password
     * @param keyStorePath        - keystore path
     * @param _redisPrivateKeyPath - path to private redis client key
     * @param _certPath            - path to redis client certificate, signed by redis client ca
     * @param _rootCaPath          - path to siptls root certificate authority, to verify redis server cert
     */
    public RedisKeyStoreConfiguration(
            @Value("${security.redis.keystore.path:}") final String keyStorePath,
            @Value("${security.redis.keystore.password:}") final String password,
            @Value("${security.redis.client.key.path:}") final String _redisPrivateKeyPath,
            @Value("${security.redis.client.cert.path:}") final String _certPath,
            @Value("${security.siptls.root.ca.path:}") final String _rootCaPath
    ) {
        authorities = new HashSet<>();
        // pick tls.crt, tls.key or ca.crt if exists
        final String redisPrivateKeyPath = TLSCertUtils.checkCertsInPathAndTakePreference(_redisPrivateKeyPath, "priv");
        final String certPath = TLSCertUtils.checkCertsInPathAndTakePreference(_certPath, "cert");
        final String rootCaPath = TLSCertUtils.checkCertsInPathAndTakePreference(_rootCaPath, "ca");

        // load cert, key and ca paths into KeyStoreConfiguration.
        // if validReadable path, generate keyStores
        // otherwise just record the paths
        if (FileService.isPathValid(keyStorePath)) {
            keyStoreFile = Path.of(keyStorePath);
            keystorePassword = password;
            if (FileService.isPathValidReadable(redisPrivateKeyPath)
                    && FileService.isPathValidReadable(certPath)) {
                cert = new CertificateKeyEntry(getAlias().toString(), redisPrivateKeyPath, certPath);
                certDefinedInValues = new CertificateKeyEntry(getAlias().toString(), redisPrivateKeyPath, certPath);
                if (FileService.isPathValidReadable(rootCaPath)) {
                    authorities.add(new CertificateCA(SIP_TLS_ROOT_CERT_ALIAS, rootCaPath));
                }
                setValidConfiguration(true);
            } else if (FileService.isPathValid(redisPrivateKeyPath) &&
                       FileService.isPathValid(certPath) &&
                       FileService.isPathValid(rootCaPath)) {
                cert = createCertificateKeyEntry(redisPrivateKeyPath, certPath);
                certDefinedInValues = createCertificateKeyEntry(redisPrivateKeyPath, certPath);
                authorities.add(new CertificateCA(ApplicationConstantsUtils.SIP_TLS_ROOT_CERT_ALIAS, rootCaPath));
            }
        }
        setAuthoritiesDefinedInValues();
    }

    /**
     * Get the keystore config alias
     *
     * @return the keystore config alias
     */
    @Override
    public KeyStoreAlias getAlias() {
        return REDIS;
    }

    private CertificateKeyEntry createCertificateKeyEntry(final String privateKeyPath, final String serverCertPath) {
        final CertificateKeyEntry cert = new CertificateKeyEntry(privateKeyPath);
        cert.setAlias(getAlias().toString());
        cert.setCertificatePath(serverCertPath);
        return cert;
    }
}
