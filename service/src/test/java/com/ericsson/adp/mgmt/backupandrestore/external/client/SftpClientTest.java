/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.external.client;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.anyInt;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.exception.ImportExportException;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientImportProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.JSchSocketFactory;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.SftpConnection;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SftpClientTest {
    private static final String KEX = "kex";
    private static final String HOST_KEY_ALGORITHMS = "server_host_key";
    private static final String PUBLIC_KEY_ALGORITHMS = "PubkeyAcceptedAlgorithms";
    private static final String CIPHER_S2C = "cipher.s2c";
    private static final String CIPHER_C2S = "cipher.c2s";
    private static final String MAC_S2C = "mac.s2c";
    private static final String MAC_C2S = "mac.c2s";
    private static final String OLD_KEX_ALGORITHMS = ",diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha1,diffie-hellman-group1-sha1";
    private static final String OLD_HOST_KEY_ALGORITHMS = ",ssh-rsa,ssh-dss";
    private static final String OLD_PUBLIC_KEY_ALGORITHMS = ",ssh-rsa,ssh-dss";
    private static final String OLD_CIPHERS = ",aes128-cbc,blowfish-cbc,aes192-cbc,aes256-cbc";
    private static final String OLD_MAC_ALGORITHMS = ",hmac-md5,hmac-sha1-96,hmac-md5-96";
    private static final String CONNECTION_ERROR_MESSAGE = "Unable to connect to host %s on port %d";
    private static final String HOST = "localhost";
    private static final String USER = "user";
    private static final int PORT = 22;

    private ExternalClientProperties externalClientProperties;

    @Before
    public void setup() {
        externalClientProperties = new ExternalClientImportProperties("sftp://user@localhost:22/path", "password", Paths.get("/backupdir"),
                Paths.get("/backupfile"));
    }

    @Test
    public void connect_validInput_valid() throws JSchException {
        final JSch jsch = createMock(JSch.class);
        final Session session = createMock(Session.class);
        final ChannelSftp channel = createMock(ChannelSftp.class);

        expect(jsch.getSession(externalClientProperties.getUser(), externalClientProperties.getHost(), externalClientProperties.getPort()))
                .andReturn(session);

        session.setConfig("StrictHostKeyChecking", "no");
        expectLastCall();

        expectEncryptionAlgorithmConfiguration(session);

        session.setPassword(externalClientProperties.getPassword());
        expectLastCall();

        session.setSocketFactory(EasyMock.isA(JSchSocketFactory.class));
        expectLastCall();

        session.connect(EasyMock.anyInt());
        expectLastCall();
        session.disconnect();
        expectLastCall().anyTimes();

        expect(session.isConnected()).andReturn(true);
        expect(session.openChannel("sftp")).andReturn(channel);
        channel.setBulkRequests(EasyMock.anyInt());
        expectLastCall().anyTimes();
        channel.connect(EasyMock.anyInt());
        expectLastCall();

        channel.disconnect();
        expectLastCall().anyTimes();

        replay(jsch, session, channel);

        final SftpClient sftpClient = new SftpClient();
        sftpClient.setJsch(jsch);
        sftpClient.setTimeoutBytesReceivedSeconds(0);
        final SftpConnection connection = sftpClient.connect(externalClientProperties);
        verify(jsch, session, channel);
        assertTrue(connection instanceof SftpConnection);
    }

    private void expectEncryptionAlgorithmConfiguration(final Session session) {
        session.setConfig(KEX, JSch.getConfig(KEX) + OLD_KEX_ALGORITHMS);
        expectLastCall().atLeastOnce();

        session.setConfig(HOST_KEY_ALGORITHMS, JSch.getConfig(HOST_KEY_ALGORITHMS) + OLD_HOST_KEY_ALGORITHMS);
        expectLastCall().atLeastOnce();

        session.setConfig(PUBLIC_KEY_ALGORITHMS, JSch.getConfig(PUBLIC_KEY_ALGORITHMS) + OLD_PUBLIC_KEY_ALGORITHMS);
        expectLastCall().atLeastOnce();

        session.setConfig(CIPHER_C2S, JSch.getConfig(CIPHER_C2S) + OLD_CIPHERS);
        expectLastCall().atLeastOnce();

        session.setConfig(CIPHER_S2C, JSch.getConfig(CIPHER_S2C) + OLD_CIPHERS);
        expectLastCall().atLeastOnce();

        session.setConfig(MAC_C2S, JSch.getConfig(MAC_C2S) + OLD_MAC_ALGORITHMS);
        expectLastCall().atLeastOnce();

        session.setConfig(MAC_S2C, JSch.getConfig(MAC_S2C) + OLD_MAC_ALGORITHMS);
        expectLastCall().atLeastOnce();
    }

    @Test(expected = ImportExportException.class)
    public void connect_unableToConnectWithHost_throwsException() throws JSchException {
        final JSch jsch = createMock(JSch.class);
        final Session session = createMock(Session.class);

        expect(jsch.getSession(externalClientProperties.getUser(), externalClientProperties.getHost(), externalClientProperties.getPort()))
                .andThrow(new ImportExportException(String.format(CONNECTION_ERROR_MESSAGE, HOST, PORT)));

        replay(jsch, session);

        final SftpClient sftpClient = new SftpClient();
        sftpClient.setJsch(jsch);
        sftpClient.connect(externalClientProperties);
    }

    @Test(expected = ImportExportException.class)
    public void connect_unableToCreateSession_throwsException() throws JSchException {
        final JSch jsch = createMock(JSch.class);
        final Session session = createMock(Session.class);

        expect(jsch.getSession(externalClientProperties.getUser(), externalClientProperties.getHost(), externalClientProperties.getPort()))
                .andReturn(session);

        session.setConfig("StrictHostKeyChecking", "no");
        expectLastCall();

        expectEncryptionAlgorithmConfiguration(session);

        session.setPassword("password");
        expectLastCall();

        session.setSocketFactory(EasyMock.isA(JSchSocketFactory.class));
        expectLastCall();

        session.connect(EasyMock.anyInt());
        expectLastCall().andThrow(new ImportExportException(String.format(CONNECTION_ERROR_MESSAGE, HOST, PORT)));

        session.disconnect();
        expectLastCall().anyTimes(); // Disconnect will be called as part of retry logic

        replay(jsch, session);

        final SftpClient sftpClient = new SftpClient();
        sftpClient.setJsch(jsch);
        sftpClient.setTimeoutBytesReceivedSeconds(1);
        sftpClient.connect(externalClientProperties);
    }

    @Test(expected = ImportExportException.class)
    public void connect_unableToCreateChannel_throwsException() throws JSchException {
        final JSch jsch = createMock(JSch.class);
        final Session session = createMock(Session.class);
        final Channel channel = createMock(Channel.class);

        expect(jsch.getSession(USER, HOST, PORT)).andReturn(session);

        session.setConfig("StrictHostKeyChecking", "no");
        expectLastCall();

        session.setPassword("password");
        expectLastCall();

        expectEncryptionAlgorithmConfiguration(session);

        session.setSocketFactory(EasyMock.isA(JSchSocketFactory.class));
        expectLastCall();

        session.connect(EasyMock.anyInt());
        expectLastCall();

        expect(session.isConnected()).andReturn(false);

        expect(session.openChannel("sftp")).andReturn(channel);

        channel.connect(anyInt());
        expectLastCall().andThrow(new ImportExportException(String.format(CONNECTION_ERROR_MESSAGE, HOST, PORT)));

        session.disconnect();
        expectLastCall().anyTimes();
        channel.disconnect();
        expectLastCall().anyTimes();

        replay(jsch, session, channel);

        final SftpClient sftpClient = new SftpClient();
        sftpClient.setJsch(jsch);
        sftpClient.connect(externalClientProperties);
        }

}
