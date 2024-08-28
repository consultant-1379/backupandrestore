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
package com.ericsson.adp.mgmt.backupandrestore.backup;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.exception.DeleteBackupException;
import com.ericsson.adp.mgmt.backupandrestore.exception.JsonParsingException;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.util.BackupFileValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.ericsson.adp.mgmt.backupandrestore.util.OrchestratorDataFileService;

/**
 * Responsible for writing/reading backups to/from json files
 */
@Service
public class BackupFileService extends OrchestratorDataFileService<PersistedBackup> {

    public static final String BACKUP_FOLDER = "backups";

    private static final Logger logger = LogManager.getLogger(BackupFileService.class);

    private final List<Version<PersistedBackup>> versions = List.of(getDefaultVersion(
        this::parseJsonStringToPersistedObject,
        p -> p.getFileName().toString().endsWith(JSON_EXTENSION)
        ));


    private BackupFileValidator backupFileValidator;
    /**
     * Writes backup to file.
     * @param backup to be written.
     */
    public void writeToFile(final Backup backup) {
        final Path backupFolder = getBackupFolder(backup.getBackupManagerId());
        final Path backupFile = backupFolder.resolve(getFile(backup.getBackupId()));
        if (backup.getVersion() == null) {
            backup.setVersion(getLatestVersion());
        }
        writeFile(backupFolder, backupFile, toJson(backup).getBytes(), backup.getVersion());
    }

    /**
     * Gets persisted backups from a backupManager.
     * @param backupManagerId owner of backups.
     * @return list of backups.
     */
    public List<PersistedBackup> getBackups(final String backupManagerId) {
        final Path backupFolder = getBackupFolder(backupManagerId);
        if (exists(backupFolder)) {
            return readObjectsFromFiles(backupFolder, path -> getDefaultBackup(path, backupManagerId));
        }
        return new ArrayList<>();
    }

    private PersistedBackup getDefaultBackup(final Path backupFilePath, final String backupManagerId) {
        final String backupId = backupFilePath.getFileName().toString().split("\\.json")[0];
        logger.error("Unreadable backup file with id <{}> discovered for backup manager <{}>", backupId, backupManagerId);

        // using the current backup manager and the backup id as the filename
        // we add an entry in our backup list with status set to INCOMPLETE,
        // During BRO initialization all incomplete backups are marked as corrupted
        final PersistedBackup backup = new PersistedBackup();
        backup.setBackupId(backupId);
        backup.setBackupManagerId(backupManagerId);
        backup.setStatus(BackupStatus.INCOMPLETE);
        backup.setCreationTime(OffsetDateTime.now().toString());
        logger.error("Backup <{}> will be marked as corrupted at the end of backup manager initialization sequence", backupId);

        return backup;
    }


    /**
     * Deletes backup.
     * @param backupManagerId owner of backup.
     * @param backupId to be deleted.
     */
    public void deleteBackup(final String backupManagerId, final String backupId) {
        final Path backupFolder = getBackupFolder(backupManagerId);
        final List<PersistedBackup> backups = getBackups(backupManagerId);
        Path backupFile = backupFolder.resolve(getFile(backupId));
        final Optional<PersistedBackup> backupToRemove = Optional.ofNullable(backups.stream()
                .filter(bck->bck.backupId.equals(backupId)).findAny().orElse(null));
        if (backupToRemove.isPresent()) {
            backupFile = backupToRemove.get().getVersion().fromBase(backupFile);
            try {
                delete(backupFile);
            } catch (final IOException e) {
                throw new DeleteBackupException("Failed to delete backup <" + backupId + "> of backupManager <" + backupManagerId + ">", e);
            }
        } else {
            throw new DeleteBackupException("Can't find backup <" + backupId + "> of backupManager <" + backupManagerId + "> to delete");
        }
    }

    /**
     * Read an importedBackup from String and create a Backup object from it.
     * @param contentOfFile the backup file content.
     * @return backup object.
     */
    public PersistedBackup readBackup(final String contentOfFile) {
        final PersistedBackup persistedBackup = parseJsonStringToPersistedObject(contentOfFile).orElseThrow(JsonParsingException::new);
        backupFileValidator.validateBackupFile(persistedBackup);
        return persistedBackup;
    }

    /**
     * Gets the BackupFolder
     * @param backupManagerId backupManagerId
     * @return Path
     */
    public Path getBackupFolder(final String backupManagerId) {
        return backupManagersLocation.resolve(backupManagerId).resolve(BACKUP_FOLDER);
    }

    /**
     * Gets the BackupFilePath
     * @param backupManagerId backupManagerId
     * @param backupName backupName
     * @return Path
     */
    public Path getBackupFilePath(final String backupManagerId, final String backupName) {
        return backupManagersLocation.resolve(backupManagerId).resolve(BACKUP_FOLDER).resolve(backupName + ".json");
    }

    private String toJson(final Backup backup) {
        final String timezone = String.format("{\"timezone\":\"%s\"}", backup.getCreationTime().getOffset());
        return jsonService.toJsonString(new PersistedBackup(backup)).concat(timezone);
    }

    /**
     * Parse importedBackup from String and create a persisted Backup object from it
     * @param jsonString imported backup
     * @return Persisted backup
     */
    protected Optional<PersistedBackup> parseJsonStringToPersistedObject(final String jsonString) {
        final Optional<PersistedBackup> backup = jsonService.parseJsonString(jsonString, PersistedBackup.class);
        if (backup.isPresent() && (getTimeZone(jsonString) != null)) {
            final ZoneOffset offset = ZoneOffset.of(getTimeZone(jsonString));
            backup.get().setCreationTime(DateTimeUtils.offsetDateTimeFrom(backup.get().getCreationTime(), offset));
        }
        return backup;
    }

    @Autowired
    public void setBackupFileValidator(final BackupFileValidator backupFileValidator) {
        this.backupFileValidator = backupFileValidator;
    }

    @Override
    protected List<Version<PersistedBackup>> getVersions() {
        return versions;
    }
}
