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
package com.ericsson.adp.mgmt.backupandrestore.exception;

/**
 * KeyStoreGenerationException represents exception if the Orchestrator fails to generate a keystore.
 */
public class KeyStoreGenerationException extends RuntimeException {
    private static final long serialVersionUID = -7825335954413015436L;

    /**
     * @param message Error message
     */
    public KeyStoreGenerationException(final String message) {
        super(message);
    }

    /**
     * Creates KeyStoreGenerationException exception.
     *
     * @param message message
     * @param cause exception cause
     */
    public KeyStoreGenerationException(final String message, final Exception cause) {
        super(message, cause);
    }
}
