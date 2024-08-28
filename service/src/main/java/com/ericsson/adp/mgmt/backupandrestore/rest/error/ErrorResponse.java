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
package com.ericsson.adp.mgmt.backupandrestore.rest.error;

/**
 * JSON response that encapsulates an error.
 */
public class ErrorResponse {

    private int statusCode;
    private String message;

    /**
     * Default constructor, to be used by Jackson.
     */
    public ErrorResponse() {}

    /**
     * Creates errorResponse based on a statusCode and message.
     * @param statusCode consequence of the error.
     * @param message of the error.
     */
    public ErrorResponse(final int statusCode, final String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(final int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

}
