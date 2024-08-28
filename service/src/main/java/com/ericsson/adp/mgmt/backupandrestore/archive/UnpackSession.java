/*------------------------------------------------------------------------------
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
package com.ericsson.adp.mgmt.backupandrestore.archive;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_DATA_FOLDER_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_FILE_FOLDER_NAME;

/**
 * Mostly stateless class to unpack an archived backup, by consuming archive entries one at a time and
 * constructing the relevant files on disk
 *
 * */
public class UnpackSession {
    private static final Logger log = LogManager.getLogger(UnpackSession.class);

    private final Path fileDir;
    private final Path dataDir;
    private final InputStream input;

    private final Set<Path> created = new HashSet<>();
    private final ArchiveUtils utils;

    /**
     * Constructor
     * @param fileDir - location to store the extracted backup's metadata .json file (under the /bro/backupManagers folder)
     * @param dataDir - location to store the extracted backup's data (under the /bro/backups folder)
     * @param input - input stream to read a given tar entries data from
     * @param utils - the archiveUtils this unpacking session relies on
     * */
    public UnpackSession(final Path fileDir, final Path dataDir, final InputStream input, final ArchiveUtils utils) {
        this.fileDir = fileDir;
        this.dataDir = dataDir;
        this.input = input;
        this.utils = utils;
    }

    /**
     * Consume a tar entry and write it to disk
     * @param entry - the tar entry to consume and write to disk
     * @throws IOException if writing to sile fails
     * */
    public void next(final TarArchiveEntry entry) throws IOException {
        final Path entryLocation = Paths.get(entry.getName());
        // Get the position in the entry's name parts where these markers appear. This
        // lets us find the tarball-specific prefix of the entry, and then replace it
        // with the right parent path before storing
        final Optional<Integer> fileMarkerPrefixDepth = indexIn(entryLocation, BACKUP_FILE_FOLDER_NAME);
        final Optional<Integer> dataMarkerPrefixDepth = indexIn(entryLocation, BACKUP_DATA_FOLDER_NAME);
        // We want to ensure we use the shallower of the two prefixes here, to support folders named
        // "backupfile" under the "backupdata" location (or vice versa)
        boolean isFile = fileMarkerPrefixDepth.isPresent();
        if (fileMarkerPrefixDepth.isPresent() && dataMarkerPrefixDepth.isPresent()) {
            final int filePrefix = fileMarkerPrefixDepth.get();
            final int dataPrefix = dataMarkerPrefixDepth.get();
            isFile = filePrefix < dataPrefix;
        }
        // If this is the backup metadata file, save it to the backup file directory
        if (isFile) {
            final Path tarPrefix = entryLocation.subpath(0, fileMarkerPrefixDepth.get() + 1);
            save(fileDir.resolve(tarPrefix.relativize(entryLocation)), entry);
            return;
        }
        // Otherwise, if we found the data prefix, save this file to the data directory
        if (dataMarkerPrefixDepth.isPresent()) {
            final Path tarPrefix = entryLocation.subpath(0, dataMarkerPrefixDepth.get() + 1);
            save(dataDir.resolve(tarPrefix.relativize(entryLocation)), entry);
            return;
        }
        log.warn("Found tar entry not under backupfile or backupdata prefixes, contravening IWD: {}", entryLocation);
    }

    private Optional<Integer> indexIn(final Path path, final String part) {
        for (int i = 0; i < path.getNameCount(); i++) {
            if (path.getName(i).toString().equals(part)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private void save(final Path toSave, final TarArchiveEntry entry) throws IOException {
        log.info("Saving tar entry {} to location {}", entry.getName(), toSave);

        if (entry.isDirectory()) {
            createDirectory(toSave);
            return;
        }

        if (toSave.getParent() != null && utils.getProvider().exists(toSave.getParent())) {
            createDirectory(toSave.getParent());
        }

        try (OutputStream out = utils.getOutputStream(toSave)) {
            created.add(toSave);
            final byte [] btoRead = new byte[ArchiveUtils.BLOCK_SIZE];
            int len = input.read(btoRead);
            while (len != -1) {
                out.write(btoRead, 0, len);
                len = input.read(btoRead);
            }
        }
    }

    private void createDirectory(final Path toCreate) throws IOException {
        if (!utils.getProvider().exists(toCreate)) {
            if (!utils.getProvider().mkdirs(toCreate)) {
                throw new IOException("Failed to create directory: " + toCreate);
            }
            created.add(toCreate);
        }
    }

    /**
     * Returns a list of all files and directories created during this extraction
     * @return list of file created during this extraction
     * */
    public List<Path> created() {
        return new ArrayList<>(created);
    }
}
