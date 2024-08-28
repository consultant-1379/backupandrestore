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
 * Performs actions post restore
 */
public class RestorePostActionState extends RestoreState {

    /**
     * @param agent The agent performing the restore
     * @param restoreInformation required for restore
     */
    public RestorePostActionState(final Agent agent, final RestoreInformation restoreInformation) {
        super(agent, restoreInformation);
    }

    @Override
    public AgentState processMessage(final OrchestratorControl message) {
        return new WaitingForActionState(this.agent);
    }

    @Override
    public void trigger() {
        this.agent.postRestore(new PostRestoreActions(this.agent, this.restoreInformation));
        this.agent.finishAction();
    }

    @Override
    public AgentState finishAction() {
        return new FinishedActionState(this.agent, this.restoreInformation.getBackupName(), Action.RESTORE);
    }

}
