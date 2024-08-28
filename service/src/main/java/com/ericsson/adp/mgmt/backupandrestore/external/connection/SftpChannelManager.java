/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.external.connection;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jasypt.contrib.org.apache.commons.codec_1_3.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.Endpoint;
import com.ericsson.adp.mgmt.backupandrestore.exception.ImportExportException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidSftpServerHostKeyException;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientProperties;
import com.ericsson.adp.mgmt.backupandrestore.kms.CMKeyPassphraseService;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Class to manage an SFTP connection.
 * */

@Component
@Scope("prototype")
public class SftpChannelManager {
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
    private static final Logger log = LogManager.getLogger(SftpChannelManager.class);

    private int timeout;
    private byte egressTrafficDscp;
    private JSch jsch;
    private ExternalClientProperties clientProperties;
    private CMKeyPassphraseService cmKeyPassphraseService;


    private ChannelSftp channel;
    private Session session;

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    public JSch getJsch() {
        return jsch;
    }

    /**
     * Update the JSch logger, and set it.
     * @param jsch the Jsch
     */
    public void setJsch(final JSch jsch) {
        JSch.setLogger(new JschToLog4j());
        this.jsch = jsch;
    }

    public ExternalClientProperties getClientProperties() {
        return clientProperties;
    }

    public void setClientProperties(final ExternalClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }

    /**
     * Retrieve a channel to communicate with the SFTP server over
     * @return an sftp channel
     * */
    public ChannelSftp channel() {
        return channel;
    }

    @Autowired
    public void setCmKeyPassphraseService(final CMKeyPassphraseService cmKeyPassphraseService) {
        this.cmKeyPassphraseService = cmKeyPassphraseService;
    }

    public void setEgressTrafficDscp(final byte egressTrafficDscp) {
        this.egressTrafficDscp = egressTrafficDscp;
    }

    /**
     * Request the connection to the server. Does nothing when connection is made, but
     * will cause a channel reset when connection has failed and needs to be rebuilt or
     * on first call
     *
     * @throws JSchException exception if any parameter is invalid
     */
    @Retryable( value = JSchException.class,
            maxAttemptsExpression = "${sftp.retry.attempts:10}", backoff = @Backoff(delayExpression = "${sftp.retry.delayMs:3000}"))
    public void connect() throws JSchException {
        // If we're trying to connect, we need to cleanup the last
        // connection if it was open to avoid leaking resources
        close();
        if (channel == null || session == null || !channel.isConnected() || !session.isConnected()) {
            configureSessionAndConnect();
            openChannelFromSession();
        }
    }

    /**
     * Recovery action when @Retryable attempts fail when NotificationFailedException is thrown
     * @throws JSchException notificationFailedException
     * @param exception JSchException
     */
    @Recover
    public void connectionException(final JSchException exception) throws JSchException {
        log.error("Failed retry connect to SFTP server: ", exception);
        throw exception;
    }

    /**
     * Shut down the sftp channel and session, if they were open
     * */
    public void close() {
        if (channel != null) {
            channel.disconnect();
        }
        if (session != null) {
            session.disconnect();
        }
    }

    private void openChannelFromSession() throws JSchException {
        if (session.isConnected()) {
            log.info("Attempting to opening sftp channel");
            channel = (ChannelSftp) session.openChannel(SftpConnection.CHANNEL_TYPE_SFTP);
            channel.connect(timeout);
            log.info("Opened sftp channel");
        } else {
            throw new ImportExportException(SftpConnection.CONNECTION_ERROR_MESSAGE);
        }
    }

    private void configureSessionAndConnect() throws JSchException {
        log.info("Attempting to open sftp session");
        session = jsch.getSession(clientProperties.getUser(), clientProperties.getHost(),
                clientProperties.getPort());
        session.setSocketFactory(new JSchSocketFactory(egressTrafficDscp, timeout));
        configureEncryptionAlgorithms();

        if (clientProperties.getPassword() != null) {
            log.info("Using password to authenticate to the sftp server.");
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(clientProperties.getPassword());
        } else {
            log.info("Using public key credentials to authenticate to the sftp server.");
            session.setConfig("StrictHostKeyChecking", "yes");
            usePublicKeyAuthentication();
        }
        session.connect(timeout);
        log.info("Opened sftp session");
    }

    private void configureEncryptionAlgorithms() {
        session.setConfig(KEX, JSch.getConfig(KEX) + OLD_KEX_ALGORITHMS);
        session.setConfig(HOST_KEY_ALGORITHMS, JSch.getConfig(HOST_KEY_ALGORITHMS) + OLD_HOST_KEY_ALGORITHMS);
        session.setConfig(PUBLIC_KEY_ALGORITHMS, JSch.getConfig(PUBLIC_KEY_ALGORITHMS) + OLD_PUBLIC_KEY_ALGORITHMS);
        session.setConfig(CIPHER_C2S, JSch.getConfig(CIPHER_C2S) + OLD_CIPHERS);
        session.setConfig(CIPHER_S2C, JSch.getConfig(CIPHER_S2C) + OLD_CIPHERS);
        session.setConfig(MAC_C2S, JSch.getConfig(MAC_C2S) + OLD_MAC_ALGORITHMS);
        session.setConfig(MAC_S2C, JSch.getConfig(MAC_S2C) + OLD_MAC_ALGORITHMS);
    }

    private void usePublicKeyAuthentication() throws JSchException {
        clearJSChKeys();
        final Endpoint endpoint = clientProperties.getSftpServer().getEndpoints().getEndpoint()[0];
        setClientIdentity(endpoint);
        setKnownHosts(endpoint);
    }

    private void clearJSChKeys() throws JSchException {
        log.debug("Removing existing client key identities and known host keys.");
        jsch.removeAllIdentity();
        jsch.getHostKeyRepository().remove(clientProperties.getHost(), null);
    }

    private void setClientIdentity(final Endpoint endpoint) throws JSchException {
        final String encryptedClientPrivateKey = endpoint.getClientIdentity()
                                                .getPublicKey().getLocalDefinition()
                                                .getPrivateKey();
        String encodedClientPrivateKey = encryptedClientPrivateKey;
        if (cmKeyPassphraseService.isEnabled()) {
            encodedClientPrivateKey = cmKeyPassphraseService.getPassphrase(encryptedClientPrivateKey);
        }
        final byte[] privateKey = decodeBase64(encodedClientPrivateKey);

        final String encodedClientPublicKey = endpoint.getClientIdentity()
                                                .getPublicKey().getLocalDefinition()
                                                .getPublicKey();
        final byte[] publicKey = decodeBase64(encodedClientPublicKey);

        log.debug("Setting client's SSH key pair.");
        // The fourth parameter <null> means the private key is not protected by a passphrase.
        jsch.addIdentity("bro", privateKey, publicKey, null);
    }

    private void setKnownHosts(final Endpoint endpoint) {
        final Collection<String> hostKeys = endpoint.getServerAuthentication()
                                        .getSshHostKeys().getLocalDefinition()
                                        .getHostKeys();
        final HostKeyRepository hostKeyRepository = jsch.getHostKeyRepository();
        log.debug("Setting known host keys.");
        for (final String key : hostKeys) {
            // The OpenSSH public key is in the format [algorithm-type] [base64-encoded-key] [optional-comment].
            // JSCh only accepts the [base64-encoded-key] part when creating host keys.
            // As such, the key needs to be parsed and then decoded before creating a host key.
            try {
                final String[] hostKeyElements = getHostKeyElements(key);
                final byte[] hostKey = decodeBase64(hostKeyElements[1]);
                final HostKey host = new HostKey(clientProperties.getHost(), hostKey);
                hostKeyRepository.add(host, null);
                log.info("Added a host key that is using <{}> algorithm", hostKeyElements[0]);
            } catch (Exception e) {
                log.error("Failed to parse the host key", e);
                throw new InvalidSftpServerHostKeyException("The SFTP Server host key is not valid.");
            }
        }
    }

    /**
     * Decodes a base 64 encoded key string
     * @param key the base64 encoded key string
     * @return the decoded key string
     */
    private byte[] decodeBase64(final String key) {
        return Base64.decodeBase64(key.getBytes());
    }

    /**
     * Decodes the base 64 encoded OpenSSH public key
     * and then returns an array containing the host key elements.
     * @param encodedOpenSSHKey a base 64 encoded public key
     * @return An array containing the host key elements [algorithm-type base64-encoded-key optional-comment].
     */
    private String[] getHostKeyElements(final String encodedOpenSSHKey) {
        return new String(decodeBase64(encodedOpenSSHKey)).trim().split(" ");
    }

}
