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
 * Represents something wrong happened while deleting a backup.
 */
public class DeleteBackupException extends RuntimeException {

    private static final long serialVersionUID = 5743010802897584570L;

    /**
     * Creates exception.
     * @param message explaining what happened.
     */
    public DeleteBackupException(final String message) {
        super(message);
    }

    /**
     * Creates exception.
     * @param message explaining what happened.
     * @param cause of exception.
     */
    public DeleteBackupException(final String message, final Exception cause) {
        super(message, cause);
    }

}
