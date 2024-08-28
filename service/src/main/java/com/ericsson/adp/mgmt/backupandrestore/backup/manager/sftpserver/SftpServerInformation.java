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

import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Represents an SFTP Server configured through the CMYP CLI
 */
public class SftpServerInformation implements Versioned<SftpServerInformation> {
    protected String name;
    protected Endpoints endpoints;
    private Version<SftpServerInformation> version;

    /**
     * Default constructor, to be used by Jackson.
     */
    public SftpServerInformation() {
    }

    /**
     * Represents an SFTPServerInformation read from or pushed to CM.
     * @param name an arbitrary name for the SFTP server
     * @param endpoints container for the list of endpoints
     */
    public SftpServerInformation(final String name, final Endpoints endpoints) {
        this.name = name;
        this.endpoints = endpoints;
    }

    /**
     * Creates an instance of the SFTPServerInformation from an SFTPServer instance
     * in the backup manager.
     * @param sftpServer the SFTP server in the backupmanager
     */
    public SftpServerInformation(final SftpServer sftpServer) {
        this(sftpServer.getName(), sftpServer.getEndpoints());
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Endpoints getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(final Endpoints endpoints) {
        this.endpoints = endpoints;
    }

    @Override
    @JsonIgnore
    public Version<SftpServerInformation> getVersion() {
        return version;
    }

    @Override
    @JsonIgnore
    public void setVersion(final Version<SftpServerInformation> version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "SftpServerInformation [name=" + name + ", endpoints=" + endpoints + ", version=" + version + "]";
    }

}
