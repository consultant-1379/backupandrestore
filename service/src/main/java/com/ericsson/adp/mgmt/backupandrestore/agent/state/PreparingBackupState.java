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

import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V2_0;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V4_0;

import com.ericsson.adp.mgmt.backupandrestore.agent.AgentInputStream;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;
import com.ericsson.adp.mgmt.control.Register;
/**
 * Represents a registered agent who is preparing for a backup.
 */
public class PreparingBackupState extends AgentBackupState {

    /**
     * Creates state based on agent's registration information.
     * @param registrationInformation agent's information.
     * @param job responsible for doing backup.
     */
    public PreparingBackupState(final Register registrationInformation, final CreateBackupJob job) {
        super(registrationInformation, job);
    }

    @Override
    public AgentStateChange executeBackup(final AgentInputStream inputStream) {
        final ApiVersion apiVersion = getApiVersion();
        if (apiVersion.equals(API_V3_0) || apiVersion.equals(API_V4_0)) {
            return new AgentStateChange(new ExecutingBackupState(registrationInformation, job), inputStream::executeBackup);
        }

        throw new UnsupportedOperationException("Unable to change state because Agent API Version " + apiVersion.getStringRepresentation() +
            " is not supported.");
    }

    @Override
    public RecognizedState resetState() {
        if (getApiVersion().equals(API_V2_0)) {
            return  new RecognizedState(registrationInformation);
        }

        return super.resetState();
    }
}
