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
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.control.Register;

/**
 * Represents a registered agent who is executing a backup.
 */
public class ExecutingBackupState extends AgentBackupState {

    /**
     * Creates state based on agent's registration information.
     * @param registrationInformation agent's information.
     * @param job responsible for doing backup.
     */
    public ExecutingBackupState(final Register registrationInformation, final CreateBackupJob job) {
        super(registrationInformation, job);
    }

    @Override
    public AgentStateChange executeBackupPostAction(final AgentInputStream inputStream) {
        return new AgentStateChange(new PostActionBackupState(registrationInformation, job), inputStream::executeBackupPostAction);
    }

}
