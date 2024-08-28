/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler;

/**
 * Holds SchedulerConstants for general utility.
 */
public enum SchedulerConstants {

    DEFAULT_SCHEDULED_BACKUP_NAME("SCHEDULED_BACKUP"),
    SCHEDULER("scheduler"),
    ADMIN_STATE("admin-state"),
    AUTO_EXPORT("auto-export"),
    AUTO_EXPORT_URI("auto-export-uri"),
    AUTO_EXPORT_PASSWORD("auto-export-password"),
    SCHEDULED_BACKUP_NAME("scheduled-backup-name"),
    SFTP_SERVER_NAME("sftp-server-name");

    private final String stringRepresentation;

    /**
     * @param stringRepresentation string constants.
     */
    SchedulerConstants(final String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    /**
     * Get String representation of enum
     * @return String representation of enum
     */
    @Override
    public String toString() {
        return stringRepresentation;
    }
}
