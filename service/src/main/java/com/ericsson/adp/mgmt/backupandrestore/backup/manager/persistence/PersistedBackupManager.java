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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.FullBackupManagerInformation;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Information from a backupManager that should be written to a file.
 *
 * Include.NON_NULL as non-vBRMs will have a null agent list and parent id
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersistedBackupManager extends FullBackupManagerInformation implements Versioned<PersistedBackupManager> {

    private Version<PersistedBackupManager> version;

    /**
     * Default constructor, to be used by Jackson.
     */
    public PersistedBackupManager() {}

    /**
     * Creates persistedBackupManager from an existing backupManager.
     * @param backupManager to be persisted.
     */
    protected PersistedBackupManager(final BackupManager backupManager) {
        this.backupManagerId = backupManager.getBackupManagerId();
        this.backupType = backupManager.getBackupType();
        this.backupDomain = backupManager.getBackupDomain();
        this.version = backupManager.getVersion();
    }

    @Override
    @JsonProperty(value = "id")
    public String getBackupManagerId() {
        return backupManagerId;
    }

    public void setBackupManagerId(final String backupManagerId) {
        this.backupManagerId = backupManagerId;
    }

    @Override
    @JsonIgnore
    public Version<PersistedBackupManager> getVersion() {
        return version;
    }

    @Override
    @JsonIgnore
    public void setVersion(final Version<PersistedBackupManager> version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "PersistedBackupManager [version=" + version + ", backupManagerId=" + backupManagerId + ", backupType=" + backupType
                + ", backupDomain=" + backupDomain + "]";
    }

}
