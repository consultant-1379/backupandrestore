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

import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_DATA_FOLDER_NAME;

/**
 * Handler for packing and unpacking backups relying on placing files into the temp dir
 *
 * This class is exclusively used by the HTTP export logic, and therefore can be safely removed
 * along with that code when HTTP export is fully disabled
 * */
@Deprecated(since = "25/05/21")
public class TempFileArchiveService {
    private static final Logger log = LogManager.getLogger(TempFileArchiveService.class);

    private final StreamingArchiveService streamService;
    private final ArchiveUtils utils;

    /**
     * Construct a temp file based archive service
     * @param utils - the utils this archive service relies on
     * @param streamService - a streaming archive service this service relies on for some stream processing
     * */
    public TempFileArchiveService(final ArchiveUtils utils, final StreamingArchiveService streamService) {
        this.utils = utils;
        this.streamService = streamService;
    }

    /**
     * Compress a backup and return a file handle to the compressed data. Generates the file name used based on
     * current time and backup name
     *
     * ENTRY_POINT: HTTP EXPORT - COMPRESSION PRIOR TO FILE UPLOAD
     *
     * @param backupFile
     *            backupFile
     * @param backupData
     *            backupData
     * @param backupManagerId
     *            backupManagerId
     * @param backupName
     *            backupName
     * @param backup
     *            backup, used to generate compressed file name
     * @return File tarFile
     * @throws IOException
     *             IOException
     * @deprecated as used only for HTTP export, which is to be removed
     */
    public Optional<File> compressBackup(final Path backupFile, final Path backupData, final String backupManagerId, final String backupName,
                                         final Backup backup)
            throws IOException {
        return compressBackup(backupFile, backupData, backupManagerId, backupName, ArchiveUtils.getTarballName(backup));
    }

    private Optional<File> compressBackup(final Path backupFile, final Path backupData, final String backupManagerId, final String backupName,
                                          final String compressedFileName) {
        final Path tar = getTarballLocation(compressedFileName);

        try (TarArchiveOutputStream tos = new TarArchiveOutputStream(new GZIPOutputStream(utils.getOutputStream(tar)))) {
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            streamService.addDataOutputStream(tos, backupFile, backupData, backupManagerId, backupName);
        } catch (final InterruptedException exception) {
            log.warn("Compress backup interrupted {}", backupName, exception);
            Thread.currentThread().interrupt();
        } catch (final Exception e) {
            log.error("Failed to compress backup: ", e);
            return Optional.empty();
        }
        return Optional.of(tar.toFile());
    }


    /**
     * This method uncompress the backup file
     * ENTRY_POINT: HTTP IMPORT - DECOMPRESSION OF DOWNLOADED TARBALL
     *
     * @param tarFile
     *            tarFile
     * @return File untarFile
     * @throws IOException
     *             IOException
     * @deprecated as only used in HTTP import path, which is to be removed
     */
    public File uncompressBackup(final File tarFile) throws IOException {
        File untarFile;
        try (TarArchiveInputStream tis = streamService.openTarGzipInput(new FileInputStream(tarFile), null)) {
            // Create temp directory with tarFile name to store untarred files
            final String untarFilename = tarFile.getName().split(ArchiveUtils.TAR_GZ)[0];
            final String untarPath = tarFile.getParent() + File.separator + untarFilename;
            log.info("untarPath {}", untarPath);
            untarFile = new File(untarPath);
            untarFile.mkdirs();

            TarArchiveEntry entry = tis.getNextTarEntry();

            while (entry != null) {
                final File file = new File(untarFile.getAbsolutePath() + File.separator + entry.getName());
                log.info("Untarring to {}", file.getAbsolutePath());

                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    uncompressFile(file, tis);
                }
                entry = tis.getNextTarEntry();
            }
        }
        return untarFile;
    }

    private void uncompressFile(final File file, final TarArchiveInputStream tis) throws IOException {
        final byte[] buffer = new byte[1024];
        int len;

        try (FileOutputStream fos = new FileOutputStream(file)) {
            while ((len = tis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }

    /**
     * Get the location to store a packed backup tarball to
     * @param fileName - the name of the file to store the tarball in
     * @return the location to store the tarball
     * */
    public Path getTarballLocation(final String fileName) {
        final Path tmpdir = Path.of(System.getProperty("java.io.tmpdir"));
        final Path tarFile = tmpdir.resolve(fileName).toAbsolutePath().normalize();
        log.info("Created TarGzFile {}", tarFile);
        return tarFile;
    }

    /**
     * Clone a directory or file from src to dest
     * @param src - the source object to clone
     * @param dest - the location to clone the src to
     * @throws IOException if cloning fails
     * */
    public void recursiveCopy(final File src, final File dest) throws IOException {
        for (final File file : Objects.requireNonNull(src.listFiles())) {
            final File newFile = new File(dest.getAbsolutePath() + File.separator + file.getName());
            if (file.isDirectory()) {
                log.info("Creating directory {}", newFile.getAbsolutePath());
                newFile.mkdirs();
                recursiveCopy(new File(src.getAbsolutePath() + File.separator + file.getName()), newFile);
            } else {
                log.info("Creating file {}", newFile.getAbsolutePath());
                FileCopyUtils.copy(file, newFile);
            }
        }
    }

    /**
     * This will check if path of the backupdata folder
     * exists locally
     *
     * @param dir
     *            dir
     * @throws IOException
     *             IOException
     * @return String backupPath
     * */
    public String getLocalBackupPath(final Path dir) throws IOException {
        final String backupPath;
        try (Stream<Path> stream = Files.walk(dir)) {
            backupPath = stream.peek(file -> log.debug("Found file {}", file))
                    .filter(file -> (file.toFile().isDirectory() && file.toString().endsWith(BACKUP_DATA_FOLDER_NAME))).findFirst()
                    .map(file -> file.getParent().toString())
                    .orElseThrow(() -> new FileNotFoundException("backup path not found in dir: " + dir.toString()));
        }

        log.info("Found backup path {}", backupPath);
        return backupPath;
    }

}
