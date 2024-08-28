package com.ericsson.adp.mgmt.brotestagent.exception;
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

/**
 * Represents an error has occurred during a backup
 */
public class FailedToCreateBackupException extends RuntimeException {

    private static final long serialVersionUID = 7399261108336358764L;

    /**
     * Creates exception with custom message.
     *
     * @param message
     *            The messages
     * @param exception
     *            What triggered the issue
     */
    public FailedToCreateBackupException(final String message, final Exception exception) {
        super(message, exception);
    }

}
