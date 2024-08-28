/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.bro.api.agent;

/**
 * provides method to download the fragments to be restored
 */
public class RestoreExecutionActions extends RestoreActions {

    /**
     * provides method to download the fragments to be restored
     *
     * @param agent The agent performing the restore action
     * @param restoreInformation required for restore
     */
    public RestoreExecutionActions(final Agent agent, final RestoreInformation restoreInformation) {
        super(agent, restoreInformation);
    }

    /**
     * Once all fragments have been restored call this method to inform the Orchestrator that the restore execution has completed
     *
     * @param success
     *            Inform the Orchestrator if the restore was successful or not
     * @param message
     *            Inform the Orchestrator why something went wrong or just that all is well
     */
    public void restoreComplete(final boolean success, final String message) {
        super.sendStageComplete(success, message);
    }
}
