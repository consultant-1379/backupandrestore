/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
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
 * Invalid Number of Backups are allowed to be entered
 *
 */
public class InvalidNumberOfBackupsAllowed extends UnprocessableEntityException {

    private static final long serialVersionUID = 7777233444098148038L;

    /**
     * Creates InvalidNumberOfBackupsAllowed exception.
     *
     * @param reason
     *            explaining what happened.
     */
    public InvalidNumberOfBackupsAllowed(final String reason) {
        super(reason);
    }
}
