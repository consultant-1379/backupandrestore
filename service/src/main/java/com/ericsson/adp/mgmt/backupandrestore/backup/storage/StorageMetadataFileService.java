/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup.storage;

import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.exception.FilePersistenceException;
import com.ericsson.adp.mgmt.backupandrestore.persist.FileService;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;

/**
 * Service for persisting metadata relating to the BR-Internal-Storage interface.
 */
@Service
public class StorageMetadataFileService extends FileService<StorageMetadata> {

    private static final Logger logger = LogManager.getLogger(StorageMetadataFileService.class);
    private static final String BR_INTERNAL_STORAGE_VERSION = "1.4.0";
    private static final String STORAGE_METADATA_FILENAME = "brIntStorage.json";

    private final List<Version<StorageMetadata>> versions = List.of(getDefaultVersion(
        s -> jsonService.parseJsonString(s, StorageMetadata.class),
        p -> p.getFileName().toString().endsWith(JSON_EXTENSION)
    ));

    private BackupLocationService backupLocationService;

    /**
     * Creates the br-internal-storage metadata file for a backup
     *
     * @param backupManagerId The backup manager which owns the backup
     * @param backupName The name of the backup
     */
    public void createStorageMetadataFile(final String backupManagerId, final String backupName) {
        final StorageMetadata storageMetadata = new StorageMetadata(BR_INTERNAL_STORAGE_VERSION);
        storageMetadata.setVersion(getLatestVersion());
        final Path backupPath = backupLocationService.getBackupFolder(backupManagerId, backupName).getBackupLocation();

        try {
            writeFile(
                backupPath,
                backupPath.resolve(STORAGE_METADATA_FILENAME),
                jsonService.toJsonString(storageMetadata).getBytes(),
                    storageMetadata.getVersion()
            );
        } catch (final Exception exception) {
            logger.error("Error while writing {} file.", STORAGE_METADATA_FILENAME);
            throw new FilePersistenceException(exception);
        }
    }

    @Override
    protected List<Version<StorageMetadata>> getVersions() {
        return versions;
    }

    @Autowired
    public void setBackupLocationService(final BackupLocationService backupLocationService) {
        this.backupLocationService = backupLocationService;
    }
}
