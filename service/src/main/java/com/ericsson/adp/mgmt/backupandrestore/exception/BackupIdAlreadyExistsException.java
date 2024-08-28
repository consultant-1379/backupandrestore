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
 * Represents error if a backup with an already used backupId is requested.
 */
public class BackupIdAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = -590777128210204069L;

    /**
     * Creates exception.
     *
     * @param backupId
     *            which already exists.
     */
    public BackupIdAlreadyExistsException(final String backupId) {
        super("BackupId <" + backupId + "> already exists.");
    }

}
