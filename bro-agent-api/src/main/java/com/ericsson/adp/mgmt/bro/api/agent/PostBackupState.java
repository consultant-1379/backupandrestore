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

import com.ericsson.adp.mgmt.control.OrchestratorControl;

/**
 * Describes a {@link PostBackupState}
 */
public class PostBackupState extends BackupState {
    /**
     * @param agent - The {@link Agent} associated with this {@link PostBackupState}.
     * @param actionInformation - An {@link ActionInformation} associated with this {@link BackupState}.
     */
    public PostBackupState(final Agent agent, final ActionInformation actionInformation) {
        super(agent, actionInformation);
    }

    @Override
    public AgentState processMessage(final OrchestratorControl message) {
        return new WaitingForActionState(agent);
    }

    @Override
    public void trigger() {
        agent.postBackup(new PostBackupActions(agent, actionInformation));
        agent.finishAction();
    }

    @Override
    public AgentState finishAction() {
        return new FinishedActionState(agent, actionInformation.getBackupName(), BACKUP);
    }
}
