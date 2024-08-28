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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence;

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
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.HousekeepingInformation;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMHousekeepingJson;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.util.OrchestratorDataFileService;

/**
 * Responsible for writing/reading housekeeping information to/from files.
 */
@Service
public class HousekeepingFileService extends OrchestratorDataFileService<HousekeepingInformation> {

    private static final Logger log = LogManager.getLogger(HousekeepingFileService.class);
    private static final String HOUSEKEEPING_INFORMATION_FILE = "housekeepingInformation";
    private static final int DEPTH_OF_HOUSEKEEPING_INFORMATION_FILE = 2;

    private final List<Version<HousekeepingInformation>> versions = List.of(getDefaultVersion(
        s -> jsonService.parseJsonString(s, HousekeepingInformation.class),
        p -> getFile(HOUSEKEEPING_INFORMATION_FILE).equals(p.getFileName())
    ));

    private BackupManagerRepository managerRepository;

    /**
     * Write housekeeping information to file.
     * @param housekeeping to be stored
     */
    public void writeToFile(final Housekeeping housekeeping) {
        final Path housekeepingFile = getBackupManagerFolder(housekeeping.getBackupManagerId())
                .resolve(getFile(HOUSEKEEPING_INFORMATION_FILE));
        if (housekeeping.getVersion() == null) {
            housekeeping.setVersion(getLatestVersion());
        }
        writeFile(getBackupManagerFolder(housekeeping.getBackupManagerId()), housekeepingFile,
                toJson(housekeeping).getBytes(), housekeeping.getVersion());
    }

    /**
     * Gets housekeeping information written to file.
     * @param backupManagerId of backupmanager to fetch housekeeping information from.
     * @return persisted housekeeping information of a backupManager.
     */
    public HousekeepingInformation getPersistedHousekeepingInformation(final String backupManagerId) {
        final List<HousekeepingInformation> housekeepingInformations = new ArrayList<>();
        if (exists(backupManagersLocation)) {
            final Path localHouseKeeping = getBackupManagerFolder(backupManagerId);
            housekeepingInformations.addAll(readObjectsFromFiles(localHouseKeeping, path -> getDefaultHousekeeping(path, backupManagerId)));
        }
        return housekeepingInformations.get(0);
    }

    /**
     * Backup housekeepingInformation.json file in backup
     * @param backupManager of backupmanager to fetch housekeeping information from.
     * @param storeLocation storeLocation
     * @param gzOut tar output stream
     */
    public void backupHousekeepingConfig(final BackupManager backupManager, final Path storeLocation, final TarArchiveOutputStream gzOut) {
        StreamingArchiveService.passDataToTarOutputStreamLocation(storeLocation, gzOut,
                HOUSEKEEPING_INFORMATION_FILE + ".json", toJson(backupManager.getHousekeeping()));
    }

    @Override
    protected int getMaximumDepth() {
        return DEPTH_OF_HOUSEKEEPING_INFORMATION_FILE;
    }

    private Housekeeping getDefaultHousekeeping(final Path localHouseKeepingFile, final String backupManagerId) {
        final Housekeeping housekeeping = managerRepository.createHousekeeping(backupManagerId);
        housekeeping.persist();
        log.warn ("Corrupted, housekeeping file {}, setting new values - Max Backups: {} autoDelete:{} ",
                localHouseKeepingFile.getFileName(), housekeeping.getMaxNumberBackups(), housekeeping.getAutoDelete());
        return housekeeping;
    }

    @Override
    protected List<Version<HousekeepingInformation>> getVersions() {
        return versions;
    }

    private String toJson(final Housekeeping housekeeping) {
        return jsonService.toJsonString(new BRMHousekeepingJson(housekeeping));
    }

    private Path getBackupManagerFolder(final String backupManagerId) {
        return backupManagersLocation.resolve(backupManagerId);
    }

    @Autowired
    public void setManagerRepository(final BackupManagerRepository managerRepository) {
        this.managerRepository = managerRepository;
    }
}
