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


import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.nio.file.Path;
import java.security.SecureRandom;

/**
 * Configuration file for ease of use
 */
@Plugin(name = "StreamSslConfiguration", category = "Core", elementType = "config", printObject = true)
public class StreamSslConfiguration {
    private final Path keyLocation;
    private final Path certLocation;
    private final Path caLocation;
    private final Path keystoreLocation;
    private final String caAlias;
    private final String certAlias;
    private final String keystorePW;

    /**
     * Config
     * @param keyLocation key location
     * @param certLocation cert location
     * @param caLocation ca location
     * @param keystoreLocation keystore location
     * @param caAlias ca alias
     * @param certAlias cert alias
     */
    public StreamSslConfiguration(final Path keyLocation,
                                  final Path certLocation,
                                  final Path caLocation,
                                  final Path keystoreLocation,
                                  final String caAlias,
                                  final String certAlias) {
        this.keyLocation = keyLocation;
        this.certLocation = certLocation;
        this.caLocation = caLocation;
        this.keystoreLocation = keystoreLocation;
        this.caAlias = caAlias;
        this.certAlias = certAlias;
        keystorePW = getRandomAsciiString(20);
    }

    /**
     * e
     * @param key private key
     * @param cert certificate
     * @param caBundle ca cert bundle
     * @param keystore keystore
     * @param caAl ca alias
     * @param certAl cert alias
     * @return e
     */
    @PluginFactory
    public static StreamSslConfiguration createConfigurationFile(@PluginAttribute("privatekey") final String key,
                                                                 @PluginAttribute("certificate") final String cert,
                                                                 @PluginAttribute("ca") final String caBundle,
                                                                 @PluginAttribute("keystore") final String keystore,
                                                                 @PluginAttribute("caAlias") final String caAl,
                                                                 @PluginAttribute("certAlias") final String certAl) {

        return new StreamSslConfiguration(Path.of(key),
                Path.of(cert),
                Path.of(caBundle),
                Path.of(keystore),
                caAl,
                certAl
        );
    }

    /**
     * The keystore only accept certain ASCII char as password
     * @param numChars the number of chars
     * @return the ASCII string
     */
    private String getRandomAsciiString(final int numChars) {
        final SecureRandom random = new SecureRandom();
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numChars; i++) {
            builder.append((char) (random.nextInt(75) + '0'));
        }
        return builder.toString();
    }

    /**
     * Gets key location
     * @return key locatiom
     */
    public Path getKeyLocation() {
        return keyLocation;
    }

    /**
     * Gets cert location
     * @return cert location
     */
    public Path getCertLocation() {
        return certLocation;
    }

    /**
     * Gets ca location
     * @return ca location
     */
    public Path getCaLocation() {
        return caLocation;
    }

    /**
     * Gets keystore location
     * @return keystore location
     */
    public Path getKeystoreLocation() {
        return keystoreLocation;
    }

    /**
     * Gets ca Alias
     * @return ca alias
     */
    public String getCaAlias() {
        return caAlias;
    }

    /**
     * Gets cert alias
     * @return cert alias
     */
    public String getCertAlias() {
        return certAlias;
    }

    /**
     * Gets keystore PW
     * @return keystore PW
     */
    public String getKeystorePW() {
        return keystorePW;
    }
}
