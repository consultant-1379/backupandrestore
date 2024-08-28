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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.operation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;

/**
 * Creates patches to act on BackupManagers.
 */
@Service
public class BackupManagerPatchFactory {

    private BackupManagerRepository backupManagerRepository;

    /**
     * Creates a patch to add a backupManager to CM.
     * @param backupManager to be added.
     * @return patch.
     */
    public AddBackupManagerPatch getPatchToAddBackupManager(final BackupManager backupManager) {
        return new AddBackupManagerPatch(backupManager);
    }

    /**
     * Creates a patch to update a backupManager in CM.
     * @param backupManager to be updated.
     * @return patch.
     */
    public UpdateBackupManagerPatch getPatchToUpdateBackupManager(final BackupManager backupManager) {
        return new UpdateBackupManagerPatch(backupManager, getPathToBackupManager(backupManager.getBackupManagerId()));
    }

    /**
     * Gets CM Path to a backupManager.
     * @param backupManagerId to look for.
     * @return path to backupManager.
     */
    public String getPathToBackupManager(final String backupManagerId) {
        return String.valueOf(backupManagerRepository.getIndex(backupManagerId));
    }

    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

}
