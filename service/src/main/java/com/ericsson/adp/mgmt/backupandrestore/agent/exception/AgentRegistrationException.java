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

import io.grpc.Status;

/**
 * Represents an error has occurred during agent registration.
 */
public abstract class AgentRegistrationException extends RuntimeException {

    private static final long serialVersionUID = -7546414976911209355L;

    /**
     * Creates exception.
     */
    public AgentRegistrationException() {
        super("Registration failed");
    }

    /**
     * Creates exception with custom message.
     * @param message custom message.
     */
    public AgentRegistrationException(final String message) {
        super(message);
    }

    public Status getStatus() {
        return Status.INVALID_ARGUMENT;
    }

}
