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
package com.ericsson.adp.mgmt.backupandrestore.action.yang;

import com.ericsson.adp.mgmt.backupandrestore.exception.UnprocessableEntityException;

/**
 * Represents invalid request.
 */
public class UnprocessableYangRequestException extends UnprocessableEntityException {

    private static final long serialVersionUID = -6135867535120146110L;

    /**
     * Create exception.
     * @param message explaining what happened.
     * @param cause of what happened.
     */
    public UnprocessableYangRequestException(final String message, final Exception cause) {
        super(message, cause);
    }

}
