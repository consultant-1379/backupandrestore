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
package com.ericsson.adp.mgmt.backupandrestore.backup;

import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Represents Backup to be persisted.
 */
public class PersistedBackup extends BackupInformation implements Versioned<PersistedBackup> {

    protected String creationTime;
    private Version<PersistedBackup> version;

    /**
     * Constructor used by jackson.
     */
    public PersistedBackup() {
    }

    /**
     * Creates PersistedBackup based on Backup.
     *
     * @param backup
     *            -the backup to be persisted.
     */
    public PersistedBackup(final Backup backup) {
        backupId = backup.getBackupId();
        backupManagerId = backup.getBackupManagerId();
        name = backup.getName();
        creationTime = DateTimeUtils.convertToString(backup.getCreationTime().toLocalDateTime());
        creationType = backup.getCreationType();
        status = backup.getStatus();
        userLabel = backup.getUserLabel();
        softwareVersions = backup.getSoftwareVersions();
        this.version = backup.getVersion();
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(final String creationTime) {
        this.creationTime = creationTime;
    }

    public void setBackupId(final String backupId) {
        this.backupId = backupId;
    }

    public void setBackupManagerId(final String backupManagerId) {
        this.backupManagerId = backupManagerId;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setCreationType(final BackupCreationType creationType) {
        this.creationType = creationType;
    }

    public void setSoftwareVersions(final List<SoftwareVersion> softwareVersions) {
        this.softwareVersions = softwareVersions;
    }

    @Override
    @JsonIgnore
    public Version<PersistedBackup> getVersion() {
        return version;
    }

    @Override
    @JsonIgnore
    public void setVersion(final Version<PersistedBackup> version) {
        this.version = version;
    }
}
