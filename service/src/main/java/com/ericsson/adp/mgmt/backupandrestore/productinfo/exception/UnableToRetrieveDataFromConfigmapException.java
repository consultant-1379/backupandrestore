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
package com.ericsson.adp.mgmt.backupandrestore.productinfo.exception;

/**
 * Represents an error while trying to retrieve Product Information or
 * Product Matching Criteria from Configmap
 */
public class UnableToRetrieveDataFromConfigmapException extends RuntimeException {

    private static final long serialVersionUID = 5738291595959555991L;

    /**
     * Creates exception.
     * @param message explaining reason for exception
     */
    public UnableToRetrieveDataFromConfigmapException(final String message) {
        super(message);
    }

    /**
     * Creates exception.
     * @param message explaining reason for exception
     * @param cause Throwable cause
     */
    public UnableToRetrieveDataFromConfigmapException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
