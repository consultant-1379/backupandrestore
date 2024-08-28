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
 * ExportException represents exception related to export action.
 */
public class ExportException extends RuntimeException {

    private static final long serialVersionUID = 7979880068250043969L;

    /**
     * Creates ExportException exception.
     *
     * @param message the error message
     */
    public ExportException(final String message) {
        super(message);
    }

    /**
     * Creates ExportException exception.
     *
     * @param message the error message
     * @param cause   the error cause
     */
    public ExportException(final String message, final Exception cause) {
        super(message, cause);
    }
}
