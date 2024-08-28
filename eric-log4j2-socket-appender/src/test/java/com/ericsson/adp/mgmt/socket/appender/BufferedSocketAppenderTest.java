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


import com.ericsson.adp.mgmt.socket.appender.util.KeyStoreHelper;
import com.ericsson.adp.mgmt.socket.appender.util.StreamSslConfiguration;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.net.ssl.StoreConfigurationException;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.security.KeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class BufferedSocketAppenderTest {

    private int LOG4J_PORT = 5014;
    private LoggerContext ctx;
    private Configuration config;
    private BufferedSocketAppender bufferedSocketAppender;
    private AtomicBoolean serverStop = new AtomicBoolean();
    private AtomicBoolean serverRunning = new AtomicBoolean();
    private Path certLocation = Path.of("src/test/resources/cmmclientcert.pem");
    private Path keyLocation = Path.of("src/test/resources/clientprivkey.key");
    private Path caCertLocation = Path.of("src/test/resources/ca.pem");
    private Path keyStoreDir = Path.of("src/test/resources/test.p12");
    private ServerSocket serverSocket;
    private AtomicInteger receivedCount;


    @Before
    public void setUp() {
        ctx = LoggerContext.getContext();
        config = ctx.getConfiguration();
        bufferedSocketAppender = config.getAppender("TLSAppender");
        startServer();
    }

    @After
    public void tearDown() {
        stopServer();
    }

    // start a default server
    private void startServer() {
        startServer((s) -> true);
    }

    // start a server and check the message received
    private void startServer(final Predicate<String> messageTest) {
        serverStop.set(false);
        new Thread(() -> this.serverLoop(messageTest)).start();
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> serverRunning.get());
    }

    private void stopServer() {
        serverStop.set(true);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> !serverRunning.get());
    }

    @Test
    public void append_plainText_toServerSocket() {
        //TODO this actually asserts nothing and by extension tests nothing
        bufferedSocketAppender.append(new MutableLogEvent());
        Assert.assertTrue(true);
    }

    @Test
    public void append_populateKeystore() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, StoreConfigurationException, KeyException, IOException {
        KeyStoreHelper.populateKeystore(new StreamSslConfiguration(keyLocation, certLocation, caCertLocation, keyStoreDir, "siptls", "LT"));
        Assert.assertTrue(true);
    }

    @Test
    public void append_withNoServer() {
        stopServer();
        bufferedSocketAppender.append(new MutableLogEvent());
        Assert.assertTrue(true);
    }

    @Test (expected = NullPointerException.class)
    public void append_withNoAppender() {
        bufferedSocketAppender = null;
        bufferedSocketAppender.append(null);
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void append_multipleLogs() {
        bufferedSocketAppender = config.getAppender("SimpleAppender");
        final ConcurrentMap<String, Boolean> goodVals = new ConcurrentHashMap<>();
        stopServer();
        startServer(goodVals::containsKey);
        final int messageCount = 10;
        for(int i=0;i< messageCount; i++) {
            final var event = new MutableLogEvent(new StringBuilder().append(i),null);
            goodVals.put(new String(bufferedSocketAppender.getLayout().toByteArray(event)), true);
            bufferedSocketAppender.append(event);
        }
        // Some events will be dropped here as we're purposefully forcing a full queue scenario with a
        // "misbehaving" server that closes the connection after receiving an event. This, happily,
        // does test connection rebuilding, and we can assert that at least three of the messages make it,
        // as that is the size of the message queue in this test
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> receivedCount.get() > 3);
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void append_multipleLogs_noConnection() {
        stopServer();
        final int messageCount = 5;
        for(int i=0;i< messageCount; i++) {
            final var event = new MutableLogEvent(new StringBuilder().append(i),null);
            bufferedSocketAppender.append(event);
        }
    }

    // start a socket server, read the received message, check the message.
    private void serverLoop(final Predicate<String> passed) {
        receivedCount = new AtomicInteger();
        try {
            serverSocket = new ServerSocket(LOG4J_PORT);
            serverSocket.setSoTimeout(100);
        } catch (IOException e) {
            System.out.println(e);
            System.out.println("Failed to startup test server");
            return;
        }
        while(!serverStop.get()) {
            serverRunning.set(true);
            try {
                Socket client = serverSocket.accept();
                var input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String received;
                while (!serverStop.get() && input.ready() && (received = input.readLine()) != null) {
                    received = received + "\n";
                    if(!passed.test(received)) {
                        throw new RuntimeException("Message " + received + " failed predicate");
                    }
                    receivedCount.getAndIncrement();
                }
                client.close();
            } catch (IOException ignored) {
                // we don't care if this does nothing
            }
        }
        try {
            serverSocket.close();
            System.out.println("Server stopped");
        } catch (IOException e) {
            System.out.println("Failed to stop server");
            System.out.println(e);
        }
        serverRunning.set(false);
    }
}
