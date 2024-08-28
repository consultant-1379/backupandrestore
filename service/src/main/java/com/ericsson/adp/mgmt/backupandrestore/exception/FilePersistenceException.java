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
 * Represents error dealing with persisted files.
 */
public class FilePersistenceException extends RuntimeException {

    private static final long serialVersionUID = 2147591557549708145L;

    /**
     * Creates Exception
     * @param message - the error message.
     */
    public FilePersistenceException(final String message) {
        super(message);
    }

    /**
     * Creates exception.
     * @param root exception.
     */
    public FilePersistenceException(final Exception root) {
        super("Error handling persisted file", root);
    }

}
