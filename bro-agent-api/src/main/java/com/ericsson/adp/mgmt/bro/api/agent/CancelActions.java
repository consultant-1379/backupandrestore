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

import com.ericsson.adp.mgmt.action.Action;

/**
 * Provides means to perform some actions on cancel
 */
public class CancelActions {

    private final Agent agent;
    private final String backupName;
    private final Action action;

    /**
     * @param agent Agent participating in action
     * @param backupName Name of the backup
     * @param action type of action that being executed
     */
    public CancelActions(final Agent agent, final String backupName, final Action action) {
        this.agent = agent;
        this.backupName = backupName;
        this.action = action;
    }

    /**
     * Once all the actions in the stage is completed call this method to inform the Orchestrator that the stage has completed
     *
     * @param success
     *            Inform the Orchestrator if the stage was successful or not
     * @param message
     *            Inform the Orchestrator why something went wrong or just that all is well
     */
    public void sendStageComplete(final boolean success, final String message) {
        this.agent.sendStageCompleteMessage(success, message, this.action);
    }

    /**
     * Provides the name of the backup.
     * @return backupName
     */
    public String getBackupName() {
        return this.backupName;
    }

    /**
     * Provides the type of action that being executed
     * @return action type
     */
    public Action getAction() {
        return this.action;
    }

}
