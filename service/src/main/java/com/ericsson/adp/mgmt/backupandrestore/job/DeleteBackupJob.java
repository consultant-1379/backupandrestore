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
package com.ericsson.adp.mgmt.backupandrestore.job;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.exception.DeleteBackupException;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProvider;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProviderFactory;

/**
 * Responsible for deleting a backup.
 */
public class DeleteBackupJob extends Job {

    private static final Logger log = LogManager.getLogger(DeleteBackupJob.class);
    private BackupLocationService backupLocationService;
    private BackupRepository backupRepository;
    private Backup backup;

    @Override
    protected void triggerJob() {
        backup = backupManager.getBackup(getBackupId(), Ownership.OWNED);
        /* Restart of BRO during delete_backup lets a partial backup behind and is not cleaned up.
        Since this will still have status "success" there is a risk there could be an attempt to restore it.
        Fix is to set to state to corrupted before delete backup operation performed to mitigate this risk.
        housekeeping will then eventually clean it up on next create-backup */

        backup.setStatus(BackupStatus.CORRUPTED);
        backup.persist();

        deleteBackupFiles();
        backupRepository.deleteBackup(backup, backupManager);
    }

    @Override
    protected boolean didFinish() {
        return !backupManager.getBackups(Ownership.OWNED).contains(backup);
    }

    @Override
    protected void completeJob() {
        log.info("Finished deleting backup <{}> of backupManager <{}>", backup.getBackupId(), backupManager.getBackupManagerId());
        resetCM(true);
    }

    @Override
    protected void fail() {
        if (backup != null) {
            backup.setStatus(BackupStatus.CORRUPTED);
            backup.persist();
        }
        resetCM(true);
    }

    private String getBackupId() {
        final BackupNamePayload payload = (BackupNamePayload) action.getPayload();
        return payload.getBackupName();
    }

    private void deleteBackupFiles() {
        deleteBackupFiles(backupLocationService, backupManager, backup, this.getAwsConfig());
    }

    /**
     * Deletes the files on disk for a given backup. Responsibility for other deletion steps is left to caller
     * @param locationService BackupLocationService used to locate backup on disk.
     * @param manager BackupManager used to manage backup to be deleted.
     * @param backup Backup who's files are to be deleted on disk
     * @param s3Config the AWS client configuration object
     * */
    public static void deleteBackupFiles(
            final BackupLocationService locationService,
            final BackupManager manager,
            final Backup backup,
            final S3Config s3Config) {

        final PersistProviderFactory providerFactory = new PersistProviderFactory();
        providerFactory.setAwsConfig(s3Config);
        deleteBackupFiles(locationService, manager, backup, providerFactory.getPersistProvider());
    }

    private static void deleteBackupFiles(final BackupLocationService locationService,
                                              final BackupManager manager,
                                              final Backup backup,
                                              final PersistProvider provider) {
        final Path backupLocation = locationService.getBackupFolder(manager.getBackupManagerId(), backup.getBackupId()).getBackupLocation();
        if (provider.exists(backupLocation)) {
            try (Stream<Path> files = provider.walk(backupLocation, Integer.MAX_VALUE)) {
                files.sorted(Comparator.reverseOrder()).forEach(file -> {
                    try {
                        provider.delete(file);
                    } catch (IOException e) {
                        throw new DeleteBackupException("Failed deleting the file " + file + " of backup <" + backup.getBackupId() + ">", e);
                    }
                });
            } catch (final IOException e) {
                throw new DeleteBackupException("Failed deleting fragments of backup <" + backup.getBackupId() + ">", e);
            }
        }
    }

    protected void setBackupLocationService(final BackupLocationService backupLocationService) {
        this.backupLocationService = backupLocationService;
    }

    public void setBackupRepository(final BackupRepository backupRepository) {
        this.backupRepository = backupRepository;
    }
}
