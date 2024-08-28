/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
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
 * Represents something could not be processed due to semantic reasons.
 */
public class UnprocessableEntityException extends RuntimeException {

    private static final long serialVersionUID = -1455895258733062236L;

    /**
     * Creates exception.
     * @param message explaining what happened.
     */
    public UnprocessableEntityException(final String message) {
        super(message);
    }

    /**
     * Creates exception.
     *
     * @param message explaining what happened.
     * @param cause The underlying exception
     */
    public UnprocessableEntityException(final String message, final Exception cause) {
        super(message, cause);
    }

}
