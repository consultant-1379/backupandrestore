/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */
package com.ericsson.adp.mgmt.socket.appender.util;

import org.apache.logging.log4j.core.net.ssl.KeyStoreConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.StoreConfigurationException;
import org.apache.logging.log4j.core.net.ssl.TrustStoreConfiguration;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

/**
 * KeystoreConfig class
 */
public class KeyStoreHelper {

    private static final String KEYSTORE_TYPE = "PKCS12";
    /**
     * Default Constructor
     */
    private KeyStoreHelper() {
    }

    /**
     * Parses a private key from the specified path
     * @param privateKeyPath path of the private key
     * @return privateKey object
     */
    private static PrivateKey getPrivateKey(final Path privateKeyPath) throws KeyException, IOException {
        PrivateKey key = null;
        try (
                PEMParser parser = new PEMParser(new BufferedReader(new FileReader(privateKeyPath.toFile())));
        ) {
            final JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();
            final Object readObject = parser.readObject();
            if (readObject instanceof PEMKeyPair) {
                final PEMKeyPair pemKeyPair = ((PEMKeyPair) readObject);
                keyConverter.setProvider(BouncyCastleProvider.PROVIDER_NAME);
                key = keyConverter.getKeyPair(pemKeyPair).getPrivate();
            } else if (readObject instanceof PrivateKeyInfo) {
                final PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) readObject;
                key = keyConverter.getPrivateKey(privateKeyInfo);
            } else {
                throw new KeyException("Invalid Security Object Type");
            }
            return key;
        }
    }

    /**
     * Gets a certificate from a specified path
     * @param pathToCertificate path to certificate
     * @param instance instance for the certificate factory eg X509
     * @return the Certificate
     */
    private static Certificate getCertificateFromPath(final Path pathToCertificate, final String instance) throws IOException, CertificateException {
        Certificate cert = null;
        CertificateFactory certificateFactory;
        try (BufferedInputStream certStream = new BufferedInputStream(new FileInputStream(pathToCertificate.toFile()))) {
            certificateFactory = CertificateFactory.getInstance(instance);
            cert = certificateFactory.generateCertificate(certStream);
        }
        return cert;
    }

    /**
     *Populates the keystore with the given parameters
     * @param streamSslConfiguration ssl configuration
     * @return StreamSslConfiguration used to build the socket
     * @throws KeyStoreException keyStore Exception
     * @throws CertificateException certificate Exeception
     * @throws NoSuchAlgorithmException nsa Execption
     * @throws IOException io Exception
     * @throws StoreConfigurationException storeConfiguration Exception
     * @throws KeyException key Exception
     */
    public static SslConfiguration populateKeystore(final StreamSslConfiguration streamSslConfiguration)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, StoreConfigurationException, KeyException {
        final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        keyStore.load(null);
        final Certificate[] chain = {getCertificateFromPath(streamSslConfiguration.getCertLocation(), "X.509")};
        final PrivateKey privateKey = getPrivateKey(streamSslConfiguration.getKeyLocation());
        keyStore.setKeyEntry(streamSslConfiguration.getCertAlias(), privateKey, streamSslConfiguration.getKeystorePW().toCharArray(), chain);
        keyStore.setCertificateEntry(streamSslConfiguration.getCaAlias(), getCertificateFromPath(streamSslConfiguration.getCaLocation(), "X.509"));
        Files.createDirectories(streamSslConfiguration.getKeystoreLocation().getParent());
        final File file = new File(streamSslConfiguration.getKeystoreLocation().toString());
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            keyStore.store(fileOutputStream, streamSslConfiguration.getKeystorePW().toCharArray());
            final KeyStoreConfiguration keyStoreConfiguration = KeyStoreConfiguration.createKeyStoreConfiguration(
                    file.getCanonicalPath(),
                    streamSslConfiguration.getKeystorePW().toCharArray(),
                    null,
                    null,
                    KEYSTORE_TYPE,
                    null);
            final TrustStoreConfiguration trustStoreConfiguration = TrustStoreConfiguration.createKeyStoreConfiguration(
                    file.getCanonicalPath(),
                    streamSslConfiguration.getKeystorePW().toCharArray(),
                    null,
                    null,
                    KEYSTORE_TYPE,
                    null);
            return SslConfiguration.createSSLConfiguration("TLS", keyStoreConfiguration, trustStoreConfiguration);
        }
    }
}