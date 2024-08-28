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
 * Represents error if a backup with non existent backupId is requested.
 */
public class BackupNotFoundException extends NotFoundException {

    private static final long serialVersionUID = -5614125679797100435L;

    /**
     * Creates exception.
     *
     * @param backupId
     *            id of backup.
     */
    public BackupNotFoundException(final String backupId) {
        super("Backup <" + backupId + "> not found");
    }

}
