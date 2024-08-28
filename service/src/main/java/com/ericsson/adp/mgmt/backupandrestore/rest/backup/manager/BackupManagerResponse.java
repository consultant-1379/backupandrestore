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
package com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.FullBackupManagerInformation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON response representing one backupManager.
 *
 * Include.NON_NULL as non-vBRMs will have a null agent list and parent id
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BackupManagerResponse extends FullBackupManagerInformation {

    /**
     * Default constructor, to be used by Jackson.
     */
    public BackupManagerResponse() {}

    /**
     * Creates BackupManagerResponse from a BackupManager.
     * @param backupManager to use.
     */
    public BackupManagerResponse(final BackupManager backupManager) {
        this.backupManagerId = backupManager.getBackupManagerId();
        this.backupDomain = backupManager.getBackupDomain();
        this.backupType = backupManager.getBackupType();
    }

    @Override
    @JsonProperty("id")
    public String getBackupManagerId() {
        return backupManagerId;
    }

    public void setBackupManagerId(final String backupManagerId) {
        this.backupManagerId = backupManagerId;
    }

}
