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
 *----------------------------------------------------------------------------*/

package com.ericsson.adp.mgmt.bro.api.agent;

import com.ericsson.adp.mgmt.bro.api.util.CertificateType;
import io.grpc.internal.GrpcUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible for holding agent configuration for networking and security.
 */
public class OrchestratorConnectionInformation {
    public static final int DEFAULT_REGISTRATION_ACK_TIMEOUT = 10;
    private static final int DEFAULT_MAX_INBOUND_MESSAGE_SIZE = GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;
    private static final Logger log = LogManager.getLogger(OrchestratorConnectionInformation.class);

    private final String host;
    private final int port;
    private String certificateAuthorityName;
    private String certificateAuthorityPath;
    private String clientCertificatePath;
    private String clientPrivKeyPath;
    private final boolean tlsEnabled;
    private final boolean mTlsEnabled;
    private final int maxInboundMessageSize;
    private final int numberSecondsWaitForACK;

    private Map<String, CertificateType> originalCertificatePaths = new HashMap<>();

    /**
     * Creates orchestrator connection information based on host and port.
     *
     * @param host address of orchestrator.
     * @param port address of orchestrator.
     */
    public OrchestratorConnectionInformation(final String host, final int port) {
        this(host, port, DEFAULT_MAX_INBOUND_MESSAGE_SIZE, DEFAULT_REGISTRATION_ACK_TIMEOUT);
    }

    /**
     * Creates orchestrator connection information based on host and port.
     *
     * @param host address of orchestrator.
     * @param port address of orchestrator.
     * @param maxInboundMessageSize the maximum size in bytes of messages sent from the orchestrator to the agent. This
     *                              is important for agents that expect to send a large number of fragments (in the
     *                              thousands) with a large amount of metadata per fragment, as a single message
     *                              containing a list of all fragments information for each agent is sent to each agent
     *                              on restore.
     * @param numberSecondsWaitForACK Maximum number of seconds to wait for a Registration ACK from Server
     */
    public OrchestratorConnectionInformation(final String host, final int port, final int maxInboundMessageSize,
            final int numberSecondsWaitForACK) {
        this.host = host;
        this.port = port;
        this.tlsEnabled = false;
        this.mTlsEnabled = false;
        this.maxInboundMessageSize = maxInboundMessageSize;
        this.numberSecondsWaitForACK = numberSecondsWaitForACK;
    }

    /**
     * Creates orchestrator connection information based on host and port.
     *
     * @param host address of orchestrator.
     * @param port address of orchestrator.
     * @param maxInboundMessageSize the maximum size in bytes of messages sent from the orchestrator to the agent. This
     *                              is important for agents that expect to send a large number of fragments (in the
     *                              thousands) with a large amount of metadata per fragment, as a single message
     *                              containing a list of all fragments information for each agent is sent to each agent
     *                              on restore.
     */
    public OrchestratorConnectionInformation(final String host, final int port, final int maxInboundMessageSize) {
        this (host, port, maxInboundMessageSize, DEFAULT_REGISTRATION_ACK_TIMEOUT);
    }

    /**
     * Creates orchestrator connection information based on host and port
     * and tls information for enable/disable/ and certificate authority name and path
     *
     * @param host address of orchestrator.
     * @param port address of orchestrator.
     * @param certificateAuthorityName name of the certificate authority
     * @param certificateAuthorityPath path to the certificate authority file
     */
    public OrchestratorConnectionInformation(final String host, final int port, final String certificateAuthorityName,
                                             final String certificateAuthorityPath) {
        this(host, port, certificateAuthorityName, certificateAuthorityPath, DEFAULT_MAX_INBOUND_MESSAGE_SIZE, DEFAULT_REGISTRATION_ACK_TIMEOUT);
    }

    /**
     * Creates orchestrator connection information based on host and port
     * and tls information for enable/disable/ and certificate authority name and path
     *
     * @param host address of orchestrator.
     * @param port address of orchestrator.
     * @param certificateAuthorityName name of the certificate authority
     * @param certificateAuthorityPath path to the certificate authority file
     * @param maxInboundMessageSize the maximum size in bytes of messages sent from the orchestrator to the agent. This
     *                              is important for agents that expect to send a large number of fragments (in the
     *                              thousands) with a large amount of metadata per fragment, as a single message
     *                              containing a list of all fragments information for each agent is sent to each agent
     *                              on restore.
     * @param numberSecondsWaitForACK Maximum number of seconds to wait for a Registration ACK from Server
     */
    public OrchestratorConnectionInformation(final String host, final int port, final String certificateAuthorityName,
                                             final String certificateAuthorityPath, final int maxInboundMessageSize,
                                             final int numberSecondsWaitForACK) {
        this.host = host;
        this.port = port;
        this.certificateAuthorityName = certificateAuthorityName;
        this.certificateAuthorityPath = certificateAuthorityPath;
        this.tlsEnabled = true;
        this.mTlsEnabled = false;
        this.maxInboundMessageSize = maxInboundMessageSize;
        this.numberSecondsWaitForACK = numberSecondsWaitForACK;
        this.originalCertificatePaths = getMapValidPaths();
    }

    /**
     * Creates orchestrator connection information based on host and port
     * and tls information for enable/disable/ and certificate authority name and path
     *
     * @param host address of orchestrator.
     * @param port address of orchestrator.
     * @param certificateAuthorityName name of the certificate authority
     * @param certificateAuthorityPath path to the certificate authority file
     * @param maxInboundMessageSize the maximum size in bytes of messages sent from the orchestrator to the agent. This
     *                              is important for agents that expect to send a large number of fragments (in the
     *                              thousands) with a large amount of metadata per fragment, as a single message
     *                              containing a list of all fragments information for each agent is sent to each agent
     *                              on restore.
     */
    public OrchestratorConnectionInformation(final String host, final int port, final String certificateAuthorityName,
                                             final String certificateAuthorityPath, final int maxInboundMessageSize) {
        this (host, port, certificateAuthorityName, certificateAuthorityPath, maxInboundMessageSize, DEFAULT_REGISTRATION_ACK_TIMEOUT);
    }
    /**
     * Creates orchestrator connection information based on host and port
     * and mTLS information for enable/disable/ and certificate authority name and path
     *
     * @param host address of orchestrator.
     * @param port address of orchestrator.
     * @param certificateAuthorityName name of the certificate authority
     * @param certificateAuthorityPath path to the certificate authority file
     * @param clientCertificatePath path to the client certificatey file
     * @param clientPrivKeyPath path to the client private key file
     */
    public OrchestratorConnectionInformation(final String host, final int port, final String certificateAuthorityName,
                                             final String certificateAuthorityPath, final String clientCertificatePath,
                                             final String clientPrivKeyPath) {
        this(
            host,
            port,
            certificateAuthorityName,
            certificateAuthorityPath,
            clientCertificatePath,
            clientPrivKeyPath,
            DEFAULT_MAX_INBOUND_MESSAGE_SIZE,
            DEFAULT_REGISTRATION_ACK_TIMEOUT);
    }

    /**
     * Creates orchestrator connection information based on host and port
     * and mTLS information for enable/disable and certificate authority name and path
     *
     * @param host address of orchestrator.
     * @param port address of orchestrator.
     * @param certificateAuthorityName name of the certificate authority
     * @param certificateAuthorityPath path to the certificate authority file
     * @param clientCertificatePath path to the client certificatey file
     * @param clientPrivKeyPath path to the client private key file
     *@param maxInboundMessageSize the maximum size in bytes of messages sent from the orchestrator to the agent. This
     *                             is important for agents that expect to send a large number of fragments (in the
     *                             thousands) with a large amount of metadata per fragment, as a single message
     *                             containing a list of all fragments information for each agent is sent to each agent
     *                             on restore.
     */
    public OrchestratorConnectionInformation(final String host, final int port, final String certificateAuthorityName,
                                             final String certificateAuthorityPath, final String clientCertificatePath,
                                             final String clientPrivKeyPath, final int maxInboundMessageSize) {
        this(host, port, certificateAuthorityName, certificateAuthorityPath,
                clientCertificatePath, clientPrivKeyPath, maxInboundMessageSize, DEFAULT_REGISTRATION_ACK_TIMEOUT);
    }
    /**
     * Creates orchestrator connection information based on host and port
     * and mTLS information for enable/disable and certificate authority name and path
     *
     * @param host address of orchestrator.
     * @param port address of orchestrator.
     * @param certificateAuthorityName name of the certificate authority
     * @param certificateAuthorityPath path to the certificate authority file
     * @param clientCertificatePath path to the client certificatey file
     * @param clientPrivKeyPath path to the client private key file
     *@param maxInboundMessageSize the maximum size in bytes of messages sent from the orchestrator to the agent. This
     *                             is important for agents that expect to send a large number of fragments (in the
     *                             thousands) with a large amount of metadata per fragment, as a single message
     *                             containing a list of all fragments information for each agent is sent to each agent
     *                             on restore.
     * @param numberSecondsWaitForACK Maximum number of seconds to wait for a Registration ACK from Server
     */
    public OrchestratorConnectionInformation(final String host, final int port, final String certificateAuthorityName,
                                             final String certificateAuthorityPath, final String clientCertificatePath,
                                             final String clientPrivKeyPath, final int maxInboundMessageSize,
                                             final int numberSecondsWaitForACK) {
        this.host = host;
        this.port = port;
        this.certificateAuthorityName = certificateAuthorityName;
        this.certificateAuthorityPath = certificateAuthorityPath;
        this.clientCertificatePath = clientCertificatePath;
        this.clientPrivKeyPath = clientPrivKeyPath;
        this.tlsEnabled = true;
        this.mTlsEnabled = true;
        this.maxInboundMessageSize = maxInboundMessageSize;
        this.numberSecondsWaitForACK = numberSecondsWaitForACK;
        this.originalCertificatePaths = getMapValidPaths();
    }

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public boolean isMTlsEnabled() {
        return mTlsEnabled;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getCertificateAuthorityName() {
        return certificateAuthorityName;
    }

    public String getCertificateAuthorityPath() {
        return certificateAuthorityPath;
    }

    public Map<String, CertificateType> getOriginalCertificatePaths() {
        return originalCertificatePaths;
    }

    public String getClientCertificatePath() {
        return clientCertificatePath;
    }

    public String getClientPrivKeyPath() {
        return clientPrivKeyPath;
    }


    public void setCertificateAuthorityPath(final String certificateAuthorityPath) {
        this.certificateAuthorityPath = certificateAuthorityPath;
    }

    public void setClientCertificatePath(final String clientCertificatePath) {
        this.clientCertificatePath = clientCertificatePath;
    }

    public void setClientPrivKeyPath(final String clientPrivKeyPath) {
        this.clientPrivKeyPath = clientPrivKeyPath;
    }

    public int getMaxInboundMessageSize() {
        return maxInboundMessageSize;
    }

    public int getNumberSecondsWaitForACK() {
        return numberSecondsWaitForACK;
    }

    /**
     * Get the list of cert or key paths that are valid for this connection, i.e. an empty list
     * if the connection is plaintext, just the CA if it's TLS and the CA, Client cert and key
     * paths if it's mTLS
     * @return the list of cert or key paths that are valid (required) for this connection.
     * */
    public List<String> getValidPaths() {
        final List<String> result = new ArrayList<>();
        if (tlsEnabled) {
            result.add(certificateAuthorityPath);
        }
        if (mTlsEnabled) {
            if (clientCertificatePath == null) {
                log.error("The expected client certificate path is null");
            } else {
                result.add(clientCertificatePath);
            }
            if (clientPrivKeyPath == null) {
                log.error("The expected private key path is null");
            } else {
                result.add(clientPrivKeyPath);
            }
        }
        return result;
    }
    /**
     * Get the map of cert or key paths with type that are valid for this connection, i.e. an empty list
     * if the connection is plaintext, just the CA if it's TLS and the CA, Client cert and key
     * paths if it's mTLS
     * @return the map of cert or key paths that are valid (required) for this connection.
     * */
    private Map<String, CertificateType> getMapValidPaths() {
        final Map<String, CertificateType> result = new HashMap<>();
        if (tlsEnabled) {
            log.info("CA path {}", certificateAuthorityPath);
            result.put(certificateAuthorityPath, CertificateType.CERTIFICATE_AUTHORITY);
        }
        if (mTlsEnabled) {
            if (clientCertificatePath == null) {
                log.error("The expected client certificate path is null");
            } else {
                log.info("Client path {}", clientCertificatePath);
                result.put(clientCertificatePath, CertificateType.PUBLIC_KEY);
            }
            if (clientPrivKeyPath == null) {
                log.error("The expected private key path is null");
            } else {
                log.info("PK path {}", clientPrivKeyPath);
                result.put(clientPrivKeyPath, CertificateType.PRIVATE_KEY);
            }
        }
        return result;
    }
}
