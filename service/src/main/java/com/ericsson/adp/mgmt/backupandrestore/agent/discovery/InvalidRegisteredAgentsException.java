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
package com.ericsson.adp.mgmt.backupandrestore.agent.discovery;

import java.util.Set;

import io.kubernetes.client.openapi.ApiException;

/**
 * Represents a mismatch between registered and expected agents.
 */
public class InvalidRegisteredAgentsException extends RuntimeException {

    private static final long serialVersionUID = 9023414978656840632L;

    /**
     * Creates exception.
     * @param expectedAgentIds ids from expected agents.
     * @param registeredAgentIds ids from registered agents.
     */
    public InvalidRegisteredAgentsException(final Set<String> expectedAgentIds, final Set<String> registeredAgentIds) {
        super("Registered agents " + registeredAgentIds + " do not match with expected agents " + expectedAgentIds);
    }

    /**
     * Creates exception.
     * @param cause why it failed.
     */
    public InvalidRegisteredAgentsException(final ApiException cause) {
        super("Failed to access Kubernetes API", cause);
    }

}
