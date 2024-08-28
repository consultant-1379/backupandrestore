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

/**
 * Represents agent participating in a restore.
 */
public abstract class RestoreState extends AgentState {

    protected final RestoreInformation restoreInformation;

    /**
     * @param agent The agent performing the restore
     * @param restoreInformation information required for restore
     */
    public RestoreState(final Agent agent, final RestoreInformation restoreInformation) {
        super(agent);
        this.restoreInformation = restoreInformation;
    }

    @Override
    protected AgentState cancelAction() {
        return new CancelActionState(this.agent, this.restoreInformation.getBackupName(), Action.RESTORE);
    }

}
