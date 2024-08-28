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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.archive.StreamingArchiveService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.ssl.EncryptionService;
import com.ericsson.adp.mgmt.backupandrestore.util.OrchestratorDataFileService;
/**
 * Responsible for writing/reading scheduler information to/from files.
 */
@Service
public class SchedulerFileService extends OrchestratorDataFileService<SchedulerInformation> {
    private static final String SCHEDULER_INFORMATION_FILE = "schedulerInformation";
    private static final int DEPTH_OF_SCHEDULER_INFORMATION_FILE = 2;
    private static final Logger log = LogManager.getLogger(SchedulerFileService.class);
    private EncryptionService encryptor;

    private final List<Version<SchedulerInformation>> versions = List.of(getDefaultVersion(
        s -> jsonService.parseJsonString(s, SchedulerInformation.class),
        p -> getFile(SCHEDULER_INFORMATION_FILE).equals(p.getFileName())
    ));

    private BackupManagerRepository managerRepository;

    /**
     * Write scheduler information to file.
     * @param scheduler to be stored
     */
    public void writeToFile(final Scheduler scheduler) {
        final Path schedulerFile = getBackupManagerFolder(scheduler.getBackupManagerId())
                .resolve(getFile(SCHEDULER_INFORMATION_FILE));
        if (scheduler.getVersion() == null) {
            scheduler.setVersion(getLatestVersion());
        }
        writeFile(getBackupManagerFolder(scheduler.getBackupManagerId()), schedulerFile,
                toJson(scheduler).getBytes(), scheduler.getVersion());
    }

    /**
     * Gets scheduler information written to file.
     * @param backupManagerId of backupmanager to fetch scheduler information from.
     * @return persisted scheduler information of a backupManager.
     */
    public SchedulerInformation getPersistedSchedulerInformation(final String backupManagerId) {
        final List<SchedulerInformation> schedulerInformation = new ArrayList<>();
        if (exists(backupManagersLocation)) {
            final Path localScheduler = getBackupManagerFolder(backupManagerId);
            schedulerInformation.addAll(readObjectsFromFiles(localScheduler, path ->  getDefaultScheduler(path, backupManagerId)));
        }
        final SchedulerInformation returnVal = schedulerInformation.get(0);
        if (!returnVal.getAutoExportPassword().isEmpty()) { // Only attempt to decrypt if the password is set
            returnVal.setAutoExportPassword(encryptor.decrypt(returnVal.getAutoExportPassword()));
        }
        return returnVal;
    }

    /**
     * Backup schedulerInformation.json information file in backup
     * @param backupManager of backupmanager to fetch scheduler information from.
     * @param storeLocation storeLocation
     * @param gzOut tar output stream
     */
    public void backupSchedulerConfig(final BackupManager backupManager, final Path storeLocation, final TarArchiveOutputStream gzOut) {
        StreamingArchiveService.passDataToTarOutputStreamLocation(storeLocation, gzOut,
                SCHEDULER_INFORMATION_FILE + ".json", toJson(backupManager.getScheduler()));
    }

    @Override
    protected int getMaximumDepth() {
        return DEPTH_OF_SCHEDULER_INFORMATION_FILE;
    }

    @Override
    protected List<Version<SchedulerInformation>> getVersions() {
        return versions;
    }

    private Scheduler getDefaultScheduler (final Path localPath, final String backupManagerId) {
        final Scheduler scheduler = managerRepository.createAndPersistNewScheduler(backupManagerId);
        scheduler.persist();
        log.warn ("Scheduler file {} corrupt, setting default values {}",
                localPath, scheduler.getScheduledBackupName());
        return scheduler;
    }

    /**
     * Convert a scheduler object to a json string representing it's state. Encrypts autoExportPassword.
     * */
    private String toJson(final Scheduler scheduler) {
        final SchedulerInformation copy = new SchedulerInformation(scheduler);
        if (!copy.getAutoExportPassword().isEmpty()) { // Only attempt to encrypt if the password is set
            copy.setAutoExportPassword(encryptor.encrypt(scheduler.getAutoExportPassword()));
        }
        return jsonService.toJsonString(copy);
    }

    private Path getBackupManagerFolder(final String backupManagerId) {
        return backupManagersLocation.resolve(backupManagerId);
    }

    @Autowired
    public void setEncryptor(final EncryptionService encryptor) {
        this.encryptor = encryptor;
    }

    @Autowired
    public void setManagerRepository(final BackupManagerRepository managerRepository) {
        this.managerRepository = managerRepository;
    }

}
