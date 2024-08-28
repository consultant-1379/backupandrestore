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
 * Performs actions upon receiving cancel message
 */
public class CancelActionState extends AgentState {

    private final String backupName;
    private final Action action;

    /**
     * @param agent The agent participating in the action
     * @param backupName Name of the backup
     * @param action type of action that being executed
     */
    public CancelActionState(final Agent agent, final String backupName, final Action action) {
        super(agent);
        this.action = action;
        this.backupName = backupName;
    }

    @Override
    public AgentState processMessage(final OrchestratorControl message) {
        return new WaitingForActionState(this.agent);
    }

    @Override
    public void trigger() {
        this.agent.cancelAction(new CancelActions(this.agent, this.backupName, this.action));
        this.agent.finishAction();
    }

    @Override
    public AgentState finishAction() {
        return new WaitingForActionState(this.agent);
    }

    @Override
    protected AgentState cancelAction() {
        return finishAction();
    }

}
