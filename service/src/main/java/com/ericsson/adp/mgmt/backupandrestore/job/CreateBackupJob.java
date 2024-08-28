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
package com.ericsson.adp.mgmt.backupandrestore.job;

import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.EXPORT;
import static com.ericsson.adp.mgmt.backupandrestore.util.YangEnabledDisabled.ENABLED;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.action.payload.ExportPayload;
import com.ericsson.adp.mgmt.backupandrestore.agent.discovery.AgentDiscoveryService;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.backup.storage.StorageMetadataFileService;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnauthorizedDataChannelException;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.data.Metadata;

/**
 * Responsible for performing a backup.
 */
public class CreateBackupJob extends JobWithStages<CreateBackupJob> {

    private static final Logger log = LogManager.getLogger(CreateBackupJob.class);

    private boolean autoDeleteFailures;
    private Backup backup;
    private BackupRepository backupRepository;
    private BackupLocationService backupLocationService;
    private AgentDiscoveryService agentDiscoveryService;
    private StorageMetadataFileService storageMetadataFileService;

    private final List<CreateActionRequest> postExecActions = new ArrayList<>();

    public String getBackupName() {
        return this.backup.getName();
    }

    /**
     * Receive the new fragment being sent through the data channel
     * @param agentId the agent id handling the fragment
     * @param fragmentId fragment id
     */
    public void receiveNewFragment(final String agentId, final String fragmentId) {
        this.jobStage.receiveNewFragment(agentId, fragmentId);
        updateProgressPercentage();
    }

    /**
     * Set the fragment progress to succeeded
     * @param agentId the agent id handling the fragment
     * @param fragmentId fragment id
     */
    public void fragmentSucceeded(final String agentId, final String fragmentId) {
        this.jobStage.fragmentSucceeded(agentId, fragmentId);
        updateProgressPercentage();
        changeAndTriggerJobStage();
    }

    /**
     * Set the fragment progress to failed
     * @param agentId the agent id handling the fragment
     * @param fragmentId fragment id
     */
    public void fragmentFailed(final String agentId, final String fragmentId) {
        this.jobStage.fragmentFailed(agentId, fragmentId);
        updateProgressPercentage();
        changeAndTriggerJobStage();
    }

    @Override
    public FragmentFolder getFragmentFolder(final Metadata metadata) {
        if (!metadataBelongsToBackup(metadata)) {
            throw new UnauthorizedDataChannelException(this.backup.getName(), metadata);
        }
        return this.backupLocationService.getFragmentFolder(metadata, this.backupManager.getBackupManagerId(), this.backup.getName());
    }

    @Override
    public void handleUnexpectedDataChannel(final Metadata metadata) {
        if (getAgents().stream().anyMatch(agent -> agent.getAgentId().equals(metadata.getAgentId()))) {
            handleUnexpectedDataChannel(metadata.getAgentId());
        }
    }

    @Override
    protected void triggerJob() {
        this.agentDiscoveryService.validateRegisteredAgents(this.backupManager, this.agents);
        this.backup = this.backupRepository.createBackup(this.backupManager, this.action, this.agents);
        storageMetadataFileService.createStorageMetadataFile(backupManager.getBackupManagerId(), backup.getName());
        buildJobPerfMetric();
        this.jobStage.trigger();
    }

    @Override
    protected void completeJob() {
        super.completeJob();
        this.backup.setStatus(BackupStatus.COMPLETE);
        this.backup.persist();

        log.info("Finished backup <{}>", this.backup.getName());

        // If this action is scheduled and auto export is enabled, queue up a post execution EXPORT action
        if (this.action.isScheduledEvent() && this.backupManager.getScheduler().getAutoExport() == ENABLED) {
            log.info("Queueing automatic EXPORT of backup <{}>", this.backup.getName());
            final CreateActionRequest export = new CreateActionRequest();
            export.setAction(EXPORT);
            final ExportPayload payload = new ExportPayload();
            payload.setBackupName(this.backup.getName());
            payload.setPassword(this.backupManager.getScheduler().getAutoExportPassword());
            payload.setUri(this.backupManager.getScheduler().getAutoExportUri());
            payload.setSftpServerName(this.backupManager.getScheduler().getSftpServerName());
            export.setPayload(payload);
            export.setScheduledEvent(true);
            postExecActions.add(export);
        }
        resetCM(true);
    }

    @Override
    protected void fail() {
        super.fail();
        if (this.backup != null) {
            this.backup.setStatus(BackupStatus.CORRUPTED);
            this.backup.persist();
            log.error("Backup <{}> failed", this.backup.getName());
            if (autoDeleteFailures) {
                log.info("Auto-delete failed backups enabled, deleting failed backup <{}>", this.backup.getName());
                deleteFailedBackup();
            }
        }
        resetCM(true);
    }

    @Override
    protected List<CreateActionRequest> getPostExecutionActions() {
        return postExecActions;
    }

    private void deleteFailedBackup() {
        DeleteBackupJob.deleteBackupFiles(backupLocationService, backupManager, backup, this.getAwsConfig());
        backupRepository.deleteBackup(backup, backupManager);
    }

    private boolean metadataBelongsToBackup(final Metadata metadata) {
        return this.backup.getName().equals(metadata.getBackupName())
                && this.agents.stream().anyMatch(agent -> agent.getAgentId().equals(metadata.getAgentId()));
    }

    protected void setBackupRepository(final BackupRepository backupRepository) {
        this.backupRepository = backupRepository;
    }

    protected void setBackupLocationService(final BackupLocationService backupLocationService) {
        this.backupLocationService = backupLocationService;
    }

    protected void setAgentDiscoveryService(final AgentDiscoveryService agentDiscoveryService) {
        this.agentDiscoveryService = agentDiscoveryService;
    }

    public void setStorageMetadataFileService(final StorageMetadataFileService storageMetadataFileService) {
        this.storageMetadataFileService = storageMetadataFileService;
    }

    public void setAutoDeleteFailures(final boolean autoDeleteFailures) {
        this.autoDeleteFailures = autoDeleteFailures;
    }

}
