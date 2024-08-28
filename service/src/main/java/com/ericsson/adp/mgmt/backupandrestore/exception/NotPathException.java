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
 * To be thrown when something is not a Path, but a Path was expected.
 */
public class NotPathException extends RuntimeException {

    private static final long serialVersionUID = 7234844786031391875L;

    /**
     * Creates an exception
     */
    public NotPathException() {
        super("Not a path");
    }

}