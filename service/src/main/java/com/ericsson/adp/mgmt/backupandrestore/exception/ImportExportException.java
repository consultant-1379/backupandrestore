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
 * ImportExportException represents exception if export or import request is invalid.
 */
public class ImportExportException extends RuntimeException {

    private static final long serialVersionUID = 2528225633352741225L;

    /**
     * Creates ImportExportException exception.
     *
     * @param message
     *            the error message
     */
    public ImportExportException(final String message) {
        super(message);
    }

    /**
     * Creates ImportExportException exception.
     *
     * @param message
     *            the error message
     * @param cause
     *            the error cause
     */
    public ImportExportException(final String message, final Exception cause) {
        super(message, cause);
    }
}
