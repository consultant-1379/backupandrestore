/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.restore;

/**
 * ChecksumValidationException represents exception when there is a mismatch between calculated and stored checksums
 */
public class ChecksumValidationException extends RuntimeException {

    private static final long serialVersionUID = -6904802028301039131L;

    /**
     * Creates exception.
     *
     * @param message
     *            - exception information
     */
    public ChecksumValidationException(final String message) {
        super(message);
    }

    /**
     * Creates exception.
     * @param message - what happened.
     * @param cause - what caused it.
     */
    public ChecksumValidationException(final String message, final Exception cause) {
        super(message, cause);
    }

}