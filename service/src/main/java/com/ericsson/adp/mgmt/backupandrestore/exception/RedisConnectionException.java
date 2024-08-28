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
 * Represents exception related to redis connection.
 */
public class RedisConnectionException extends RuntimeException{

    private static final long serialVersionUID = 4221697093810606795L;

    /**
     * creates exception.
     *
     * @param message
     *            - exception information
     */
    public RedisConnectionException(final String message) {
        super(message);
    }

    /**
     * Creates exception.
     * @param message - what happened.
     * @param cause - what caused it.
     */
    public RedisConnectionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
