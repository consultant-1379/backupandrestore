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
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.exception;

/**
 * To be used when validating ExternalClientProperties.
 */
public class InvalidExternalClientProperties extends RuntimeException {

    private static final long serialVersionUID = -3682851594947437751L;

    /**
     * Creates InvalidExternalClientProperties exception.
     * @param message error message.
     */
    public InvalidExternalClientProperties(final String message) {
        super(message);
    }

}
