/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence;

import com.ericsson.adp.mgmt.backupandrestore.archive.StreamingArchiveService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.VirtualInformation;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.util.OrchestratorDataFileService;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Fileservice to manage the persistence of VirtualInformation objects
 * */
@Service
public class VirtualInformationFileService extends OrchestratorDataFileService<VirtualInformation> {
    private static final String VIRTUAL_INFORMATION_FILE_NAME = "virtualInformation";

    private final List<Version<VirtualInformation>> versions = List.of(getDefaultVersion(
        s -> jsonService.parseJsonString(s, VirtualInformation.class),
        p -> getFile(VIRTUAL_INFORMATION_FILE_NAME).equals(p.getFileName())
    ));

    /**
     * Write BackupManager to file.
     * @param backupManager to be written.
     */
    public void writeToFile(final BackupManager backupManager) {
        final Path backupManagerFolder = getBackupManagerFolder(backupManager.getBackupManagerId());
        final Path virtualInformationFile = backupManagerFolder.resolve(getFile(VIRTUAL_INFORMATION_FILE_NAME));
        final VirtualInformation information = backupManager.getVirtualInformation();
        if (information.getVersion() == null) {
            information.setVersion(getLatestVersion());
        }
        writeFile(backupManagerFolder, virtualInformationFile, toJson(information).getBytes(), information.getVersion());
    }

    /**
     * Backup the virtualInformation.json file
     * @param backupManager the backup manager to fetch the virtual information from
     * @param storeLocation the path in tar where the file will be stored
     * @param gzOut the tar output stream
     */
    public void backupVirtualInfoConfig(final BackupManager backupManager, final Path storeLocation, final TarArchiveOutputStream gzOut) {
        StreamingArchiveService.passDataToTarOutputStreamLocation(storeLocation, gzOut,
                VIRTUAL_INFORMATION_FILE_NAME + ".json", toJson(backupManager.getVirtualInformation()));
    }

    /**
     * Retrieves the persistedBackupManager from a backupManagerId
     * @param backupManagerId backup Manager Id to filter
     * @return an optional PersistedBackupManager
     */
    public Optional<VirtualInformation> getVirtualInformation(final String backupManagerId) {
        return readObjectsFromFiles(getBackupManagerFolder(backupManagerId)).stream()
                .findAny();
    }

    private String toJson(final VirtualInformation information) {
        return jsonService.toJsonString(information);
    }

    private Path getBackupManagerFolder(final String backupManagerId) {
        return this.backupManagersLocation.resolve(backupManagerId);
    }

    @Override
    protected List<Version<VirtualInformation>> getVersions() {
        return versions;
    }
}
