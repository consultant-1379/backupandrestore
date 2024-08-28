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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Payload for importing a backup.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportPayload extends SftpServerPayload implements Payload {

    protected String backupPath;

    /**
     * Empty constructor required for jackson deserialization
     */
    public ImportPayload() {
    }

    /**
     * Create an instance of import payload using
     * the sftp-server uri and password input
     * @param uri the uri of the remote sftp-server path
     * @param password the sftp-server password
     */
    public ImportPayload(final URI uri, final String password) {
        this.uri = uri;
        this.password = password;
    }

    /**
     * Create an instance of import payload using sftp-server-name input
     * @param backupPath the backup name
     * @param sftpServerName the sftp-server-name
     */
    public ImportPayload(final String backupPath, final String sftpServerName) {
        this.backupPath = backupPath;
        this.sftpServerName = sftpServerName;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(final String backupPath) {
        this.backupPath = backupPath;
    }

    @Override
    public String toString() {
        return "ImportPayload [uri=" + uri + ", sftpServerName=" + sftpServerName + ", backupName=" + backupPath + "]";
    }
}
