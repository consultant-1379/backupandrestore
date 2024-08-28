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

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import com.ericsson.adp.mgmt.backupandrestore.exception.KeyStoreGenerationException;

/**
 * Certificates to be included into the keystore
 * Assigns the given key (that has already been protected) to the given alias.
 */
public class CertificateKeyEntry extends CertificatesCommon{
    private static final Logger log = LogManager.getLogger(CertificateKeyEntry.class);

    private final Optional<String> privateKeyPath;
    private List<Certificate> listCertificates;
    private Optional<PrivateKey> privateKey;

    private long certKnownModified;
    private long keyKnownModified;

    /**
     * @param alias alias name used as key
     * @param privateKeyPath the key (in protected format) to be associated with the alias
     * @param certificatePath  the certificate chain for the corresponding public key
     */
    public CertificateKeyEntry(final String alias, final String privateKeyPath, final String certificatePath) {
        super();
        this.privateKeyPath = Optional.of(privateKeyPath);
        setCertificatePath(certificatePath);
        setAlias(alias);
        if (isValid()) {
            listCertificates = getCertificates();
            // Cache the modified time of the certificate file at read time, so we can check if it's been
            // changed since then when the cert list is retrieved using getListCertificates and refresh it if it has
            certKnownModified = Paths.get(certificatePath).toFile().lastModified();
            privateKey = Optional.of(getPrivateKey(this.privateKeyPath.get()));
        } else {
            log.warn("Invalid certificate entered {}", alias);
            listCertificates = Collections.emptyList();
            privateKey = Optional.empty();
        }
    }

    /**
     * @param privateKeyPath the key (in protected format) to be associated with the alias
     */
    public CertificateKeyEntry(final String privateKeyPath) {
        super();
        this.privateKeyPath = Optional.ofNullable(privateKeyPath);
    }

    /**
     * @return the privateKeyPath
     */
    protected Optional<String> getPrivateKeyPath() {
        return privateKeyPath;
    }

    private PrivateKey getPrivateKey(final String privateKeyPath) {
        PrivateKey key;
        try (
                PEMParser parser = new PEMParser(new BufferedReader(new FileReader(privateKeyPath)));

        ) {
            final JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();
            final Object data = parser.readObject();
            if (data instanceof PEMKeyPair) {
                final PEMKeyPair pemKeyPair = ((PEMKeyPair) data);
                keyConverter.setProvider(BouncyCastleProvider.PROVIDER_NAME);
                key = keyConverter.getKeyPair(pemKeyPair).getPrivate();
            } else if (data instanceof PrivateKeyInfo) {
                final PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) data;
                key = keyConverter.getPrivateKey(privateKeyInfo);
            } else {
                throw new KeyStoreGenerationException("Error while retrieving the private key. Illegal format " + parser.getClass().getSimpleName());
            }
            // Cache the modified timestamp of the key file at read time, so we can check if it's been changed since we
            // last read it when getPrivateKey() is called, and refresh the data in memory if it has been
            keyKnownModified = Paths.get(privateKeyPath).toFile().lastModified();
            return key;
        } catch (Exception e) {
            throw new KeyStoreGenerationException("Error while retrieving the private key.", e);
        }
    }

    /**
     * @return the listCertificates
     */
    public List<Certificate> getListCertificates() {
        final Optional<String> certificatePath = getCertificatePath();
        if (certificatePath.isPresent()) {
            //Get the modified timestamp of the certificate file on disk
            final long currentModified = Paths.get(certificatePath.get()).toFile().lastModified();
            if (certKnownModified != currentModified) {
                if (log.isDebugEnabled()) {
                    log.debug("Detected change in certificate at path {}, refreshing", certificatePath.get());
                }
                listCertificates = getCertificates();
            }
            // Cache the modified time of the certificate file at read time, so we can check if it's been
            // changed since then when the cert list is retrieved using getListCertificates and refresh it if it has
            certKnownModified = currentModified;
        }
        return listCertificates;
    }

    /**
     * @return the privateKey
     */
    public Optional<PrivateKey> getPrivateKey() {
        if (privateKeyPath.isPresent() && keyKnownModified != Paths.get(privateKeyPath.get()).toFile().lastModified()) {
            if (log.isDebugEnabled()) {
                log.debug("Detected change in private key at path {}, refreshing", privateKeyPath.get());
            }
            privateKey = Optional.of(getPrivateKey(privateKeyPath.get()));
        }
        return privateKey;
    }

    /**
     * Validates if the certificates includes a valid reference for the certificate
     * @return true if both paths are included otherwise false
     * @throws SecurityException on invalid certificate
     */
    public final boolean isValid() {
        final Optional<String> certificatePath = getCertificatePath();
        if (certificatePath.isPresent() && getPrivateKeyPath().isPresent()) {
            return Files.exists(Paths.get(certificatePath.get()));
        }
        return false;
    }
}
