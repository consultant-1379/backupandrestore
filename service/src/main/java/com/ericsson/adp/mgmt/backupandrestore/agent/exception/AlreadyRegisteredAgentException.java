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
 * Represents agent trying to connect with id that's already being used.
 */
public class AlreadyRegisteredAgentException extends AgentRegistrationException {

    private static final long serialVersionUID = -1160856948615109240L;

    /**
     * Create exception with agent id that caused the issue.
     * @param agentId that caused the issue.
     */
    public AlreadyRegisteredAgentException(final String agentId) {
        super("Failed to register agent because id <" + agentId + "> is already used");
    }

    @Override
    public Status getStatus() {
        return Status.ALREADY_EXISTS;
    }

}
