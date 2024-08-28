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
 * Exception to throw when an error occurs on the Data Channel.
 */
public class DataChannelException extends RuntimeException {

    private static final long serialVersionUID = -1698113963933620693L;

    /**
     * Exception to throw when an error occurs on the Data Channel.
     * @param message message to pass.
     */
    public DataChannelException(final String message) {
        super(message);
    }
}
