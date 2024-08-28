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

package com.ericsson.adp.mgmt.socket.appender;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.StoreConfigurationException;

import com.ericsson.adp.mgmt.socket.appender.util.ConnectionManager;
import com.ericsson.adp.mgmt.socket.appender.util.KeyStoreHelper;
import com.ericsson.adp.mgmt.socket.appender.util.StreamSslConfiguration;

import java.io.IOException;
import java.io.Serializable;
import java.security.KeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Custom Appender to mimic the functionality of a socket appender with added functionality
 * Created from the log4j2.xml file
 */
@Plugin(name = "BufferedSocketAppender", category = "Core", elementType = "appender", printObject = true)
public class BufferedSocketAppender extends AbstractAppender {
    private int port;
    private String host;
    private int maxSize;
    private int connectionTimeout;
    private long appendTimeout;
    private int readTimeout;
    private BlockingDeque<LogEvent> queuedLogEvents;
    private ConnectionManager manager;

    /**
     * Constructor for the custom appender
     * created from the configuration file
     * @param name name of Appender
     * @param filter The Filter to associate with the Appender.
     * @param layout The layout to use to format the event.
     * @param ignoreExceptions If true, exceptions will be logged and suppressed. If false errors will be logged and then passed to the application.
     * @param properties Optional properties
     */
    protected BufferedSocketAppender(final String name,
                                     final Filter filter,
                                     final Layout<? extends Serializable> layout,
                                     final boolean ignoreExceptions,
                                     final Property[] properties
                                     ) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    private void setPort(final int port) {
        this.port = port;
    }

    private void setHost(final String host) {
        this.host = host;
    }

    private void setMaxSize(final int maxSize) {
        this.maxSize = maxSize;
    }

    private void setConnectionTimeout(final int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    private void setAppendTimeout(final long appendTimeout) {
        this.appendTimeout = appendTimeout;
    }

    public void setReadTimeout(final int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * initiate the log appender
     * @param sslConfiguration the ssl configuration, or null if using plaintext
     */
    public void init(final StreamSslConfiguration sslConfiguration) {
        this.queuedLogEvents = new LinkedBlockingDeque<>(maxSize);
        this.manager = new ConnectionManager(sslConfiguration, host, port, connectionTimeout, readTimeout);
        final Thread thread = new Thread(this::executeLoop);
        thread.start();
    }

    /**
     * @param name name from configuration file
     * @param host host from configuration file
     * @param port port from configuration file
     * @param maxSize maxSize of buffer from configuration file
     * @param connectionTimeout time in milliseconds to wait to connect to logging endpoint
     * @param appendTimeout maximum time in milliseconds application code can block trying to add events to event queue
     * @param readTimeout the amount of time in milliseconds to wait for the data from the logging endpoint server to arrive.
     * @param filter filter from configuration file
     * @param layout layout from configuration file
     * @param streamSslConfiguration configuration file for the keystore
     * @throws CertificateException certificate exception
     * @throws NoSuchAlgorithmException nsa for the certificate
     * @throws KeyStoreException kse could not create keystore
     * @throws StoreConfigurationException sce could not configure keystore
     * @throws KeyException key exception for invalid private key
     * @throws IOException IOException
     * @return BufferedSocketAppender from configuration file
     */
    @PluginFactory
    @SuppressWarnings("PMD.ExcessiveParameterList")
    public static BufferedSocketAppender createAppender(@PluginAttribute("name") final String name,
                                                        @PluginAttribute("host") final String host,
                                                        @PluginAttribute("port") final int port,
                                                        @PluginAttribute(value = "maxSize", defaultInt = 1000) final int maxSize,
                                                        @PluginAttribute(value = "connectionTimeout", defaultInt = 500) final int connectionTimeout,
                                                        @PluginAttribute(value = "appendTimeout", defaultLong = 5000) final int appendTimeout,
                                                        @PluginAttribute(value = "readTimeout", defaultInt = 60000) final int readTimeout,
                                                        @PluginElement("Filter") final Filter filter,
                                                        @PluginElement("Layout") final Layout<? extends Serializable> layout,
                                                        @PluginElement("BroSslConfig") final StreamSslConfiguration streamSslConfiguration)
            throws CertificateException,
            NoSuchAlgorithmException,
            KeyStoreException,
            StoreConfigurationException,
            KeyException,
            IOException {
        final BufferedSocketAppender bufferedSocketAppender = new BufferedSocketAppender(name, filter, layout, true, null);
        bufferedSocketAppender.setHost(host);
        bufferedSocketAppender.setPort(port);
        bufferedSocketAppender.setMaxSize(maxSize);
        bufferedSocketAppender.setConnectionTimeout(connectionTimeout);
        bufferedSocketAppender.setAppendTimeout(appendTimeout);
        bufferedSocketAppender.setReadTimeout(readTimeout);
        bufferedSocketAppender.init(streamSslConfiguration);
        //If SSL is enabled, build a context here but do nothing with it, to assert at startup time that the ssl config is valid
        if (streamSslConfiguration != null) {
            final SslConfiguration sslConfiguration = KeyStoreHelper.populateKeystore(streamSslConfiguration);
            sslConfiguration.getSslContext().getSocketFactory().createSocket();
        }
        return bufferedSocketAppender;
    }


    /**
     * Main worker thread for sending LogEvents
     */
    private void executeLoop() {
        final boolean running = true;
        while (running) {
            try {
                final LogEvent logEvent = queuedLogEvents.takeFirst();
                processLogEvent(logEvent);
            } catch (InterruptedException e) {
                LOGGER.log(Level.ERROR, e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processLogEvent(final LogEvent logEvent) {
        try {
            manager.send(getLayout().toByteArray(logEvent));
        } catch (Exception e) {
            if (!queuedLogEvents.offerFirst(logEvent)) {
                // log event is dropped when offerFirst() is failed.
                LOGGER.log(Level.WARN, "Failed to send: {}", logEvent.getMessage());
                LOGGER.log(Level.WARN, "Buffer is full, a log of log level <{}> is dropped", logEvent.getLevel());
            } else {
                LOGGER.log(Level.DEBUG, "Failed to send, will try to resend the log event");
            }
        }
    }


    /***
     * Append method is called on a log event to write it down a socket connection.
     * During graceful shutdown, a calling thread in a BLOCKED state will be interrupted.
     * When the thread is interrupted, this implementation will retry appending the log event.
     * This one-off best-effort retry attempt does not guarantee that the log event will be added
     * to the queue, as the queue could still be full.
     * Add a logEvent at the tail of this deque.
     * @param logEvent to be appended
     */
    @Override
    public void append(final LogEvent logEvent) {
        try {
            // Insert the log at the tail of the queue, waiting up to timeout milliseconds if necessary for space to become
            // available.The event is dropped if the insertion fails.
            if (!queuedLogEvents.offer(logEvent.toImmutable(), appendTimeout, TimeUnit.MILLISECONDS)) {
                LOGGER.log(Level.ERROR, "Failed to append event of severity {} to queue, dropping", logEvent.getLevel());
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARN,
                    "Exception ({}) happens when appending the event of severity {} to queue, retrying append.",
                    e.getMessage(),
                    logEvent.getLevel());
            // Retry inserting the log at the tail of the queue, without waiting for space to become available.
            // The event is dropped if the insertion fails.
            if (!queuedLogEvents.offer(logEvent.toImmutable())) {
                LOGGER.log(Level.ERROR, "Failed to append event of severity {} to queue again after retry, dropping",
                        logEvent.getLevel());
            }
            Thread.currentThread().interrupt();
        }
    }
}