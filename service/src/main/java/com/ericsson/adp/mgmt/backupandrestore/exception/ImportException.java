/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
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
 * ImportException represents exception related to import action.
 */
public class ImportException extends RuntimeException {

    private static final long serialVersionUID = 5424568799682030266L;

    /**
     * Creates ImportException exception.
     *
     * @param message the error message
     */
    public ImportException(final String message) {
        super(message);
    }

    /**
     * Creates ImportException exception.
     *
     * @param message the error message
     * @param cause   the error cause
     */
    public ImportException(final String message, final Exception cause) {
        super(message, cause);
    }
}
