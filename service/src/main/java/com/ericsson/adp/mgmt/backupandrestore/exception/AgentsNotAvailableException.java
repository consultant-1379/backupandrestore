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

import java.util.Collection;

/**
 * Represents an error where an agent is required, but is not registered.
 */
public class AgentsNotAvailableException extends RuntimeException {

    private static final long serialVersionUID = -1416582413118249167L;

    /**
     * Creates an AgentsNotAvailableException.
     *
     * @param reason
     *            explaining what happened.
     */
    public AgentsNotAvailableException(final String reason) {
        super(reason);
    }

    /**
     * Creates an AgentsNotAvailableException
     *
     * @param agentIds
     *            The ids of the agents that are not available.
     * @param backupManagerId
     *            The id of the backup manager where the agents are expected to be registered with.
     */
    public AgentsNotAvailableException(final Collection<String> agentIds, final String backupManagerId) {
        super("Agents with the following IDs are required: " + agentIds
                + " \n but are currently not registered with the backup manager: " + backupManagerId);
    }
}