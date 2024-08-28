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
package com.ericsson.adp.mgmt.backupandrestore.backup;

/**
 * Indicates the state of the backup.
 */
public enum BackupStatus {

    COMPLETE("backup-complete", 0), INCOMPLETE("backup-incomplete", 1), CORRUPTED("backup-corrupted", 2);

    private String cmRepresentation;
    private int precedence;

    /**
     * Creates status
     * @param cmRepresentation how CM expects it.
     * @param precedence of the backup if try to be deleted.
     */
    BackupStatus(final String cmRepresentation, final int precedence) {
        this.cmRepresentation = cmRepresentation;
        this.precedence = precedence;
    }

    public String getCmRepresentation() {
        return cmRepresentation;
    }

    /**
     * Returns the precedence of a backup if needs to be deleted.
     * @return precedence of the backup to be deleted.
     */
    public int getPrecedence() {
        return precedence;
    }

}
