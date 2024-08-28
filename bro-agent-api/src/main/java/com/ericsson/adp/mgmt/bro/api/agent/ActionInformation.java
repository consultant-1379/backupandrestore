/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.bro.api.agent;

import com.ericsson.adp.mgmt.control.Preparation;

/**
 * Holds action information
 */
public class ActionInformation {

    private final String backupName;
    private final String backupType;

    /**
     * @param backupName Name of backup to be restored
     * @param backupType The type of the backup to be restored
     */
    public ActionInformation(final String backupName, final String backupType) {
        this.backupName = backupName;
        this.backupType = backupType;
    }

    /**
     * @param preparation message.
     */
    public ActionInformation(final Preparation preparation) {
        this.backupName = preparation.getBackupName();
        this.backupType = preparation.getBackupType();
    }

    public String getBackupName() {
        return backupName;
    }

    public String getBackupType() {
        return backupType;
    }

}
