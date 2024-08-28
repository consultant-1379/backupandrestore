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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;

/**
 * Performs restore
 */
public class RestoreExecutionState extends RestoreState {

    private static final Logger log = LogManager.getLogger(RestoreExecutionState.class);

    /**
     * @param agent The agent performing the restore
     * @param restoreInformation information required for restore
     */
    public RestoreExecutionState(final Agent agent, final RestoreInformation restoreInformation) {
        super(agent, restoreInformation);
    }

    @Override
    public AgentState processMessage(final OrchestratorControl message) {
        if (isPostActionsMessage(message)) {
            log.info("Received restore post action message for backup: {}", this.restoreInformation.getBackupName());
            return new RestorePostActionState(this.agent, this.restoreInformation);
        } else if (isCancelMessage(message)) {
            log.info("Received cancel restore message for backup: {}", this.restoreInformation.getBackupName());
            return cancelAction();
        }
        this.agent.sendStageCompleteMessage(false, "Unsuccessful restore stage", Action.RESTORE);
        return new WaitingForActionState(this.agent);
    }

    private boolean isPostActionsMessage(final OrchestratorControl message) {
        return message.getOrchestratorMessageType().equals(OrchestratorMessageType.POST_ACTIONS) && message.hasPostActions();
    }

    @Override
    public void trigger() {
        this.agent.executeRestore(new RestoreExecutionActions(this.agent, this.restoreInformation));
    }

    @Override
    public AgentState finishAction() {
        this.agent.sendStageCompleteMessage(false, "Finish action called during restore execution", Action.RESTORE);
        return new FinishedActionState(this.agent, this.restoreInformation.getBackupName(), Action.RESTORE);
    }

}
