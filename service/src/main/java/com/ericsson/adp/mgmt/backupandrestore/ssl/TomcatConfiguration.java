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

import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.CMM_NOTIF;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BRO_HTTP_PORT;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BRO_TLS_CMM_NOTIF_PORT;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BRO_TLS_PORT;
import static org.springframework.boot.web.server.Ssl.ClientAuth.NEED;
import static org.springframework.boot.web.server.Ssl.ClientAuth.WANT;

import java.io.File;
import java.security.KeyStore;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration class for configuring tomcat
 */
@Configuration
@EnableScheduling
public class TomcatConfiguration {
    // All except the first cipher taken from https://wiki.openssl.org/index.php/TLS1.3#Ciphersuites
    // and listed as "used by default" in openssl when TSL 1.3 is in use, and listed as either MUST
    // or SHOULD implement here: https://datatracker.ietf.org/doc/html/rfc8446#section-9.1
    private static final String[] supportedCiphers = new String[] {
        "ECDHE-ECDSA-AES256-GCM-SHA384",
        "TLS_AES_256_GCM_SHA384",
        "TLS_CHACHA20_POLY1305_SHA256",
        "TLS_AES_128_GCM_SHA256",
    };
    private static final Logger log = LogManager.getLogger(TomcatConfiguration.class);
    private static final String[] ALLOWED_TLS_PROTOCOLS = {"TLSv1.2", "+TLSv1.3"};
    private static final String REST_ACTIONS_TLS_OPTIONAL = "optional";
    Predicate<String> clientCertEnforced = x -> "required".equalsIgnoreCase(x) || "optional".equalsIgnoreCase(x);


    private boolean enableCMM;

    private int serverPort; // default 7001
    private int restTlsPort; // default 7002
    private int cmmNotifPort; // default 7004

    private String restActionsTlsEnforced;
    private String verifyRestActionsClientCertificateEnforced;
    private String verifyCMMClientNotifCertificateEnforced;

    private final AtomicBoolean globalTlsEnabled = new AtomicBoolean();
    private final AtomicBoolean applicationReady = new AtomicBoolean();
    private volatile KeyStoreService keyStoreService;
    private final Set<Connector> connectors = new CopyOnWriteArraySet<>();

    /**
     * Configures SSL for the REST interface
     * @return WebServerFactoryCustomizer<TomcatServletWebServerFactory>
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> configureTomcat() {
        return factory -> {
            if (globalTlsEnabled.get()) {
                keyStoreService.generateKeyStores();
                configureSsl(factory, KeyStoreAlias.BRO);
                factory.addConnectorCustomizers(tomcatConnector -> {
                    tomcatConnector.setPort(getRestTlsPort());
                    addConnector(tomcatConnector);
                });
                if (REST_ACTIONS_TLS_OPTIONAL.equals(restActionsTlsEnforced)) {
                    addRestConnector(factory);
                }
                if (isEnableCMM() && isConfigurationValid(CMM_NOTIF)) {
                    final Connector connector = createSslConnector(KeyStoreAlias.CMM_NOTIF, getCmmNotifPort(),
                            clientCertEnforced.test(verifyCMMClientNotifCertificateEnforced));
                    factory.addAdditionalTomcatConnectors(connector);
                    addConnector(connector);
                }
            }
        };
    }

    private void addRestConnector(final TomcatServletWebServerFactory tomcat) {
        final Connector restConnector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        restConnector.setPort(getServerPort());
        tomcat.addAdditionalTomcatConnectors(restConnector);
    }

    private boolean isConfigurationValid(final KeyStoreAlias alias) {
        return keyStoreService.getKeyStoreConfig(alias).isValidConfiguration();
    }

    /**
     * To be used to for SSL Ports
     * @param port Port
     * @param clientAuth clientAuth
     * @return Connector to be added
     */
    private Connector createSslConnector(final KeyStoreAlias alias, final int port,
                                         final boolean clientAuth) {
        final KeyStoreConfiguration config = keyStoreService.getKeyStoreConfig(alias);
        final Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        final Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
        final SSLHostConfigCertificate.Type certType = SSLHostConfigCertificate.Type.RSA;

        try {
            final File keystore = config.getKeyStoreFile().toFile();
            connector.setScheme("https");
            connector.setSecure(true);
            connector.setPort(port);
            protocol.setSSLEnabled(true);
            // Creates and set a new SSLHostConfig
            final SSLHostConfig sslHostConfig = createSSLHostConfig(keystore.getAbsolutePath(), config.getKeyStorePassword(),
                    clientAuth);
            // Creates a set a new SSLHostConfigCertificate
            final SSLHostConfigCertificate certificate = createSSLHostConfigCertificate(
                    sslHostConfig, keystore.getAbsolutePath(), config.getKeyStorePassword(),
                    certType, alias.toString());
            sslHostConfig.addCertificate(certificate);
            connector.addSslHostConfig(sslHostConfig);
            return connector;
        } catch (Exception ex) {
            throw new IllegalStateException("Error accessing either the keystore or truststore:", ex);
        }
    }

    private void configureSsl(final TomcatServletWebServerFactory tomcat,
                              final KeyStoreAlias alias) {
        keyStoreService.generateKeyStores();
        final KeyStoreConfiguration config = keyStoreService.getKeyStoreConfig(alias);
        final Ssl ssl = new Ssl();
        ssl.setEnabled(true);
        ssl.setEnabledProtocols(ALLOWED_TLS_PROTOCOLS);
        ssl.setCiphers(supportedCiphers);
        ssl.setKeyStoreType(keyStoreService.getKeyStoreType());
        ssl.setKeyStore(config.getKeyStoreFile().toString());
        ssl.setKeyStorePassword(config.getKeyStorePassword());
        ssl.setKeyAlias(config.getAlias().toString());
        ssl.setTrustStoreType(keyStoreService.getKeyStoreType());
        ssl.setTrustStore(config.getKeyStoreFile().toString());
        ssl.setTrustStorePassword(config.getKeyStorePassword());
        if ("required".equals(verifyRestActionsClientCertificateEnforced)) {
            ssl.setClientAuth(NEED);
        } else {
            ssl.setClientAuth(WANT);
        }
        tomcat.setSsl(ssl);
    }

    private SSLHostConfigCertificate createSSLHostConfigCertificate(final SSLHostConfig sslHostConfig,
                                                                 final String absolutPath,
                                                                 final String keyStorePassword,
                                                                 final SSLHostConfigCertificate.Type certType,
                                                                 final String alias) {
        final SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(sslHostConfig, certType);
        certificate.setCertificateKeystoreFile(absolutPath);
        certificate.setCertificateKeystorePassword(keyStorePassword);
        certificate.setCertificateKeyAlias(alias);
        certificate.setCertificateKeystoreType(keyStoreService.getKeyStoreType());
        return certificate;
    }

    private SSLHostConfig createSSLHostConfig(final String absolutePath, final String storePassword,
                                              final boolean clientAuth) {
        final SSLHostConfig sslHostConfig = new SSLHostConfig();
        sslHostConfig.setEnabledProtocols(ALLOWED_TLS_PROTOCOLS);
        sslHostConfig.setEnabledCiphers(supportedCiphers);
        sslHostConfig.setCiphers(String.join(",", supportedCiphers));
        sslHostConfig.setTruststoreFile(absolutePath);
        sslHostConfig.setTruststorePassword(storePassword);
        sslHostConfig.setTruststoreType(keyStoreService.getKeyStoreType());
        // Due to https://bugs.openjdk.org/browse/JDK-8206923, CertificateVerification should be either none or required
        sslHostConfig.setCertificateVerification(clientAuth ? "required" : "none");
        return sslHostConfig;
    }

    /**
     * Regenerates and reloads the keystore every 9 seconds
     */
    @Scheduled(fixedRate = 9000)
    public void refreshContext() {
        if (globalTlsEnabled.get() && applicationReady.get()) {
            keyStoreService.generateKeyStores();
            connectors.forEach(this::refreshConnector);
        }
    }

    private void refreshConnector(final Connector connector) {
        final Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
        final SSLHostConfig[] sslHostConfigs = protocol.findSslHostConfigs();
        // need to modify the SSL Context to reload
        for (final SSLHostConfig hostConfig : sslHostConfigs) {
            Optional<KeyStore> keyStore = Optional.empty();
            for (final SSLHostConfigCertificate certificate : hostConfig.getCertificates()) {
                final String certificateAlias = certificate.getCertificateKeyAlias();
                keyStore = Optional.ofNullable(keyStoreService.getKeyStore(KeyStoreAlias.fromString(certificateAlias)));
                if (keyStore.isPresent()) {
                    certificate.setCertificateKeystore(keyStore.get());
                } else {
                    log.warn("No keystore found for certificate alias: {}", certificateAlias);
                }
            }
            if (keyStore.isPresent()) {
                hostConfig.setTrustStore(keyStore.get());
            } else {
                log.warn("No TrustStore updated for SSLHostConfig: {}", hostConfig.getHostName());
            }
        }
        if (connector.getState().isAvailable()) {
            log.debug("Refreshing connector {}.", connector);
            protocol.reloadSslHostConfigs();
        } else {
            log.warn("Connector {} is not available; cannot refresh SSL configurations.", connector);
        }
    }

    /**
     * Listens for an event which indicates that the Spring application is ready to handle http requests
     */
    @EventListener(ApplicationReadyEvent.class)
    public void setApplicationReady() {
        this.applicationReady.set(true);
    }

    @Autowired
    public void setKeyStoreService(final KeyStoreService keyStoreService) {
        this.keyStoreService = keyStoreService;
    }

    /**
     * Setter for globalTlsEnabled
     *
     * @param globalTlsEnabled Indicates whether global TLS is enabled
     */
    @Value("${global.tls:false}")
    public void setGlobalTlsEnabled(final boolean globalTlsEnabled) {
        this.globalTlsEnabled.set(globalTlsEnabled);
    }

    public boolean isGlobalTlsEnabled() {
        return globalTlsEnabled.get();
    }

    public int getRestTlsPort() {
        return restTlsPort;
    }

    @Value("${server.tls.port:" + BRO_TLS_PORT + "}")
    public void setRestTlsPort(final int restTlsPort) {
        this.restTlsPort = restTlsPort;
    }

    public int getServerPort() {
        return serverPort;
    }

    @Value("${flag.enable.cm:false}")
    public void setEnableCMM(final boolean enableCMM) {
        this.enableCMM = enableCMM;
    }

    public boolean isEnableCMM() {
        return enableCMM;
    }

    /**
     * Set server port
     * @param confServerPort default BRO port
     */
    @Value("${server.port:" + BRO_HTTP_PORT + "}")
    public void setServerPort(final int confServerPort) {
        this.serverPort = confServerPort;
    }

    @Value("${restActions.tlsRequired:required}")
    public void setRestActionsTlsEnforced(final String restActionsTlsEnforced) {
        this.restActionsTlsEnforced = restActionsTlsEnforced;
    }

    /**
     * To validate Client Certificate
     * @param verifyRestActionsClientCertificateEnforced optional / required
     */
    @Value("${restActions.verifyClientCertificateEnforced:optional}")
    public void setverifyRestActionsClientCertificateEnforced(final String verifyRestActionsClientCertificateEnforced) {
        this.verifyRestActionsClientCertificateEnforced = verifyRestActionsClientCertificateEnforced;
    }

    @Value("${rest.verifyCMMNotifyCertificateEnforced:optional}")
    public void setVerifyCMMClientNotifCertificateEnforced(final String verifyCMMClientCertificateEnforced) {
        this.verifyCMMClientNotifCertificateEnforced = verifyCMMClientCertificateEnforced;
    }

    public int getCmmNotifPort() {
        return cmmNotifPort;
    }

    @Value("${security.bro.cmm.notif.port:" + BRO_TLS_CMM_NOTIF_PORT + "}")
    public void setCmmNotifPort(final int cmmNotifPort) {
        this.cmmNotifPort = cmmNotifPort;
    }

    /**
     * Add a connector to the set of connectors
     * @param connector the connector to be added
     */
    public void addConnector(final Connector connector) {
        connectors.add(connector);
    }
}
