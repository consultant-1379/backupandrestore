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
 * Represents trying to register an agent without id.
 */
public class RegisteringAgentWithoutIdException extends AgentRegistrationException {

    private static final long serialVersionUID = 5777171985550614015L;

    /**
     * Creates exception.
     */
    public RegisteringAgentWithoutIdException() {
        super("Failed to register agent because it doesn't have an id");
    }

}
