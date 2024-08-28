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
 * Exception for the SslContextService.
 */
public class SslContextException extends RuntimeException{

    private static final long serialVersionUID = -4740472085745338131L;

    /**
     * Constructor accepts Exception
     * @param cause exception cause
     */
    public SslContextException(final Exception cause) {
        super(cause);
    }
}
