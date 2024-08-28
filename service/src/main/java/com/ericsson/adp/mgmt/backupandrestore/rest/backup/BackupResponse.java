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
package com.ericsson.adp.mgmt.backupandrestore.rest.backup;

import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.PersistedBackup;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Full JSON response of a backup.
 */
@JsonInclude(Include.NON_DEFAULT)
public class BackupResponse extends PersistedBackup {

    /**
     * Default constructor, to be used by Jackson.
     */
    public BackupResponse() {}

    /**
     * Creates response.
     * @param backup to be converted to JSON.
     */
    public BackupResponse(final Backup backup) {
        backupId = backup.getBackupId();
        backupManagerId = backup.getBackupManagerId();
        name = backup.getName();
        creationTime = DateTimeUtils.convertToString(backup.getCreationTime());
        creationType = backup.getCreationType();
        status = backup.getStatus();
        userLabel = backup.getUserLabel();
        softwareVersions = backup.getSoftwareVersions();
    }

    @Override
    @JsonProperty("id")
    public String getBackupId() {
        return super.getBackupId();
    }

    @Override
    @JsonIgnore
    public String getBackupManagerId() {
        return super.getBackupManagerId();
    }

}
