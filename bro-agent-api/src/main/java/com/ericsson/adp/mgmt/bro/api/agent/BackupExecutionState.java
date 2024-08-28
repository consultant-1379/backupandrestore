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

package com.ericsson.adp.mgmt.bro.api.agent;

import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.POST_ACTIONS;

import com.ericsson.adp.mgmt.control.OrchestratorControl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents an Agent performing a backup.
 */
public class BackupExecutionState extends BackupState {

    private static final Logger log = LogManager.getLogger(BackupExecutionState.class);

    /**
     * @param agent The agent performing the backup
     * @param actionInformation information required for backup
     */
    public BackupExecutionState(final Agent agent, final ActionInformation actionInformation) {
        super(agent, actionInformation);
    }

    @Override
    public AgentState processMessage(final OrchestratorControl message) {
        if (isPostActionsMessage(message)) {
            log.info("Received backup post action message for backup: {}", actionInformation.getBackupName());
            return new PostBackupState(agent, actionInformation);
        } else if (isCancelMessage(message)) {
            log.info("Received cancel backup message for backup: {}", actionInformation.getBackupName());
            return cancelAction();
        }
        agent.sendStageCompleteMessage(false, "Unsuccessful backup stage.", BACKUP);
        return new WaitingForActionState(agent);
    }

    private boolean isPostActionsMessage(final OrchestratorControl message) {
        return message.getOrchestratorMessageType().equals(POST_ACTIONS) && message.hasPostActions();
    }

    @Override
    public void trigger() {
        agent.executeBackup(new BackupExecutionActions(agent, actionInformation));
    }

    @Override
    public AgentState finishAction() {
        this.agent.sendStageCompleteMessage(false, "Finish action called during backup execution", BACKUP);
        return new FinishedActionState(this.agent, this.actionInformation.getBackupName(), BACKUP);
    }
}
