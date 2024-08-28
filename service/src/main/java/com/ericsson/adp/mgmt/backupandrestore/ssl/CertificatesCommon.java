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
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;

import com.ericsson.adp.mgmt.backupandrestore.exception.KeyStoreGenerationException;

/**
 * Based class to retrieve certificates from path
 */
public abstract class CertificatesCommon {

    private Optional<String> certificatePath;
    private String alias;

    /**
     * Validate and get all the certificates in a file
     * @return the list of certificates X509 on path
     */
    public List<Certificate> getCertificates() {
        final List<Certificate> listCertificates = new ArrayList<>();
        if (certificatePath.isPresent()) {
            listCertificates.addAll(getCertificate(certificatePath.get()));
        }
        return listCertificates;
    }

    private  List<Certificate> getCertificate(final String certificatePath) {
        try (
                PEMParser parser = new PEMParser(new BufferedReader(new FileReader(certificatePath)))
        ) {
            final List<Certificate> certificates = new ArrayList<>();
            Object object;
            while ((object = parser.readObject()) != null) {
                if (object instanceof X509CertificateHolder) {
                    final Certificate cert = new JcaX509CertificateConverter()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .getCertificate((X509CertificateHolder) object);
                    certificates.add(cert);
                }
            }
            return certificates;
        } catch (Exception e) {
            throw new KeyStoreGenerationException("Error while retrieving the certificate.", e);
        }
    }

    /**
     * @return the alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @param alias the alias to set
     */
    public void setAlias(final String alias) {
        this.alias = alias;
    }

    /**
     * @param certificatePath the certificatePath to set
     */
    public void setCertificatePath(final String certificatePath) {
        this.certificatePath = Optional.of(certificatePath);
    }

    public Optional<String> getCertificatePath() {
        return certificatePath;
    }
}
