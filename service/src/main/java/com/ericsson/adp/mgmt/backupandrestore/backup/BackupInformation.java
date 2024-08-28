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

import java.util.ArrayList;
import java.util.List;

/**
 * Stores information of a backup.
 */
public class BackupInformation {

    protected String backupId;
    protected String backupManagerId;
    protected String name;
    protected BackupCreationType creationType;
    protected BackupStatus status;
    protected String userLabel;
    protected List<SoftwareVersion> softwareVersions = new ArrayList<>();

    public String getBackupId() {
        return backupId;
    }

    public String getBackupManagerId() {
        return backupManagerId;
    }

    public String getName() {
        return name;
    }

    public BackupCreationType getCreationType() {
        return creationType;
    }

    public BackupStatus getStatus() {
        return status;
    }

    public void setStatus(final BackupStatus status) {
        this.status = status;
    }

    public String getUserLabel() {
        return userLabel;
    }

    public void setUserLabel(final String userLabel) {
        this.userLabel = userLabel;
    }

    public List<SoftwareVersion> getSoftwareVersions() {
        return softwareVersions;
    }

}
