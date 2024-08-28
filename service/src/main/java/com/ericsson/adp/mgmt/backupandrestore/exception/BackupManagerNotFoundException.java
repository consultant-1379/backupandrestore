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
 * To be thrown when not finding a specific backupManager.
 */
public class BackupManagerNotFoundException extends NotFoundException {

    private static final long serialVersionUID = 7040588200188955984L;

    /**
     * Creates exception.
     * @param backupManagerId that was not found.
     */
    public BackupManagerNotFoundException(final String backupManagerId) {
        super("Request was unsuccessful as Backup Manager <" + backupManagerId + "> was not found");
    }

}
