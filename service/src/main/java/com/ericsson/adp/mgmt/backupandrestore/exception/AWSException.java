/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
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
 * Generic S3 Client exception
 *
 */
public class AWSException extends RuntimeException {

    private static final long serialVersionUID = 6221754135031565830L;

    /**
     * S3 Client Generic Exception
     * @param message to be displayed
     */
    public AWSException(final String message) {
        super(message);
    }

    /**
     * S3 Creates exception.
     * @param message explaining what happened.
     * @param cause what caused it.
     */
    public AWSException(final String message, final Throwable cause) {
        super(message, cause);
    }
}