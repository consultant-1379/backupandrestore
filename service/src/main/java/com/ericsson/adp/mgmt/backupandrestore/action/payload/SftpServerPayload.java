/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.action.payload;

import java.net.URI;

/**
 * Represents a payload that needs uri and password to get/send files from.
 */
public abstract class SftpServerPayload {

    protected URI uri;
    protected String password;
    protected String sftpServerName;

    public URI getUri() {
        return uri;
    }

    public void setUri(final URI uri) {
        this.uri = uri;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getSftpServerName() {
        return sftpServerName;
    }

    public void setSftpServerName(final String sftpServerName) {
        this.sftpServerName = sftpServerName;
    }

    /**
     * Checks if the URI field of the payload has a value
     * @return true if the URI field is set and not empty, false otherwise.
     */
    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }

    /**
     * Checks if the payload has SftpServerName
     * @return true if the sftpServerName is set and not empty, false otherwise.
     */
    public boolean hasSftpServerName() {
        return sftpServerName != null && !sftpServerName.isEmpty();
    }
}
