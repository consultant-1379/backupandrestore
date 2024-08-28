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

import java.io.IOException;

/**
 * represents exception occured if the path locations for restore action are incorrect/missing.
 */
public class RestoreLocationDoesNotExistException extends RuntimeException {
    private static final long serialVersionUID = 9163222755251866926L;

    /**
     * creates exception.
     *
     * @param message
     *            -exception information.
     */
    public RestoreLocationDoesNotExistException(final String message) {
        super(message);
    }

    /**
     * creates exception
     *
     * @param message
     *            - exception information.
     * @param exception
     *            - exception
     */
    public RestoreLocationDoesNotExistException(final String message, final IOException exception) {
        super(message, exception);
    }

}
