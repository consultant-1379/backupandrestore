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

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_RESOURCE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;

/**
 * Creates patches to act on Backups.
 */
@Service
public class BackupPatchFactory {

    private BackupManagerPatchFactory backupManagerPatchFactory;
    private BackupManagerRepository backupManagerRepository;

    /**
     * Creates a patch to add a backup to CM.
     * @param manager the manager to add the backup under
     * @param backup to be added.
     * @return patch.
     */
    public AddBackupPatch getPatchToAddBackup(final BackupManager manager,
                                              final Backup backup) {
        return new AddBackupPatch(backup, getPathToBackup(manager.getBackupManagerId(), false));
    }

    /**
     * Creates an initial patch to add a backup to CM.
     * @param manager the manager to add the backup under
     * @param backup to be added.
     * @return patch.
     */
    public AddInitialBackupPatch getPatchToAddInitialBackup(final BackupManager manager,
                                              final Backup backup) {
        return new AddInitialBackupPatch(backup,
                getPathToBackup(manager.getBackupManagerId(), false));
    }

    /**
     * Creates a patch to add a backup to CM.
     * @return patch.
     */
    public AddBackupManagerPath getPathToAddBackupManager() {
        return new AddBackupManagerPath();
    }

    /**
     * Creates a patch to update a backup in CM.
     * @param manager the manager to update the backup under
     * @param backup to be updated.
     * @return patch.
     */
    public UpdateBackupPatch getPatchToUpdateBackup(final BackupManager manager, final Backup backup) {
        return new UpdateBackupPatch(backup, getPathToBackup(manager.getBackupManagerId(), backup.getBackupId()));
    }

    /**
     * Creates a patch to delete a backup in CM.
     * @param backupManagerId owner of backup.
     * @param backupIndex index of backup to be deleted.
     * @return patch.
     */
    public DeleteBackupPatch getPatchToDeleteBackup(final String backupManagerId, final int backupIndex) {
        return new DeleteBackupPatch(getPathToBackup(backupManagerId, true) + backupIndex);
    }

    /**
     * Gets CM Path to a backup.
     * @param backupManagerId owner of backup.
     * @param backupId backup to look for.
     * @return path to backup.
     */
    public String getPathToBackup(final String backupManagerId, final String backupId) {
        return getPathToBackup(backupManagerId, true) + getBackupIndex(backupManagerId, backupId);
    }

    /**
     * Gets CM Path to a backup.
     * @param backupManagerId owner of backup.
     * @param backupId backup to look for.
     * @return path to backup.
     */
    public String getPathToPostBackup(final String backupManagerId, final String backupId) {
        return getPathToBackup(backupManagerId, true) + getBackupIndex(backupManagerId, backupId);
    }

    private String getPathToBackup(final String backupManagerId, final boolean toAdd) {
        return backupManagerPatchFactory.getPathToBackupManager(backupManagerId) + "/" + BACKUP_RESOURCE + (toAdd ? "/" : "");
    }

    private int getBackupIndex(final String backupManagerId, final String backupId) {
        return backupManagerRepository
                    .getBackupManager(backupManagerId)
                    .getBackupIndex(backupId);
    }

    @Autowired
    public void setBackupManagerPatchFactory(final BackupManagerPatchFactory backupManagerPatchFactory) {
        this.backupManagerPatchFactory = backupManagerPatchFactory;
    }

    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

}
