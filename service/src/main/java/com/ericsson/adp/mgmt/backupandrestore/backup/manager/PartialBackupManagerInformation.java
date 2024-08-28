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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager;

/**
 * Holds specific backupManager information.
 */
public abstract class PartialBackupManagerInformation {

    protected String backupType;
    protected String backupDomain;

    public String getBackupType() {
        return backupType;
    }

    public void setBackupType(final String backupType) {
        this.backupType = backupType;
    }

    public String getBackupDomain() {
        return backupDomain;
    }

    public void setBackupDomain(final String backupDomain) {
        this.backupDomain = backupDomain;
    }

}
