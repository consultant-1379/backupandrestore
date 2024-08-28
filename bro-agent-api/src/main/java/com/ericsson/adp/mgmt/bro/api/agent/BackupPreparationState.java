/*
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 * ****************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.bro.api.agent;

import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.EXECUTION;

import com.ericsson.adp.mgmt.control.OrchestratorControl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Describes a {@link BackupPreparationState}
 */
public class BackupPreparationState extends BackupState {

    private static final Logger log = LogManager.getLogger(BackupPreparationState.class);

    /**
     * Constructor
     * @param agent - the {@link Agent} for this {@link BackupPreparationState}
     * @param actionInformation - the {@link ActionInformation} for this {@link BackupPreparationState}
     */
    public BackupPreparationState(final Agent agent, final ActionInformation actionInformation) {
        super(agent, actionInformation);
    }

    @Override
    public AgentState processMessage(final OrchestratorControl message) {
        if (isExecutionMessage(message)) {
            log.info("Received backup execution message for backup: {}", actionInformation.getBackupName());
            return new BackupExecutionState(agent, actionInformation);
        }
        if (isCancelMessage(message)) {
            log.info("Received cancel backup message for backup: {}", actionInformation.getBackupName());
            return cancelAction();
        }
        agent.sendStageCompleteMessage(false, "Unsuccessful backup stage", BACKUP);
        return new WaitingForActionState(agent);
    }

    private boolean isExecutionMessage(final OrchestratorControl message) {
        return message.getOrchestratorMessageType().equals(EXECUTION) && message.hasExecution();
    }

    @Override
    public void trigger() {
        agent.prepareForBackup(new BackupPreparationActions(agent, actionInformation));
    }

    @Override
    public AgentState finishAction() {
        agent.sendStageCompleteMessage(false, "Finish action called during backup preparation", BACKUP);
        return new FinishedActionState(agent, actionInformation.getBackupName(), BACKUP);
    }
}
