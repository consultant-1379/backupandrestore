/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup;

/**
 * Indicates how the backup is being triggered.
 */
public enum BackupCreationType {

    MANUAL("manual"), SCHEDULED("scheduled"), SYSTEM_CREATED("system-created");

    private String cmRepresentation;

    /**
     * Creates creationType.
     * @param cmRepresentation how CM expects it.
     */
    BackupCreationType(final String cmRepresentation) {
        this.cmRepresentation = cmRepresentation;
    }

    public String getCmRepresentation() {
        return cmRepresentation;
    }

}
