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

import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;

/**
 * Process control messages from Orchestrator
 */
public abstract class AgentState {

    protected Agent agent;

    /**
     * @param agent the Agent performing action
     */
    public AgentState(final Agent agent) {
        this.agent = agent;
    }

    /**
     * Process control messages from Orchestrator
     * @param message OrchestratorControl message
     * @return Agent state change
     */
    public abstract AgentState processMessage(final OrchestratorControl message);

    /**
     * execute state's actions
     */
    public abstract void trigger();

    /**
     * Finish the current action
     * @return Agent state
     */
    public abstract AgentState finishAction();

    /**
     * Cancel current action.
     * @return next state.
     */
    protected abstract AgentState cancelAction();

    /**
     * Indicates if the message is of type CANCEL_BACKUP_RESTORE
     * @param message Orchestrator control message
     * @return boolean indicating if the message is of type CANCEL_BACKUP_RESTORE
     */
    protected boolean isCancelMessage(final OrchestratorControl message) {
        return message.getOrchestratorMessageType().equals(OrchestratorMessageType.CANCEL_BACKUP_RESTORE) && message.hasCancel();
    }

}
