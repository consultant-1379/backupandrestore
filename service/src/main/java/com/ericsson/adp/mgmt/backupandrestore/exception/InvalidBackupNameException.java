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
 * InvalidBackupNameException represents exception for backup name validation.
 */
public class InvalidBackupNameException extends UnprocessableEntityException {

    private static final long serialVersionUID = 6667233444098148038L;

    /**
     * Creates InvalidBackupNameException exception.
     *
     * @param reason
     *            explaining what happened.
     */
    public InvalidBackupNameException(final String reason) {
        super(reason);
    }

}