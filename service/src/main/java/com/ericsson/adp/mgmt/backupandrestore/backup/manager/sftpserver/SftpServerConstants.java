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

/**
 * Utility constants for SFTP Server properties
 */
public enum SftpServerConstants{
    CLIENT_USERNAME("username"),
    CLIENT_LOCAL_PRIVATE_KEY("private-key"),
    CLIENT_LOCAL_PUBLIC_KEY("public-key"),
    NAME("name"),
    REMOTE_ADDRESS("remote-address"),
    REMOTE_PATH("remote-path"),
    REMOTE_PORT("remote-port"),
    SERVER_LOCAL_HOST_KEY("host-key"),
    SFTP_SERVER("sftp-server");

    private String value;

    /**
     * Utility constants for SFTP Server properties
     * @param value the String representation of the SFTP server property
     */
    SftpServerConstants(final String value) {
        this.value = value;
    }

    /**
     * Get the value of the SFTP property
     * @return String representation of the SFTP server property
     */
    @Override
    public String toString() {
        return this.value;
    }
}
