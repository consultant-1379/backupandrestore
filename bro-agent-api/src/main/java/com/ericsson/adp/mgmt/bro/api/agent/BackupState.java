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

/**
 * Base class for describing backup state.
 */
public abstract class BackupState extends AgentState {
    protected final ActionInformation actionInformation;

    /**
     *
     * @param agent - The {@link Agent} associated with this {@link BackupState}.
     * @param actionInformation - An {@link ActionInformation} associated with this {@link BackupState}.
     */
    public BackupState(final Agent agent, final ActionInformation actionInformation) {
        super(agent);
        this.actionInformation = actionInformation;
    }

    @Override
    protected AgentState cancelAction() {
        return new CancelActionState(this.agent, this.actionInformation.getBackupName(), BACKUP);
    }
}
