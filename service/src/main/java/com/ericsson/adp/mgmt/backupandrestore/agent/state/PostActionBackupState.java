/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.agent.state;

import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.control.Register;

/**
 * Represents a registered agent who is doing post actions after a backup.
 */
public class PostActionBackupState extends AgentBackupState {

    /**
     * Creates state based on agent's registration information.
     * @param registrationInformation agent's information.
     * @param job responsible for doing backup.
     */
    public PostActionBackupState(final Register registrationInformation, final CreateBackupJob job) {
        super(registrationInformation, job);
    }

    @Override
    public RecognizedState resetState() {
        return new RecognizedState(registrationInformation);
    }
}
