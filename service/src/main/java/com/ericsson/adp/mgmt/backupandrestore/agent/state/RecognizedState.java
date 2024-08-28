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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.agent.AgentInputStream;
import com.ericsson.adp.mgmt.backupandrestore.backup.SoftwareVersion;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.restore.RestoreInformation;
import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.Register;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

/**
 * Represents an agent who is known and registered.
 */
public class RecognizedState implements AgentState {

    private static final Logger log = LogManager.getLogger(RecognizedState.class);

    protected final Register registrationInformation;

    /**
     * Creates state based on agent's registration information.
     *
     * @param registrationInformation
     *            agent's information.
     */
    public RecognizedState(final Register registrationInformation) {
        this.registrationInformation = registrationInformation;
    }

    @Override
    public AgentStateChange processMessage(final AgentControl message) {
        log.info("Ignoring message <{}> from agent <{}> because " +
                "it is not expected to receive this message when the agent is in state <{}>.",
                message, getAgentId(), this.getClass().getSimpleName());
        return new AgentStateChange(this);
    }

    @Override
    public String getAgentId() {
        return registrationInformation.getAgentId();
    }

    @Override
    public ApiVersion getApiVersion() {
        return ApiVersion.fromString(registrationInformation.getApiVersion());
    }

    @Override
    public String getScope() {
        return registrationInformation.getScope();
    }

    @Override
    public SoftwareVersion getSoftwareVersion() {
        final SoftwareVersion softwareVersion = new SoftwareVersion();
        final SoftwareVersionInfo softwareVersionInfo = registrationInformation.getSoftwareVersionInfo();
        softwareVersion.setAgentId(getAgentId());
        softwareVersion.setProductName(softwareVersionInfo.getProductName());
        softwareVersion.setProductNumber(softwareVersionInfo.getProductNumber());
        softwareVersion.setProductRevision(softwareVersionInfo.getRevision());
        softwareVersion.setDate(softwareVersionInfo.getProductionDate());
        softwareVersion.setDescription(softwareVersionInfo.getDescription());
        softwareVersion.setType(softwareVersionInfo.getType());
        if (registrationInformation.getApiVersion().contentEquals(ApiVersion.API_V4_0.getStringRepresentation())) {
            // getSemanticVersion() and getCommercialVersion() of softwareVersionInfo
            // always return not null object
            softwareVersion.setSemanticVersion(softwareVersionInfo.getSemanticVersion());
            softwareVersion.setCommercialVersion(softwareVersionInfo.getCommercialVersion());
        }
        return softwareVersion;
    }

    @Override
    public AgentStateChange prepareForBackup(final AgentInputStream inputStream, final CreateBackupJob job) {
        log.info("Triggering backup preparation on agent <{}>", getAgentId());
        return new AgentStateChange(new PreparingBackupState(registrationInformation, job),
            () -> inputStream.prepareForBackup(job.getBackupName(), job.getBackupManager().getAgentVisibleBRMId()));
    }

    @Override
    public AgentStateChange prepareForRestore(final AgentInputStream inputStream, final RestoreJob job) {
        log.info("Triggering restore preparation on agent <{}>", getAgentId());
        return new AgentStateChange(new PreparingRestoreState(registrationInformation, job),
            () -> {
                final RestoreInformation restoreInformation = job.createRestoreInformation(getAgentId());
                inputStream.prepareForRestore(restoreInformation);
                if (registrationInformation.getApiVersion().contentEquals(ApiVersion.API_V4_0.getStringRepresentation())) {
                    inputStream.sendFragmentList(restoreInformation);
                }
            });
    }

    @Override
    public AgentStateChange cancelAction(final AgentInputStream inputStream) {
        return new AgentStateChange(new RecognizedState(registrationInformation));
    }

    @Override
    public RecognizedState resetState() {
        return new RecognizedState(registrationInformation);
    }

}
