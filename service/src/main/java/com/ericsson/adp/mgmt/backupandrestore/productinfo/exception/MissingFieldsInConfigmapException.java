/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.productinfo.exception;

/**
 * Represents error due to missing field(s) in configmap
 */
public class MissingFieldsInConfigmapException extends RuntimeException {

    private static final long serialVersionUID = -5617399169490627809L;

    /**
     * Creates Exception
     * @param message explaining reason for exception
     */
    public MissingFieldsInConfigmapException(final String message) {
        super(message);
    }
}
