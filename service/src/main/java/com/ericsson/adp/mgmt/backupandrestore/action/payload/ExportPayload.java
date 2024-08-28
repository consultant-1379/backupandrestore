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
 * Payload for exporting a backup.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExportPayload extends SftpServerPayload implements Payload {

    protected String backupName;

    /**
     * Empty constructor required for Jackson deserialization
     */
    public ExportPayload() {
    }

    /**
     * Create an instance of export payload using sftp-server URI input
     * @param backupName the backup name
     * @param uri the uri of the remote-path in the sftp server
     * @param password the sftp-server password
     */
    public ExportPayload(final String backupName, final URI uri, final String password) {
        this.backupName = backupName;
        this.uri = uri;
        this.password = password;
    }

    /**
     * Create an instance of export payload using sftp-server-name input
     * @param backupName the backup name
     * @param sftpServerName the sftp-server-name
     */
    public ExportPayload(final String backupName, final String sftpServerName) {
        this.backupName = backupName;
        this.sftpServerName = sftpServerName;
    }

    public String getBackupName() {
        return backupName;
    }

    public void setBackupName(final String backupName) {
        this.backupName = backupName;
    }

    @Override
    public String toString() {
        return "ExportPayload [uri=" + uri + ", sftpServerName=" + sftpServerName + ", backupName=" + backupName + "]";
    }

}
