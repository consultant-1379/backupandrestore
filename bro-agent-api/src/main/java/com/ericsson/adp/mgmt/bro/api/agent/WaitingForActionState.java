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

import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;

/**
 * Waits for Orchestrator control message.
 */
public class WaitingForActionState extends AgentState {

    private static final Logger log = LogManager.getLogger(WaitingForActionState.class);

    /**
     * Creates state.
     * @param agent The Agent waiting for action.
     */
    public WaitingForActionState(final Agent agent) {
        super(agent);
    }

    @Override
    public AgentState processMessage(final OrchestratorControl message) {
        if (message.hasRegisterAcknowledge()) {
            log.info(message);
            return this;
        } else if (isPreparationMessage(message)) {
            switch (message.getAction()) {
                case BACKUP:
                    log.info("The Orchestrator has requested that the Agent performs a backup: {}", message.getPreparation().getBackupName());
                    return new BackupPreparationState(this.agent, new ActionInformation(message.getPreparation()));
                case RESTORE:
                    log.info("The Orchestrator has requested that the Agent restores backup: {}", message.getPreparation().getBackupName());
                    return new RestorePreparationState(this.agent, message.getPreparation());
            }
        }
        log.info("Unexpected message received <{}>", message);
        return this;

    }

    private boolean isPreparationMessage(final OrchestratorControl message) {
        return message.getOrchestratorMessageType().equals(OrchestratorMessageType.PREPARATION) && message.hasPreparation();
    }

    @Override
    public void trigger() {
        // Do nothing
    }

    @Override
    public AgentState finishAction() {
        return this;
    }

    @Override
    protected AgentState cancelAction() {
        return this;
    }

}
