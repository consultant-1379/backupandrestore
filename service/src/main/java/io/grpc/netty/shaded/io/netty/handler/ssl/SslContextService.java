/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package io.grpc.netty.shaded.io.netty.handler.ssl;

import java.io.File;
import java.io.FileReader;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import com.ericsson.adp.mgmt.backupandrestore.exception.SslContextException;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;

/**
 * Responsible for creating DelegatingSslContext with
 * Server Certificate and Private key.
 */
public class SslContextService {
    private String certificateChainFilePath;
    private String privateKeyFilePath;
    private String certificateAuthorityFilePath;
    private String verifyClientCertificateEnforced;
    private DelegatingSslContext delegatingSslContext;

    /**
     * Retrieves the SslContext used by the GRPC Server.
     * @return DelegatingSslContext
     */
    public DelegatingSslContext getSslContext() {
        try {
            delegatingSslContext = new DelegatingSslContext(getSslContextBuilder());
            return delegatingSslContext;
        } catch (Exception e) {
            throw new SslContextException(e);
        }
    }

    /**
     * Calls the update of the DelegatingSslContext.
     */
    public void refreshContext() {
        try {
            if (delegatingSslContext != null) {
                delegatingSslContext.update(getSslContextBuilder());
            }
        } catch (Exception e) {
            throw new SslContextException(e);
        }
    }

    private SslContextBuilder getSslContextBuilder() {
        try {
            final X509Certificate[] cert = SslContext.toX509Certificates(new File(certificateChainFilePath));
            final SslContextBuilder sslClientContextBuilder = SslContextBuilder.forServer(getPrivateKey(), cert)
                    .trustManager(new File(certificateAuthorityFilePath));
            if ("required".equals(verifyClientCertificateEnforced)) {
                sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
            } else {
                sslClientContextBuilder.clientAuth(ClientAuth.OPTIONAL);
            }
            return GrpcSslContexts.configure(sslClientContextBuilder);
        } catch (Exception e) {
            throw new SslContextException(e);
        }
    }

    private PrivateKey getPrivateKey() {
        try (
                FileReader reader = new FileReader(privateKeyFilePath);
                PEMParser pem = new PEMParser(reader)
        ) {
            final JcaPEMKeyConverter jcaPEMKeyConverter = new JcaPEMKeyConverter();
            final Object file = pem.readObject();

            if (file instanceof PEMKeyPair) {
                final PEMKeyPair keyInfo = ((PEMKeyPair) file);
                return jcaPEMKeyConverter.getKeyPair(keyInfo).getPrivate();
            }
            final PrivateKeyInfo keyInfo = ((PrivateKeyInfo) file);
            return jcaPEMKeyConverter.getPrivateKey(keyInfo);
        } catch (Exception e) {
            throw new SslContextException(e);
        }
    }

    public void setCertificateChainFilePath(final String certificateChainFilePath) {
        this.certificateChainFilePath = certificateChainFilePath;
    }

    public void setPrivateKeyFilePath(final String privateKeyFilePath) {
        this.privateKeyFilePath = privateKeyFilePath;
    }

    public void setCertificateAuthorityFilePath(final String certificateAuthorityFilePath) {
        this.certificateAuthorityFilePath = certificateAuthorityFilePath;
    }

    /**
     * Sets whether clientAuth is required or optional
     * @param verifyClientCertificateEnforced clientAuth enforced/required
     */
    public void setVerifyClientCertificateEnforced(final String verifyClientCertificateEnforced) {
        this.verifyClientCertificateEnforced = verifyClientCertificateEnforced;
    }
}
