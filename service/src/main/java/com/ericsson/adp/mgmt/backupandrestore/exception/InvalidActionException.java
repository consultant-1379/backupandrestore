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
 * Represents trying to create an action with invalid information.
 */
public class InvalidActionException extends UnprocessableEntityException {

    private static final long serialVersionUID = 7323261499029094916L;

    /**
     * Creates exception.
     * @param reason explaining what happened.
     */
    public InvalidActionException(final String reason) {
        super("Failed to create action - " + reason);
    }

    /**
     * Creates exception.
     *
     * @param reason explaining what happened.
     * @param cause The underlying exception.
     */
    public InvalidActionException(final String reason, final Exception cause) {
        super("Failed to create action - " + reason, cause);
    }

}
