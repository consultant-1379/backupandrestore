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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_FOLDER_SIZE;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_MANAGER_CONFIG_BACKUP_FOLDER;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_USED_SIZE;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.CalendarEventFileService;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.archive.StreamingArchiveService;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerFileService;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.ericsson.adp.mgmt.backupandrestore.util.OrchestratorDataFileService;

/**
 * Responsible for writing/reading backupManager information to/from files.
 */
@Service
public class BackupManagerFileService extends OrchestratorDataFileService<PersistedBackupManager> {
    private static final Logger log = LogManager.getLogger(BackupManagerFileService.class);
    private static final String BACKUP_MANAGER_INFORMATION_FILE_NAME = "backupManagerInformation";
    private static final int DEPTH_OF_BACKUP_MANAGER_INFORMATION_FILE = 2;

    private final List<Version<PersistedBackupManager>> versions = List.of(getDefaultVersion(
        s -> jsonService.parseJsonString(s, PersistedBackupManager.class),
        p -> getFile(BACKUP_MANAGER_INFORMATION_FILE_NAME).equals(p.getFileName())
    ));

    private HousekeepingFileService housekeepingFileService;
    private SchedulerFileService schedulerFileService;
    private PeriodicEventFileService periodicEventFileService;
    private CalendarEventFileService calendarEventFileService;
    private BackupLocationService backupLocationService;
    private VirtualInformationFileService virtualInformationFileService;

    /**
     * Write BackupManager to file.
     * @param backupManager to be written.
     */
    public void writeToFile(final BackupManager backupManager) {
        final Path backupManagerFolder = getBackupManagerFolder(backupManager.getBackupManagerId());
        final Path backupManagerFile = backupManagerFolder.resolve(getFile(BACKUP_MANAGER_INFORMATION_FILE_NAME));
        if (backupManager.getVersion() == null) {
            backupManager.setVersion(getLatestVersion());
        }
        writeFile(backupManagerFolder, backupManagerFile, toJson(backupManager).getBytes(), backupManager.getVersion());
    }

    /**
     * Deletes the persisted backup manager's directory
     * @param backupManagerId the id of the persisted backup manager
     */
    public void delete(final String backupManagerId) {
        final Path backupManagerFolder = getBackupManagerFolder(backupManagerId);
        try {
            deleteDirectory(backupManagerFolder);
        } catch (IOException e) {
            log.error("Failed to delete the backup manager's directory {}", backupManagerFolder, e);
        }
    }

    /**
     * Deletes all the backup data associated with the persisted backup manager
     * @param backupManagerId the id of the persisted backup manager
     */
    public void deleteBackups(final String backupManagerId) {
        final Path backupFolder = backupLocationService.getBackupManagerLocation(backupManagerId);
        try {
            deleteDirectory(backupFolder);
        } catch (IOException e) {
            log.error("Failed to delete the backups directory {}", backupFolder, e);
        }
    }

    /**
     * Gets backupManagers that were written to files.
     * @return persisted backupManagers.
     */
    public List<PersistedBackupManager> getBackupManagers() {
        final ArrayList<PersistedBackupManager> backupManagers = new ArrayList<>();
        if (exists(backupManagersLocation)) {
            backupManagers.addAll(readObjectsFromFiles(backupManagersLocation, p -> null, true));
        }
        return backupManagers;
    }

    /**
     * Retrieves the persistedBackupManager from a backupManagerId
     * @param backupManagerId backup Manager Id to filter
     * @return an optional PersistedBackupManager
     */
    public Optional<PersistedBackupManager> getPersistedBackupManager(final String backupManagerId) {
        return readObjectsFromFiles(backupManagersLocation).stream()
        .filter(m -> m.getBackupManagerId().equals(backupManagerId)).findAny();
    }

    /**
     * Retrieves backups folder size (in bytes) for a Backup Manager.
     * <p>The result of invoking this method is cached.
     * <br>Cached result will be refreshed only if this method is invoked
     * after the following method of any Job type has been executed:
     * <ul><li>{@link com.ericsson.adp.mgmt.backupandrestore.job.Job Job.runJob}
     *
     * @param backupManagerId to be queried.
     * @return backups folder size in bytes.
     */
    @Cacheable(value = BACKUP_FOLDER_SIZE)
    public long getBackupManagerBackupsFolderSize(final String backupManagerId) {
        if (backupLocationService.backupManagerLocationExists(backupManagerId)) {
            return getFolderSize(backupLocationService.getBackupManagerLocation(backupManagerId));
        } else {
            return 0;
        }
    }

    /**
     * Get the sum size of backups folder and backup-managers, which is the used space size in PVC
     * @return the used
     */
    @Cacheable(value = BACKUP_USED_SIZE)
    public long getSumOfBackupUsedSize() {
        return getFolderSize(backupManagersLocation) + getFolderSize(backupLocationService.getBackupLocation());
    }


    /**
     * Returns the Backup Manager folder
     * @param backupManagerId Backup Manager Id
     * @return Path
     */
    public Path getBackupManagerFolder(final String backupManagerId) {
        return this.backupManagersLocation.resolve(backupManagerId);
    }

    /**
     * Writes backup manager, scheduling, housekeeping, periodic and calendar based event configurations of all backupManagers to backup.
     * @param backupManagerId id of backup manager
     * @param backupName backup to write files to
     * @param backupManagers list of backupManagers
     */
    public void backupBROConfig(final String backupManagerId, final String backupName,
                                                          final List<BackupManager> backupManagers) {
        final Path backup = backupLocationService.getBackupFolder(backupManagerId, backupName).getBackupLocation();
        final Path tar = backup.toAbsolutePath().resolve(BACKUP_MANAGER_CONFIG_BACKUP_FOLDER.concat(".tar.gz")).normalize();
        provider.mkdirs(tar.getParent());
        log.info("Backing up BRO configuration in {}", tar);
        try (TarArchiveOutputStream gzOut = new TarArchiveOutputStream(
                new BufferedOutputStream(provider.newOutputStream(tar)))) {
            gzOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            for (final BackupManager backupManager: backupManagers) {
                final Path pathInBackup = Path.of(BACKUP_MANAGER_CONFIG_BACKUP_FOLDER + File.separator + backupManager.getBackupManagerId());
                StreamingArchiveService.passDataToTarOutputStreamLocation(pathInBackup, gzOut,
                        BACKUP_MANAGER_INFORMATION_FILE_NAME + ".json", toJson(backupManager));
                virtualInformationFileService.backupVirtualInfoConfig(backupManager, pathInBackup, gzOut);
                schedulerFileService.backupSchedulerConfig(backupManager, pathInBackup, gzOut);
                housekeepingFileService.backupHousekeepingConfig(backupManager, pathInBackup, gzOut);
                periodicEventFileService.backupPeriodicEvents(backupManager, pathInBackup, gzOut);
                calendarEventFileService.backupCalendarEvents(backupManager, pathInBackup, gzOut);
            }
        } catch (final IOException e) {
            log.error("Error adding BRO configuration data to backup {}", backupName, e);
        }
    }

    /**
     * Create the base backup directory for each agent
     * @param backupManagerId BackupManager Id
     * @param backupName Name of the Backup
     * @param agents List of agents to create the backup-directory
     */
    public void backupAgentCreateBackupDirectory(final String backupManagerId,
                                                 final String backupName, final List<Agent> agents) {
        final Path backup = backupLocationService.getBackupFolder(backupManagerId, backupName).getBackupLocation();
        agents.forEach(agent-> {
            try {
                // the folder holder is used to align with OSMN which can't only create a folder
                provider.mkdirs(backup.resolve(agent.getAgentId()));
                final Path dir = backup.resolve(agent.getAgentId());
                provider.write(dir, dir.resolve("folderHolder"), new byte[0]);
            } catch (Exception e) {
                log.error("Error creating the parent backup directory for {}", agent.getAgentId(), e);
            }
        });
    }

    private String toJson(final BackupManager backupManager) {
        return JsonService.toJsonString(new PersistedBackupManager(backupManager));
    }

    @Autowired
    public void setSchedulerFileService(final SchedulerFileService schedulerFileService) {
        this.schedulerFileService = schedulerFileService;
    }

    @Autowired
    public void setHousekeepingFileService(final HousekeepingFileService housekeepingFileService) {
        this.housekeepingFileService = housekeepingFileService;
    }

    @Autowired
    public void setBackupLocationService(final BackupLocationService backupLocationService) {
        this.backupLocationService = backupLocationService;
    }

    @Autowired
    public void setPeriodicEventFileService(final PeriodicEventFileService periodicEventFileService) {
        this.periodicEventFileService = periodicEventFileService;
    }

    @Autowired
    public void setCalendarEventFileService(final CalendarEventFileService calendarEventFileService) {
        this.calendarEventFileService = calendarEventFileService;
    }

    @Autowired
    public void setVirtualInformationFileService(final VirtualInformationFileService virtualInformationFileService) {
        this.virtualInformationFileService = virtualInformationFileService;
    }

    @Override
    protected int getMaximumDepth() {
        return DEPTH_OF_BACKUP_MANAGER_INFORMATION_FILE;
    }

    @Override
    protected List<Version<PersistedBackupManager>> getVersions() {
        return versions;
    }

}
