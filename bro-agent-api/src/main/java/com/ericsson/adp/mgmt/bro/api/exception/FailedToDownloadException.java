package com.ericsson.adp.mgmt.bro.api.exception;
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
 * Represents an error has occurred while downloading a fragment during restore.
 */
public class FailedToDownloadException extends Exception {

    private static final long serialVersionUID = 8582169419007168913L;

    /**
     * Creates exception with custom message.
     * @param message displayed in the exception
     * @param exception What triggered the issue
     */
    public FailedToDownloadException(final String message, final Exception exception) {
        super(message, exception);
    }

    /**
     * Creates exception with custom message.
     * @param message displayed in the exception
     */
    public FailedToDownloadException(final String message) {
        super(message);
    }

}
