/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */
package com.ericsson.adp.mgmt.backupandrestore.exception;

/**
 * Represents something wrong happened when dealing with CM Mediator.
 */
public class CMMediatorException extends RuntimeException {

    private static final long serialVersionUID = 2261358519343839032L;

    /**
     * Creates Exception
     * @param message - the error message.
     */
    public CMMediatorException(final String message) {
        super(message);
    }

    /**
     * Creates exception.
     * @param message explaining what happened.
     * @param cause what caused it.
     */
    public CMMediatorException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
