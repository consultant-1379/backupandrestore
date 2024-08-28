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
 * Represents errors encountered while creating directories during Backup .
 */
public class FileDirectoryException extends RuntimeException {

    private static final long serialVersionUID = -8630134829482108534L;

    /**
     * Creates exception.
     *
     * @param ioException
     *            explaining what happened.
     */
    public FileDirectoryException(final IOException ioException) {
        super(ioException);
    }

    /**
     * Creates File-directory related exception.
     *
     * @param message the error message
     */
    public FileDirectoryException(final String message) {
        super(message);
    }

    /**
     * Creates File-directory related exception.
     *
     * @param message the error message
     * @param cause   the error cause
     */
    public FileDirectoryException(final String message, final Exception cause) {
        super(message, cause);
    }
}
