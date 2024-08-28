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

import java.net.URI;
import java.util.function.Consumer;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Holds the sftp server information of a backup manager.
 */
public class SftpServer extends SftpServerInformation{
    private static final String SFTP_SCHEME = "sftp";
    private final String backupManagerId;
    private final Consumer<SftpServer> persistFunction;

    /**
     * Creates an SFTPServer through the SFTPServerInformation read from CM notification.
     * @param sftpServerInfo the SFTPServer connection information
     * @param backupManagerId the id of the SFTPServer's backup manager
     * @param persistFunction the function for persisting the SFTPServer
     */
    public SftpServer(final SftpServerInformation sftpServerInfo, final String backupManagerId, final Consumer<SftpServer> persistFunction) {
        this.backupManagerId = backupManagerId;
        this.persistFunction = persistFunction;
        this.name = sftpServerInfo.name;
        this.endpoints = sftpServerInfo.endpoints;
    }

    /**
     * Persists the sftp server.
     */
    public void persist() {
        persistFunction.accept(this);
    }

    public String getBackupManagerId() {
        return backupManagerId;
    }

    public Consumer<SftpServer> getPersistFunction() {
        return persistFunction;
    }

    /**
     * Retrieves the URI containing the remote path in the sftp server
     * @return the URI containing the remote path in the sftp server
     */
    public URI getURI() {
        final Endpoint endpoint = getEndpoints().getEndpoint()[0];
        return UriComponentsBuilder.newInstance()
                .scheme(SFTP_SCHEME)
                .userInfo(endpoint.getClientIdentity().getUsername())
                .host(endpoint.getRemoteAddress())
                .port(endpoint.getRemotePort())
                .path(endpoint.getRemotePath())
                .build()
                .toUri();
    }

}
