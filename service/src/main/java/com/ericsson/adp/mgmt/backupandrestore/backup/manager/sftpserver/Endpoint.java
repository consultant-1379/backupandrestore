/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver;

import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.NAME;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.REMOTE_ADDRESS;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.REMOTE_PATH;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.REMOTE_PORT;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Endpoint defines the properties of an SFTP Server connection
 * used for authentication
 */
public class Endpoint {
    private String name;

    @JsonProperty("remote-address")
    private String remoteAddress;

    @JsonProperty("remote-port")
    private int remotePort;

    @JsonProperty("remote-path")
    private String remotePath;

    @JsonProperty("client-identity")
    private ClientIdentity clientIdentity;

    @JsonProperty("server-authentication")
    private ServerAuthentication serverAuthentication;

    /**
     * Default constructor, to be used by Jackson.
     */
    public Endpoint() {
    }

    /**
     * Constructor for the SFTP server endpoint definition
     * @param name the name of the SFTP Server endpoint
     * @param remoteAddress the remote IP address
     * @param remotePort the remote SFTP port
     * @param remotePath the remote path or directory in the SFTP server
     * @param clientIdentity the credentials used by the client to authenticate to the SSH server.
     * @param serverAuthentication the specification of how the SSH client can authenticate SSH servers
     */
    public Endpoint(final String name, final String remoteAddress, final int remotePort, final String remotePath, final ClientIdentity clientIdentity,
                    final ServerAuthentication serverAuthentication) {
        this.name = name;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.remotePath = remotePath;
        this.clientIdentity = clientIdentity;
        this.serverAuthentication = serverAuthentication;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(final String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(final int remotePort) {
        this.remotePort = remotePort;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(final String remotePath) {
        this.remotePath = remotePath;
    }

    public ClientIdentity getClientIdentity() {
        return clientIdentity;
    }

    public void setClientIdentity(final ClientIdentity clientIdentity) {
        this.clientIdentity = clientIdentity;
    }

    public ServerAuthentication getServerAuthentication() {
        return serverAuthentication;
    }

    public void setServerAuthentication(final ServerAuthentication serverAuthentication) {
        this.serverAuthentication = serverAuthentication;
    }

    @Override
    public String toString() {
        return "Endpoint [name=" + name + ", remoteAddress=" + remoteAddress + ", remotePort=" + remotePort + ", remotePath=" + remotePath
                + ", clientIdentity=" + clientIdentity + ", serverAuthentication=" + serverAuthentication + "]";
    }

    /**
     * Updates the property of the endpoint based on the passed yang model property
     * @param updatedProperty the YANG model property name
     * @param newValue the new value of the property
     */
    public void updateProperty(final String updatedProperty, final String newValue) {
        if (updatedProperty.equalsIgnoreCase(REMOTE_ADDRESS.toString())) {
            this.remoteAddress = newValue;
        } else if (updatedProperty.equalsIgnoreCase(REMOTE_PATH.toString())) {
            this.remotePath = newValue;
        } else if (updatedProperty.equalsIgnoreCase(REMOTE_PORT.toString())) {
            this.remotePort = Integer.valueOf(newValue);
        } else if (updatedProperty.equalsIgnoreCase(NAME.toString())) {
            this.name = newValue;
        } else {
            clientIdentity.updateProperty(updatedProperty, newValue);
        }
    }
}