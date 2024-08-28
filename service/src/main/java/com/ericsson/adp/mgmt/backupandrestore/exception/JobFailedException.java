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
 * Represents a job failing.
 */
public class JobFailedException extends RuntimeException {

    private static final long serialVersionUID = -1270451181192468437L;

    /**
     * Creates exception.
     * @param message what happened.
     */
    public JobFailedException(final String message) {
        super(message);
    }

    /**
     * Creates exception.
     * @param exception what happened.
     */
    public JobFailedException(final Exception exception) {
        super(exception);
    }

}
