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
package com.ericsson.adp.mgmt.backupandrestore.external;

import java.net.URI;
import java.net.URISyntaxException;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServer;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidURIException;

/**
 * ExternalClientProperties contains uri details. This class is used in import and export operations.
 */
public abstract class ExternalClientProperties {
    private static final String URI_SCHEME_HTTP = "http";
    protected URI uri;
    private final String password;
    private final SftpServer sftpServer;

    /**
     *
     * ExternalClientProperties holds uri properties and password
     *
     * @param uri
     *            uri
     * @param password
     *            password
     *
     */
    public ExternalClientProperties(final String uri, final String password) {

        try {
            this.uri = new URI(uri);
        } catch (final URISyntaxException e) {
            throw new InvalidURIException("Failed due to invalid URI syntax exception:: " + e.getMessage());
        }

        this.password = password;
        this.sftpServer = null;
    }

    /**
     * ExternalClientProperties holding sftpServer
     * @param sftpServer an instance of an SftpServer
     */
    public ExternalClientProperties(final SftpServer sftpServer) {
        try {
            this.uri = new URI("");
        } catch (final URISyntaxException e) {
            throw new InvalidURIException("Failed due to invalid URI syntax exception:: " + e.getMessage());
        }
        this.password = null;
        this.sftpServer = sftpServer;
    }

    public URI getUri() {
        return uri;
    }

    /**
     * Checks if the URI is using Http scheme.
     * @return true if URI scheme is http,
     *         false if the URI is null or is using another scheme.
     */
    public boolean isUsingHttpUriScheme() {
        return uri != null && uri.getScheme() != null && uri.getScheme().equals(URI_SCHEME_HTTP);
    }

    public String getUser() {
        return uri.getUserInfo();
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return uri.getHost();
    }

    public int getPort() {
        return uri.getPort();
    }

    public String getExternalClientPath() {
        return uri.getPath();
    }

    public SftpServer getSftpServer() {
        return sftpServer;
    }

}
