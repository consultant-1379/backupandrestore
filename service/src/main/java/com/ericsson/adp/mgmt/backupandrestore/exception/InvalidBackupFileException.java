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
package com.ericsson.adp.mgmt.backupandrestore.exception;

/**
 * InvalidBackupFileException represents exception for backup file validation.
 */
public class InvalidBackupFileException extends RuntimeException {
    private static final long serialVersionUID = 7323261499029094916L;

    /**
     * Creates InvalidBackupFileException exception.
     *
     * @param reason
     *            explaining what happened.
     */
    public InvalidBackupFileException(final String reason) {
        super("Invalid backup file:: " + reason);
    }

}
