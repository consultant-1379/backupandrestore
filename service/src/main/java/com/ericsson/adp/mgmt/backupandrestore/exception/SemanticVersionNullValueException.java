/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
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
 * SemanticVersionNullValueException represents exception if semantic version is null.
 */
public class SemanticVersionNullValueException extends NotFoundException {

    private static final long serialVersionUID = -5614125679997100435L;

    /**
     * Creates exception.
     * @param source Indicates what semantic version value was set as null
     */
    public SemanticVersionNullValueException(final String source) {
        super("Semantic version is null in " + source);
    }
}
