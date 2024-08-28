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
 * Represents exception related to restore file transfer to agent.
 */
public class RestoreDownloadException extends RuntimeException {

    private static final long serialVersionUID = 1215381369239929599L;

    /**
     * creates exception.
     *
     * @param message
     *            - exception information
     */
    public RestoreDownloadException(final String message) {
        super(message);
    }

    /**
     * Creates exception.
     * @param message - what happened.
     * @param cause - what caused it.
     */
    public RestoreDownloadException(final String message, final Exception cause) {
        super(message, cause);
    }

}
