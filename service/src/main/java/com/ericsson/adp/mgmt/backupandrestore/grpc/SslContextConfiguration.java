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
package com.ericsson.adp.mgmt.backupandrestore.grpc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.ericsson.adp.mgmt.backupandrestore.util.TLSCertUtils;

import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextService;

/**
 * Responsible for creating SslContextService
 * and triggering a refresh at 9 second intervals.
 */
@Configuration
@EnableScheduling
public class SslContextConfiguration {
    private boolean tlsEnabled;
    private String certificateChainFilePath;
    private String privateKeyFilePath;
    private String certificateAuthorityFilePath;
    private String verifyClientCertificateEnforced;
    private SslContextService sslContextService;

    /**
     * Initializes an SslContextService with the certificate and private key paths.
     * @return sslContextService
     */
    @Bean
    public SslContextService getSslContextService() {
        sslContextService = new SslContextService();
        sslContextService.setCertificateChainFilePath(certificateChainFilePath);
        sslContextService.setPrivateKeyFilePath(privateKeyFilePath);
        sslContextService.setCertificateAuthorityFilePath(certificateAuthorityFilePath);
        sslContextService.setVerifyClientCertificateEnforced(verifyClientCertificateEnforced);
        return sslContextService;
    }

    /**
     * Refreshes the sslContext at 9 second intervals.
     */
    @Scheduled(fixedRate = 9000)
    public void refreshContext() {
        if (tlsEnabled && sslContextService != null) {
            sslContextService.refreshContext();
        }
    }

    /**
     * Sets the file path for the certificate chain.
     * If the tls.crt exists in the parent path, it takes preference.
     * @param certificateChainFilePath the file path for the certificate chain
     */
    @Value("${security.bro.server.cert.path:}")
    public void setCertificateChainFilePath(final String certificateChainFilePath) {
        this.certificateChainFilePath = TLSCertUtils.checkCertsInPathAndTakePreference(
            certificateChainFilePath, "cert");
    }

    /**
     * Sets the file path for the private key.
     * If the tls.key exists in the parent path, it takes preference.
     * @param privateKeyFilePath the file path for the private key
     */
    @Value("${security.bro.server.key.path:}")
    public void setPrivateKeyFilePath(final String privateKeyFilePath) {
        this.privateKeyFilePath = TLSCertUtils.checkCertsInPathAndTakePreference(
            privateKeyFilePath, "priv");
    }

    /**
     * Sets the file path for the certificate authority.
     * If the ca.crt exists in the parent path, it takes preference.
     * @param certificateAuthorityFilePath the file path for the certificate authority
     */
    @Value("${security.bro.ca.path:}")
    public void setCertificateAuthorityFilePath(final String certificateAuthorityFilePath) {
        this.certificateAuthorityFilePath = TLSCertUtils.checkCertsInPathAndTakePreference(
            certificateAuthorityFilePath, "ca");
    }

    /**
     * Sets whether TLS is enabled or not.
     * @param isTlsEnabled true if TLS is enabled, false otherwise
     */
    @Value("${flag.global.security:true}")
    public void setTlsEnabled(final boolean isTlsEnabled) {
        this.tlsEnabled = isTlsEnabled;
    }

    /**
     * Sets the verification level for client certificates.
     * @param verifyClientCertificateEnforced the verification level for client certificates
     */
    @Value("${grpc.verifyClientCertificateEnforced:optional}")
    public void setVerifyClientCertificateEnforced(final String verifyClientCertificateEnforced) {
        this.verifyClientCertificateEnforced = verifyClientCertificateEnforced;
    }
}
