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
 * Exception thrown when request to KMS fails
 * */
public class KMSRequestFailedException extends RuntimeException {

    /**
     * Default constructor
     * */
    public KMSRequestFailedException() {
        super();
    }

    /**
     * Default constructor
     * @param message - message
     * */
    public KMSRequestFailedException(final String message) {
        super(message);
    }

    /**
     * Default constructor
     * @param message - message
     * @param cause - cause of exception
     * */
    public KMSRequestFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
