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
package com.ericsson.adp.mgmt.backupandrestore.agent.state;

import com.ericsson.adp.mgmt.backupandrestore.agent.AgentInputStream;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.control.Register;

/**
 * Represents a registered agent who is executing a restore.
 */
public class ExecutingRestoreState extends RestoreState {

    /**
     * Creates state based on agent's registration information.
     * @param registrationInformation agent's information.
     * @param job responsible for doing restore.
     */
    public ExecutingRestoreState(final Register registrationInformation, final RestoreJob job) {
        super(registrationInformation, job);
    }

    @Override
    public AgentStateChange executeRestorePostAction(final AgentInputStream inputStream) {
        return new AgentStateChange(new PostActionRestoreState(this.registrationInformation, this.job), inputStream::executeRestorePostAction);
    }

}
