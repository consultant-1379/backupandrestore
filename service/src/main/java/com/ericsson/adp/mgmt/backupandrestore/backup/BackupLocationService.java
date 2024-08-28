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
package com.ericsson.adp.mgmt.backupandrestore.backup;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import com.ericsson.adp.mgmt.backupandrestore.job.FragmentFolder;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProvider;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProviderFactory;
import com.ericsson.adp.mgmt.data.Metadata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;

/**
 * Prepares backup location
 */
@Service
public class BackupLocationService {
    private static final Path DEFAULT_BACKUP_LOCATION = Paths.get(System.getProperty("java.io.tmpdir"), "backups")
            .toAbsolutePath().normalize();
    private Path backupLocation = DEFAULT_BACKUP_LOCATION;
    private PersistProvider provider;

    /**
     * Setup the persistence provider used by the file service
     * @param configuration - provider configuration used
     * */
    @Autowired
    public void setProvider(final PersistProviderFactory configuration) {
        provider = configuration.getPersistProvider();
    }

    /**
     * Get where to store backup data
     *
     * @param backupManager
     *            owner of backup
     * @param backupName
     *            message sent by agent
     * @return location of backup
     */
    public BackupFolder getBackupFolder(final BackupManager backupManager, final String backupName) {
        return getBackupFolder(backupManager.getBackupManagerId(), backupName);
    }

    /**
     * Get where to store backup data
     *
     * @param backupManagerId
     *            owner of backup
     * @param backupName
     *            message sent by agent
     * @return location of backup
     */
    public BackupFolder getBackupFolder(final String backupManagerId, final String backupName) {
        final Path backupPath = backupLocation.resolve(backupManagerId).resolve(backupName);
        return new BackupFolder(backupPath);
    }

    /**
     * Gets the backupManagerLocation
     *
     * @param backupManagerId
     *            backupManagerId
     * @return the path
     */
    public Path getBackupManagerLocation(final String backupManagerId) {
        return backupLocation.resolve(backupManagerId);
    }

    /**
     * Get all the agent id from the backup folder
     * @param backupManager the backup manager
     * @param backupName the name of the backup
     * @return all the agent id involved in this backup folder
     */
    public List<String> getAgentIdsForBackup(final BackupManager backupManager, final String backupName) {
        final BackupFolder backupFolder = getBackupFolder(backupManager.getBackupManagerId(), backupName);
        return provider.list(backupFolder.getBackupLocation())
                .stream()
                .filter(provider::isDir)
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    /**
     * Gets a FragmentFolder contained under this BackupFolder
     * @param metadata Metadata for the fragment
     * @param backupManagerId id of the backup manager
     * @param backupName name of the backup
     * @return FragmentFolder for the specified fragment
     */
    public FragmentFolder getFragmentFolder(final Metadata metadata, final String backupManagerId, final String backupName) {
        final Path fragmentPath = getBackupFolder(backupManagerId, backupName).getBackupLocation()
                .resolve(metadata.getAgentId())
                .resolve(metadata.getFragment().getFragmentId());
        return new FragmentFolder(fragmentPath);
    }

    /**
     * Sets backup location
     *
     * @param backupLocation
     *            where to store backup files.
     */
    @Value("${backup.location}")
    public void setBackupLocation(final String backupLocation) {
        if (backupLocation != null && !backupLocation.isEmpty()) {
            this.backupLocation = Paths.get(backupLocation);
        }
    }

    /**
     * Get the location of the backup folder
     * @return the location of the backup folder
     */
    public Path getBackupLocation() {
        return backupLocation;
    }

    /**
     * Checks if Backup Manager Location exists
     *
     * @param backupManagerId
     *            owner of backup
     * @return returns true if it does
     */
    public boolean backupManagerLocationExists(final String backupManagerId) {
        return provider.exists(getBackupManagerLocation(backupManagerId));
    }

}
