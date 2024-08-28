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
package com.ericsson.adp.mgmt.bro.api.exception;

/**
 * An exception thrown by the createAgent(OrchestratorConnectionInformation, AgentBehavior) method of
 * {@link com.ericsson.adp.mgmt.bro.api.agent.AgentFactory}
 */
public class InvalidOrchestratorConnectionInformationException extends RuntimeException {

    private static final long serialVersionUID = -5031663726945483617L;

    /**
     * Creates exception with custom message.
     * @param message displayed in the exception
     * @param exception What triggered the issue
     */
    public InvalidOrchestratorConnectionInformationException(final String message, final Exception exception) {
        super(message, exception);
    }

}
