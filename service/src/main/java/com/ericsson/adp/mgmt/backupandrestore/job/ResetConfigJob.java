/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.job;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionFileService;
import com.ericsson.adp.mgmt.backupandrestore.archive.ArchiveUtils;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.BackupManagerFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.persistence.HousekeepingFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.ScheduledEventHandler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerFileService;
import com.ericsson.adp.mgmt.backupandrestore.exception.JobFailedException;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProvider;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_MANAGER_CONFIG_BACKUP_FOLDER;

/**
 * A job to reset a BRMs config, given a backup containing a snapshot of that BRMs config.
 *
 * */
public class ResetConfigJob extends Job {
    public static final String RESET_BRM_SUFFIX = "-bro";
    public static final String RESET_BACKUP_SUFFIX = ".reset.bak";
    public static final String RESET_TEMP_SUFFIX = ".reset.temp";
    public static final String MARKER_SUFFIX = ".reset.marker";
    public static final String REPLACE_PHASE_MARKER = "pre_replace" + MARKER_SUFFIX;
    public static final String DELETE_PHASE_MARKER = "post_replace" + MARKER_SUFFIX;

    private static final Logger log = LogManager.getLogger(ResetConfigJob.class);
    private static final byte[] MARKER_CONTENTS = "MARKER FILE FOR RESET OPERATIONS".getBytes(StandardCharsets.UTF_8);

    private Backup backup;
    private BackupManagerFileService brmFileService;
    private HousekeepingFileService housekeepingFileService;
    private ScheduledEventHandler handler;
    private SchedulerFileService schedulerFileService;
    private PersistProvider provider;
    private BackupLocationService backupLocationService;
    private boolean finished;

    @Override
    protected void triggerJob() {
        // We must be constructed with a "-bro" vBRMs parent BRM. The job factory should take care of this, and
        // vBRMs can't be reset.
        final Optional<BackupManager> optionalBackupManager = backupManager.getParent();
        if (optionalBackupManager.isEmpty() || !backupManager.getBackupManagerId().endsWith(RESET_BRM_SUFFIX)) {
            throw new JobFailedException("Reset BRM must be a vBRM ending with -bro");
        } else {
            // We'll work with the parent BRM for now
            backupManager = optionalBackupManager.get();
        }
        // Using OWNED because we can only restore the config in an owned backup
        backup = backupManager.getBackup(action.getBackupName(), Ownership.OWNED);
        try {
            deleteMarker(DELETE_PHASE_MARKER);
            doReset();
        } catch (Exception resetException) {
            log.error("Failed to do reset", resetException);
            throw new JobFailedException(resetException);
        }
        finished = true;
    }

    private void doReset() throws IOException {
        saveExistingConfig();
        writeTempConfigData();
        writeMarker(REPLACE_PHASE_MARKER);
        deleteExistingConfig();
        replaceConfigFiles();
        reload();
        deleteTempConfigData();
        writeMarker(DELETE_PHASE_MARKER);
        deleteExistingConfigBackup();
        deleteMarker(REPLACE_PHASE_MARKER);
        deleteMarker(DELETE_PHASE_MARKER);
    }

    private void revert() throws IOException {
        if (markerExists(REPLACE_PHASE_MARKER) && !markerExists(DELETE_PHASE_MARKER)) {
            // We failed before we started deleting the original config data backups, so we can do a perfect reset. Otherwise,
            // we're forced to just log that the operation failed in a cleanup step, which is unfortunate but still "atomic"
            rollbackReset();
            reload();
        } else if (markerExists(REPLACE_PHASE_MARKER) && markerExists(DELETE_PHASE_MARKER)) {
            // If both of these markers exist, we successfully did the configuration reset, but then failed in cleanup.
            // This is still an action failure, but it's a "action failed successfully", which is sad but fine I think.
            // We log this warning to tell the user "hey you don't need to re-run this action, the work is done"
            log.warn("Action failing due to failed cleanup step, but BRM configuration has been reset");
        }
        // If neither marker exists, we simply failed during initial setup, and cna just take the necessary cleanup steps
        // and fail out
        deleteTempConfigData();
        deleteExistingConfigBackup();
        deleteMarker(REPLACE_PHASE_MARKER);
        deleteMarker(DELETE_PHASE_MARKER);
    }

    @Override
    protected boolean didFinish() {
        return finished;
    }

    @Override
    protected void completeJob() {
        log.info("Config of BRM {} reset to state at time of backup {}", backupManager.getBackupManagerId(), backup.getBackupId());
    }

    @Override
    public void fail() {
        try {
            revert();
        } catch (Exception revertException) {
            log.error("Failed to revert reset", revertException);
            throw new JobFailedException(revertException);
        }
    }

    private void saveExistingConfig() throws IOException {
        final Path brmBase = brmFileService.getBackupManagerFolder(backupManager.getBackupManagerId());
        log.info("Cloning existing config data in {}", brmBase);
        walkFilterMap(brmBase, p -> !p.toString().contains(File.separator + ActionFileService.ACTIONS_FOLDER) &&
                !p.toString().contains(File.separator + BackupFileService.BACKUP_FOLDER), src -> {
                try {
                    final Path dst = src.getParent().resolve(src.getFileName().toString() + RESET_BACKUP_SUFFIX);
                    log.info("Cloning {} to {}", src, dst);
                    provider.copy(src, dst, true);
                } catch (IOException exception) {
                    return exception;
                }
                return null;
            });
    }

    private void writeTempConfigData() throws IOException {
        final Path toBackup = backupLocationService.getBackupFolder(backupManager.getBackupManagerId(), backup.getName()).getBackupLocation();
        final Path tar = toBackup.toAbsolutePath().resolve(BACKUP_MANAGER_CONFIG_BACKUP_FOLDER.concat(".tar.gz")).normalize();
        if (!provider.exists(tar)) {
            throw new JobFailedException("Cannot reset config, no config backup found");
        }
        log.info("Unpacking config backup at {}", tar);
        try (TarArchiveInputStream inputStream = new TarArchiveInputStream(provider.newInputStream(tar))) {
            TarArchiveEntry tarEntry = inputStream.getNextTarEntry();
            while (tarEntry != null) {
                writeTemporary(inputStream, tarEntry);
                tarEntry = inputStream.getNextTarEntry();
            }
        }
    }

    private void writeTemporary(final TarArchiveInputStream inputStream, final TarArchiveEntry entry) throws IOException {
        final Path entryLocation = brmFileService.getBackupManagerFolder(backupManager.getBackupManagerId())
                .getParent().getParent().resolve(entry.getName()).toAbsolutePath();
        final Path writeLocation = entryLocation.getParent().resolve(entryLocation.getFileName().toString() + RESET_TEMP_SUFFIX);
        final boolean isDir = entry.isDirectory();
        if (!writeLocation.startsWith(brmFileService.getBackupManagerFolder(backupManager.getBackupManagerId()))) {
            log.debug("Skipping {}, doesn't belong to this BRM", entryLocation);
            return;
        }
        // We don't care at all about folder reconstruction - we're only overwriting things that are here already
        if (isDir) {
            log.debug("Skipping {}, is directory", entryLocation);
            return;
        }
        log.info("Writing reset config data to temporary location {}", writeLocation);
        try (OutputStream out = provider.newOutputStream(writeLocation)) {
            final byte [] data = new byte[ArchiveUtils.BLOCK_SIZE];
            int len = inputStream.read(data);
            while (len > 0) {
                out.write(data, 0, len);
                len = inputStream.read(data);
            }
        }
    }

    private void deleteExistingConfig() throws IOException {
        final Path toWalk = brmFileService.getBackupManagerFolder(backupManager.getBackupManagerId());
        log.info("Deleting existing configuration files");
        deleteAllUnder(toWalk, p -> !p.toString().endsWith(RESET_TEMP_SUFFIX) &&
                !p.toString().endsWith(RESET_BACKUP_SUFFIX) &&
                !p.toString().endsWith(REPLACE_PHASE_MARKER) &&
                !p.toString().endsWith(DELETE_PHASE_MARKER) &&
                !p.toString().contains(File.separator + ActionFileService.ACTIONS_FOLDER) &&
                !p.toString().contains(File.separator + BackupFileService.BACKUP_FOLDER));
    }

    private void replaceConfigFiles() throws IOException {
        final Path brmBase = brmFileService.getBackupManagerFolder(backupManager.getBackupManagerId());
        log.info("Overwriting config data in {}", brmBase);
        walkFilterMap(brmBase, p -> p.getFileName().toString().endsWith(RESET_TEMP_SUFFIX), src -> {
            try {
                final int suffixIndex = src.getFileName().toString().lastIndexOf(RESET_TEMP_SUFFIX);
                final Path dst = src.getParent().resolve(src.getFileName().toString().substring(0, suffixIndex));
                log.info("Cloning {} to {}", src, dst);
                provider.copy(src, dst, true);
            } catch (IOException exception) {
                return exception;
            }
            return null;
        });
    }

    private void reload() throws IOException {
        // The only things stored in the config backup are the BRM itself, housekeeping and the scheduler, so only reload them
        backupManager.reload(brmFileService);
        backupManager.getScheduler().reload(handler, schedulerFileService);
        backupManager.getHousekeeping().reload(housekeepingFileService);
    }

    private void deleteTempConfigData() throws IOException {
        final Path toWalk = brmFileService.getBackupManagerFolder(backupManager.getBackupManagerId());
        log.info("Deleting temp new config data under {}", toWalk);
        deleteAllUnder(toWalk, p -> p.getFileName().toString().endsWith(RESET_TEMP_SUFFIX));
    }

    private void deleteExistingConfigBackup() throws IOException {
        final Path toWalk = brmFileService.getBackupManagerFolder(backupManager.getBackupManagerId());
        log.info("Deleting old config data clones under {}", toWalk);
        deleteAllUnder(toWalk, p -> p.getFileName().toString().endsWith(RESET_BACKUP_SUFFIX));
    }

    private void deleteAllUnder(final Path dir, final Predicate<Path> filter) throws IOException {
        walkFilterMap(dir, filter, p -> {
            try {
                log.info("Deleting {}", p);
                provider.delete(p);
            } catch (IOException exception) {
                return exception;
            }
            return null;
        });
    }

    private void walkFilterMap(final Path dir, final Predicate<Path> filter, final Function<Path, IOException> map) throws IOException {
        final Optional<IOException> failure = provider.walk(dir, Integer.MAX_VALUE)
                .filter(p -> !provider.isDir(p))
                .filter(filter)
                .map(map)
                .filter(Objects::nonNull)
                .findFirst();
        if (failure.isPresent()) {
            throw failure.get();
        }
    }

    private void rollbackReset() throws IOException {
        final Path toWalk = brmFileService.getBackupManagerFolder(backupManager.getBackupManagerId());
        log.info("Rolling back modified config data under {}", toWalk);
        // Walk to delete the modified config data
        deleteExistingConfig();
        // Walk to re-place originals where they should be
        walkFilterMap(toWalk, p -> p.getFileName().toString().endsWith(RESET_BACKUP_SUFFIX), clone -> {
            final int suffixIndex = clone.getFileName().toString().lastIndexOf(RESET_BACKUP_SUFFIX);
            final Path dst = clone.getParent().resolve(clone.getFileName().toString().substring(0, suffixIndex));
            try {
                provider.copy(clone, dst, true);
            } catch (IOException e) {
                return e;
            }
            return null;
        });
        deleteExistingConfigBackup();
    }

    private void writeMarker(final String marker) {
        final Path brmBase = brmFileService.getBackupManagerFolder(backupManager.getBackupManagerId());
        provider.write(brmBase, brmBase.resolve(marker), MARKER_CONTENTS);
    }

    private boolean markerExists(final String marker) {
        final Path brmBase = brmFileService.getBackupManagerFolder(backupManager.getBackupManagerId());
        return provider.exists(brmBase.resolve(marker));
    }

    private void deleteMarker(final String marker) throws IOException {
        final Path brmBase = brmFileService.getBackupManagerFolder(backupManager.getBackupManagerId());
        if (provider.exists(brmBase.resolve(marker))) {
            provider.delete(brmBase.resolve(marker));
        }
    }

    public void setBrmFileService(final BackupManagerFileService brmFileService) {
        this.brmFileService = brmFileService;
    }

    public void setProvider(final PersistProvider provider) {
        this.provider = provider;
    }

    public void setBackupLocationService(final BackupLocationService backupLocationService) {
        this.backupLocationService = backupLocationService;
    }

    public void setHandler(final ScheduledEventHandler handler) {
        this.handler = handler;
    }

    public void setHousekeepingFileService(final HousekeepingFileService housekeepingFileService) {
        this.housekeepingFileService = housekeepingFileService;
    }

    public void setSchedulerFileService(final SchedulerFileService schedulerFileService) {
        this.schedulerFileService = schedulerFileService;
    }
}
