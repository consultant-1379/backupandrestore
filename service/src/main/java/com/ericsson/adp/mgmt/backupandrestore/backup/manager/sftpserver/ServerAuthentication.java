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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ServerAuthentication specifies how the SSH client can authenticate SSH servers
 */
public class ServerAuthentication {
    @JsonProperty("ssh-host-keys")
    private SSHHostKeys sshHostKeys;

    /**
     * Default constructor, to be used by Jackson.
     */
    public ServerAuthentication() {
    }

    /**
     * ServerAuthentication specifies how the SSH client can authenticate SSH servers
     * @param sshHostKeys the SSH host keys used by the SSH client to authenticate the SSH server
     */
    public ServerAuthentication(final SSHHostKeys sshHostKeys) {
        this.sshHostKeys = sshHostKeys;
    }

    public SSHHostKeys getSshHostKeys() {
        return sshHostKeys;
    }

    public void setSshHostKeys(final SSHHostKeys sshHostKeys) {
        this.sshHostKeys = sshHostKeys;
    }

    @Override
    public String toString() {
        return "ServerAuthentication [sshHostKeys=" + sshHostKeys + "]";
    }
}