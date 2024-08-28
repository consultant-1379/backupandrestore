/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.exception;

/**
 * Represents an exception thrown when using the AWS OSMN persistence layer
 * */
public class AWSPersistenceException extends RuntimeException {

    private static final long serialVersionUID = 2147591557549708145L;

    /**
     * General exception for S3 persistence failures
     * */
    public AWSPersistenceException() {
        super();
    }

    /**
     * Creates exception.
     *
     * @param root exception.
     */
    public AWSPersistenceException(final Exception root) {
        super("Error in S3 persistence layer", root);
    }
}
