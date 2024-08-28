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

import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.StoreConfigurationException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.net.SocketFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.Optional;

/**
 * Manages the connection to the logging endpoint, noting if a send failure invalidates the connection and
 * rebuilding the socket as necessary when it is invalidated
 * */
public class ConnectionManager {
    private final StreamSslConfiguration sslConfiguration;
    private final String host;
    private final int port;
    private final int connectionTimeout;
    private final int readTimeout;
    private Optional<Socket> connection = Optional.empty();
    private Optional<OutputStream> writer = Optional.empty();

    /**
     * Construct the connection manager
     * @param configuration the ssl configuration, or null if using plaintext
     * @param host the logging endpoint host
     * @param port the logging endpoint port
     * @param connectionTimeout the maximum amount of time to wait while trying to establish a connection with the logging
     *                          endpoint, in milliseconds
     * @param readTimeout the amount of time in milliseconds to wait for the data from the logging endpoint server to arrive.
     * */
    public ConnectionManager(final StreamSslConfiguration configuration,
                             final String host,
                             final int port,
                             final int connectionTimeout,
                             final int readTimeout) {
        this.host = host;
        this.port = port;
        this.sslConfiguration = configuration;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        Security.addProvider(new BouncyCastleProvider());
    }

    private void buildConnection()
            throws IOException, CertificateException, NoSuchAlgorithmException,
            KeyStoreException, KeyException, StoreConfigurationException {
        if (this.sslConfiguration == null) {
            this.connection = Optional.of(SocketFactory.getDefault().createSocket());
        } else {
            final SslConfiguration newConfig = KeyStoreHelper.populateKeystore(this.sslConfiguration);
            this.connection = Optional.of(newConfig.getSslContext().getSocketFactory().createSocket());
        }
        final Socket socket = this.connection.get();
        socket.setSoTimeout(readTimeout);
        socket.connect(new InetSocketAddress(host, port), connectionTimeout);
        writer = Optional.of(socket.getOutputStream());
    }

    /**
     * Send the byte array to the logging endpoint. Returns an exception if the send failed, Optional.empty() otherwise
     * @param data the data to send to the endpoint
     * @throws CertificateException on failure
     * @throws NoSuchAlgorithmException on failure
     * @throws IOException on failure
     * @throws StoreConfigurationException on failure
     * @throws KeyStoreException on failure
     * @throws KeyException on failure
     * */
    public void send(final byte[] data)
            throws CertificateException, NoSuchAlgorithmException, IOException, StoreConfigurationException, KeyStoreException, KeyException {
        if (connection.isEmpty() || writer.isEmpty()) {
            buildConnection();
        }
        if (writer.isPresent()) {
            try {
                writer.get().write(data);
                writer.get().flush();
            } catch (Exception e) {
                closeConnection();
                throw e;
            }
        }
    }

    private void closeConnection() {
        this.connection.ifPresent(c -> {
            try {
                c.close();
            } catch (IOException ignored) {
                //We want to ignore socket closing failures here
            }
            this.writer = Optional.empty();
            this.connection = Optional.empty();
        });
    }
}
