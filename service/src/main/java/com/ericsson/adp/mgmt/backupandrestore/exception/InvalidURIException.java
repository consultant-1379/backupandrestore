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
 * InvalidURIException represents exception if URI is invalid.
 */
public class InvalidURIException extends RuntimeException {

    private static final long serialVersionUID = -7312264096416128747L;

    /**
     * Creates InvalidURIException exception.
     *
     * @param message
     *            the error message
     */
    public InvalidURIException(final String message) {
        super("Invalid URI: " + message);
    }
}
