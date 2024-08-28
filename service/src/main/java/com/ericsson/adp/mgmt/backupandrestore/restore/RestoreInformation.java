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
package com.ericsson.adp.mgmt.backupandrestore.restore;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.SoftwareVersion;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;
import com.ericsson.adp.mgmt.control.Preparation;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Responsible for generating restore preparation message sent to agent.
 */
public class RestoreInformation {

    private final Backup backup;
    private final BackupManager backupManager;
    private final String agentId;
    private final FragmentFileService fragmentFileService;
    private Optional<Agent> agent = Optional.empty();

    /**
     * Creates RestoreInformation.
     *
     * @param backup
     *            - backup to be restored.
     * @param backupManager
     *            - backupManager
     * @param agentId
     *            - agent
     * @param fragmentFileService
     *            - responsible to fetch agent's fragments
     */
    public RestoreInformation(final Backup backup,
            final BackupManager backupManager,
            final String agentId,
            final FragmentFileService fragmentFileService) {
        this.backup = backup;
        this.backupManager = backupManager;
        this.agentId = agentId;
        this.fragmentFileService = fragmentFileService;
    }

    /**
     * Creates RestoreInformation.
     *
     * @param backup
     *            - backup to be restored.
     * @param backupManager
     *            - backupManager
     * @param agentId
     *            - agent
     * @param fragmentFileService
     *            - responsible to fetch agent's fragments
     * @param agent Agent
     */
    public RestoreInformation(final Backup backup,
                              final BackupManager backupManager,
                              final String agentId,
                              final FragmentFileService fragmentFileService,
                              final Optional<Agent> agent) {
        this.backup = backup;
        this.backupManager = backupManager;
        this.agentId = agentId;
        this.fragmentFileService = fragmentFileService;
        if (agent.isPresent()) {
            this.agent = agent;
        }
    }

    /**
     * Generation of message for restore action sent to agent.
     *
     * @return - Preparation message to be sent to agent.
     */
    public Preparation buildPreparationMessage() {
        // If it's a v4 agent we should send no fragments initially in the Preparation message
        // Instead we will send them separately as FragmentListEntry messages
        if (agent.isPresent() && (agent.get().getApiVersion() == ApiVersion.API_V4_0)) {
            final List<Fragment> fragments = new ArrayList<>();
            if (!getBackupFragments().isEmpty()) {
                fragments.add(Fragment.newBuilder().build());
            }
            return Preparation.newBuilder()
                    .setBackupName(backup.getBackupId())
                    .setBackupType(backupManager.getAgentVisibleBRMId())
                    .addAllFragment(fragments)
                    .setSoftwareVersionInfo(getSoftwareVersionInfo()).build();
        } else {
            return Preparation.newBuilder()
                    .setBackupName(backup.getBackupId())
                    .setBackupType(backupManager.getAgentVisibleBRMId())
                    .addAllFragment(getBackupFragments())
                    .setSoftwareVersionInfo(getSoftwareVersionInfo()).build();
        }
    }

    public void setAgent(final Optional<Agent> agent) {
        this.agent = agent;
    }

    private SoftwareVersionInfo getSoftwareVersionInfo() {
        final SoftwareVersion softwareVersion = backup.getSoftwareVersions().stream()
                .filter(backupSoftwareVersion -> backupSoftwareVersion.getAgentId().equals(agentId)).findFirst()
                .orElse(new SoftwareVersion());
        return buildSoftwareVersionInfo(softwareVersion);
    }

    private SoftwareVersionInfo buildSoftwareVersionInfo(final SoftwareVersion softwareVersion) {
        final SoftwareVersionInfo.Builder builder = SoftwareVersionInfo.newBuilder()
                .setDescription(softwareVersion.getDescription())
                .setProductionDate(softwareVersion.getDate())
                .setProductName(softwareVersion.getProductName())
                .setProductNumber(softwareVersion.getProductNumber())
                .setRevision(softwareVersion.getProductRevision())
                .setType(softwareVersion.getType());

        if (softwareVersion.getCommercialVersion() != null) {
            builder.setCommercialVersion(softwareVersion.getCommercialVersion());
        }
        if (softwareVersion.getSemanticVersion() != null) {
            builder.setSemanticVersion(softwareVersion.getSemanticVersion());
        }
        return builder.build();
    }

    public List<Fragment> getBackupFragments() {
        return fragmentFileService.getFragments(backupManager.getBackupManagerId(), backup.getBackupId(), agentId);
    }

}
