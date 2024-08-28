/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.action.yang;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Input sftp-server-name.
 */
public class YangSftpServerNameInput implements YangSftpServerInput {

    @JsonProperty("ericsson-brm:sftp-server-name")
    private String sftpServerName;

    @JsonProperty("ericsson-brm:backup-path")
    private String backupPath;

    /**
     * Empty constructor required for Jackson deserialization
     */
    public YangSftpServerNameInput() {
    }

    /**
     * Create an instance of YangSftpServerNameInput
     * @param sftpServerName the sftp-server-name input
     */
    public YangSftpServerNameInput(final String sftpServerName) {
        super();
        this.sftpServerName = sftpServerName;
    }

    /**
     * Create an instance of YangSftpServerNameInput
     * @param sftpServerName the sftp-server-name input
     * @param backupPath the path to the backup name or tarball name relative to remote-path
     */
    public YangSftpServerNameInput(final String sftpServerName, final String backupPath) {
        this(sftpServerName);
        this.backupPath = backupPath;
    }

    public String getSftpServerName() {
        return sftpServerName;
    }

    public void setSftpServerName(final String sftpServerName) {
        this.sftpServerName = sftpServerName;
    }

    public final String getBackupPath() {
        return backupPath;
    }

    public final void setBackupPath(final String backupPath) {
        this.backupPath = backupPath;
    }
}
