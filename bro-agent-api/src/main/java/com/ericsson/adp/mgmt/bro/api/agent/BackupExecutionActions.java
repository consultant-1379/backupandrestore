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

import com.ericsson.adp.mgmt.bro.api.exception.FailedToTransferBackupException;
import com.ericsson.adp.mgmt.bro.api.fragment.BackupFragmentInformation;
import com.ericsson.adp.mgmt.bro.api.service.BackupService;

/**
 * Provides functions for an agent to send metadata, the backup and custom metadata.
 * An agent implementer should always send metadata followed by the backup.
 * It is optional to send custom metadata.
 */
public class BackupExecutionActions extends BackupActions {

    /**
     * provides the actions that an agent is allowed to perform during the backup execution.
     * @param agent The agent performing the backup actions
     * @param actionInformation information on the backup to be performed
     */
    public BackupExecutionActions(final Agent agent, final ActionInformation actionInformation) {
        super(agent, actionInformation);
    }

    /**
     * To be used to send backup data to the orchestrator
     * @param fragmentInformation The information about a fragment.
     * @throws FailedToTransferBackupException If there is an issue in the transfer this will be thrown
     */
    public void sendBackup(final BackupFragmentInformation fragmentInformation) throws FailedToTransferBackupException {
        final BackupService backupService = new BackupService(agent.getBackupStream());
        backupService.backup(fragmentInformation, agent.getAgentId(), actionInformation.getBackupName());
    }

    /**
     * Once all backup fragments have been sent call this method to inform the orchestrator that the backup execution has completed
     * @param success Inform the orchestrator if the backup was successful or not
     * @param message Inform the orchestrator why something went wrong or just that all is well
     */
    public void backupComplete(final boolean success, final String message) {
        sendStageComplete(success, message);
    }
}
