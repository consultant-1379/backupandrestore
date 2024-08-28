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
 * PathNotFoundException represents exception for path validation.
 */
public class PathNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -7093637621798912300L;

    /**
     * Creates PathNotFoundException exception.
     *
     * @param message explaining what happened.
     */
    public PathNotFoundException(final String message) {
        super(message);
    }
}
