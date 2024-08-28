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
package com.ericsson.adp.mgmt.backupandrestore.exception;

/**
 * Represents trying to import a backup from one backupManager into another.
 */
public class UnexpectedBackupManagerException extends RuntimeException {

    private static final long serialVersionUID = -5905029210326512238L;

    private final String backupId;

    /**
     * Creates exception.
     * @param expectedBackupManager expected backupManager.
     * @param backupManager backupManager from backup.
     * @param backupId the backupId of this action.
     */
    public UnexpectedBackupManagerException(final String expectedBackupManager, final String backupManager, final String backupId) {
        super("Failed to import backup belonging to backupManager <" + backupManager
                + "> into backupManager <" + expectedBackupManager + ">");
        this.backupId = backupId;
    }

    public String getBackupId() {
        return backupId;
    }
}
