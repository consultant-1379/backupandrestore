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
import com.ericsson.adp.mgmt.control.Preparation;

/**
 * Performs preparatory actions prior to restore
 */
public class RestorePreparationState extends RestoreState {

    private static final Logger log = LogManager.getLogger(RestorePreparationState.class);

    /**
     * @param agent The agent performing the restore
     * @param preparation preparation message obtained during restore
     */
    public RestorePreparationState(final Agent agent, final Preparation preparation) {
        super(agent, new RestoreInformation(preparation));
    }

    @Override
    public AgentState processMessage(final OrchestratorControl message) {
        if (message.hasFragmentListEntry()) {
            restoreInformation.addFragment(message.getFragmentListEntry());
            if (message.getFragmentListEntry().getLast()) {
                // We will trigger rest of prepareRestoreActions and sendStageCompleteMessage to Orchestrator
                this.trigger();
            }
            return this;
        }
        if (isExecutionMessage(message)) {
            log.info("Received restore execution message for backup: {}", this.restoreInformation.getBackupName());
            return new RestoreExecutionState(this.agent, this.restoreInformation);
        }
        if (isCancelMessage(message)) {
            log.info("Received cancel restore message for backup: {}", this.restoreInformation.getBackupName());
            return cancelAction();
        }
        this.agent.sendStageCompleteMessage(false, "Unsuccessful restore stage", Action.RESTORE);
        return new WaitingForActionState(this.agent);
    }

    private boolean isExecutionMessage(final OrchestratorControl message) {
        return message.getOrchestratorMessageType().equals(OrchestratorMessageType.EXECUTION) && message.hasExecution();
    }

    @Override
    public void trigger() {
        this.agent.prepareForRestore(new RestorePreparationActions(this.agent, this.restoreInformation));
    }

    @Override
    public AgentState finishAction() {
        this.agent.sendStageCompleteMessage(false, "Finish action called during restore preparation", Action.RESTORE);
        return new FinishedActionState(this.agent, this.restoreInformation.getBackupName(), Action.RESTORE);
    }

}
