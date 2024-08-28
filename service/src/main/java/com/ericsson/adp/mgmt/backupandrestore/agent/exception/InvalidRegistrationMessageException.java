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
package com.ericsson.adp.mgmt.backupandrestore.agent.exception;

/**
 * Agent tried to register with invalid information.
 */
public class InvalidRegistrationMessageException extends AgentRegistrationException {

    private static final long serialVersionUID = 3782492598663083561L;

    /**
     * Creates exception.
     */
    public InvalidRegistrationMessageException() {
        super("Invalid registration information");
    }

    /**
     * Creates exception with specific error message.
     * @param message - The message to report.
     */
    public InvalidRegistrationMessageException(final String message) {
        super(message);
    }

}
