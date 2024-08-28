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
 * Represents something wrong that happened while parsing json.
 */
public class JsonParsingException extends RuntimeException {

    private static final long serialVersionUID = 7104143951536268307L;

    /**
     * Creates exception.
     */
    public JsonParsingException() {
        super("Error parsing json");
    }

    /**
     * Creates exception.
     * @param cause what caused the issue.
     */
    public JsonParsingException(final Exception cause) {
        super("Error parsing json", cause);
    }

}
