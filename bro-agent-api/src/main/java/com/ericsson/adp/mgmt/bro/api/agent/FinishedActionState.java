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
import com.ericsson.adp.mgmt.control.OrchestratorControl;

/**
 * Final state of an action
 * Awaits cancel message or new action message
 */
public class FinishedActionState extends WaitingForActionState {

    private final String backupName;
    private final Action action;

    /**
     * @param agent The agent participating in action
     * @param backupName backup name
     * @param action action type
     */
    public FinishedActionState(final Agent agent, final String backupName, final Action action) {
        super(agent);
        this.action = action;
        this.backupName = backupName;
    }

    @Override
    public AgentState processMessage(final OrchestratorControl message) {
        if (isCancelMessage(message)) {
            return new CancelActionState(this.agent, this.backupName, this.action);
        }
        return super.processMessage(message);
    }

}
