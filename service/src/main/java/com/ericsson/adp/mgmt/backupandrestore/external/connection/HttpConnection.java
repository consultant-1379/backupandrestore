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
package com.ericsson.adp.mgmt.backupandrestore.external.connection;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_DATA_FOLDER_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_FILE_FOLDER_NAME;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import com.ericsson.adp.mgmt.backupandrestore.archive.ArchiveUtils;
import com.ericsson.adp.mgmt.backupandrestore.archive.StreamingArchiveService;
import com.ericsson.adp.mgmt.backupandrestore.archive.TempFileArchiveService;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.exception.ExportException;
import com.ericsson.adp.mgmt.backupandrestore.exception.ImportException;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientImportProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.ImportFormat;

/**
 * HttpConnection helps to create or imports the backups to or from the Http Server
 */
public class HttpConnection implements ExternalConnection {

    private static final Logger log = LogManager.getLogger(HttpConnection.class);
    private final TempFileArchiveService archiveService;
    private final ArchiveUtils utils;
    private final RestTemplate restTemplate;
    private final PropertyChangeListener propertyReceivedListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            return;
        }
    };
    private Optional<File> uncompressedBackup = Optional.empty();

    /**
     * Constructor
     *
     * @param restTemplate
     *            restTemplate
     * @param archiveUtils
     *            archiveUtils
     */
    public HttpConnection(final RestTemplate restTemplate, final ArchiveUtils archiveUtils) {
        this.restTemplate = restTemplate;
        this.archiveService = new TempFileArchiveService(archiveUtils, new StreamingArchiveService(archiveUtils));
        this.utils = archiveUtils;
    }

    /**
     * This method exports backup to the Http Server.
     *
     * @param backupFile
     *            the backupFile
     * @param backupData
     *            backupData
     * @param remotePath
     *            remotePath
     * @param backupName
     *            backupName
     * @param managerId
     *            managerId
     * @param backup
     *            backup
     */
    public void exportBackup(final Path backupFile, final Path backupData, final String remotePath, final String backupName, final String managerId,
            final Backup backup) {
        exportBackup(backupFile, backupData, remotePath, backupName, managerId,
            backup, propertyReceivedListener);
    }

    /**
     * This method exports backup to the Http Server.
     *
     * @param backupFile
     *            the backupFile
     * @param backupData
     *            backupData
     * @param remotePath
     *            remotePath
     * @param backupName
     *            backupName
     * @param managerId
     *            managerId
     * @param backup
     *            backup
     * @param propertyListener
     *            Listener not implemented on httpConnection
     */
    @Override
    public void exportBackup(final Path backupFile, final Path backupData, final String remotePath, final String backupName, final String managerId,
                             final Backup backup, final PropertyChangeListener propertyListener) {
        Optional<File> compressedBackup = Optional.empty();
        try {
            compressedBackup = archiveService.compressBackup(backupFile, backupData, managerId, backupName, backup);
            if (compressedBackup.isPresent()) {
                log.info("Created temporary compressedBackup {}", compressedBackup.get().getAbsolutePath());
            } else {
                throw new ExportException("Failed while trying to compress export backup to HTTP Server : ");
            }

            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(compressedBackup.get()));
            final HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(body, headers);
            final ResponseEntity<String> response = restTemplate.postForEntity(remotePath, httpEntity, String.class);
            log.info("Response from server while exporting {}", response.getStatusCode());

        } catch (final HttpClientErrorException e) {
            throw new ExportException("Failed while trying to export backup to HTTP Server : " + e.getMessage());
        } catch (final Exception e) {
            throw new ExportException("Failed while trying to export backup to HTTP Server", e);
        } finally {
            compressedBackup.ifPresent(b -> utils.deleteFile(Path.of(b.getAbsolutePath())));
        }
    }

    /**
     * This method downloads backup from Http Server.
     *
     * @param remote
     *            remoteUri
     * @return String localBackupPath
     */
    @Override
    public String downloadBackupFile(final URI remote) {
        String localBackupPath;
        Optional<File> compressedBackup = Optional.empty();

        try {
            compressedBackup = Optional.of(restTemplate.execute(remote, HttpMethod.GET, null, getFileResponseExtractor()));

            uncompressedBackup = Optional.of(archiveService.uncompressBackup(Objects.requireNonNull(compressedBackup.get())));

            final File uncompBackup = uncompressedBackup.get();
            log.info("Created temporary uncompressedBackup {}", uncompBackup.getAbsolutePath());
            localBackupPath = archiveService.getLocalBackupPath(uncompBackup.toPath());
        } catch (final HttpClientErrorException e) {
            uncompressedBackup.ifPresent(b -> utils.deleteFile(Path.of(b.getAbsolutePath())));
            throw new ImportException("Failed while trying to import backup from HTTP Server : " + e.getMessage());
        } catch (final Exception e) {
            uncompressedBackup.ifPresent(b -> utils.deleteFile(Path.of(b.getAbsolutePath())));
            throw new ImportException("Failed while trying to import backup from HTTP Server", e);
        } finally {
            compressedBackup.ifPresent(b -> utils.deleteFile(Path.of(b.getAbsolutePath())));
        }
        return localBackupPath;
    }

    private ResponseExtractor<File> getFileResponseExtractor() {
        return response -> {
            log.info("Response from server while importing {}", response.getStatusCode());

            final String fileName = response.getHeaders().getContentDisposition().getFilename();
            if (fileName == null || fileName.isEmpty()) {
                throw new ImportException("Response does not have proper filename in ContentDisposition Header");
            }
            final Path location = archiveService.getTarballLocation(fileName);
            StreamUtils.copy(response.getBody(), new FileOutputStream(location.toFile()));
            log.info("Created temporary compressedBackup {}", location);
            return location.toFile();
        };
    }

    /**
     * This method returns backupFile content.
     *
     * @param remotePath
     *            remotePath
     * @return String backupContent
     */
    @Override
    public String getBackupFileContent(final String remotePath, final ImportFormat importFormat) {
        final String backupFile = remotePath + File.separator + BACKUP_FILE_FOLDER_NAME;
        log.info("Searching backupFile in folder {}", backupFile);

        try {
            return getFileContent(getBackupFile(Paths.get(backupFile)));
        } catch (final Exception e) {
            uncompressedBackup.ifPresent(b -> utils.deleteFile(Path.of(b.getAbsolutePath()))); // delete since exception during backupFile
            throw new ImportException("Failed while trying to import backup from HTTP Server", e);

        }
    }

    private String getFileContent(final Path backupFile) throws IOException {

        try (InputStream inputStream = new FileInputStream(backupFile.toFile()); ByteArrayOutputStream result = new ByteArrayOutputStream()) {

            StreamUtils.copy(inputStream, result);
            return result.toString("UTF-8");
        }
    }

    private Path getBackupFile(final Path dir) throws IOException {
        final Path backupFile;

        try (Stream<Path> stream = Files.list(dir)) {
            backupFile = stream.peek(file -> log.debug("Found file {}", file))
                    .filter(file -> (!file.toFile().isDirectory() && file.toString().endsWith(".json"))).findFirst()
                    .orElseThrow(() -> new FileNotFoundException("backupfile doesn't exist in the path: " + dir.toString()));
        }

        log.info("Found backupFile {}", backupFile);
        return backupFile;
    }

    /**
     * Imports the backup data from the Http Server to the Orchestrator.
     *
     * @param remotePath
     *            remotePath
     * @param backupData
     *            backupDataFolder
     */
    @Override
    public void importBackupData(final String remotePath, final Path backupData, final ImportFormat importFormat) {
        try {
            final File src = new File(remotePath + File.separator + BACKUP_DATA_FOLDER_NAME);
            final File dest = new File(backupData.toString());
            archiveService.recursiveCopy(src, dest);
        } catch (final Exception e) {
            throw new ImportException("Failed while trying to import backup from HTTP Server", e);
        } finally {
            uncompressedBackup.ifPresent(b -> utils.deleteFile(Path.of(b.getAbsolutePath())));
        }
    }

    /**
     * disconnects with Http Server
     */
    @Override
    public void close() {
        // delete uncompressed file when there
        // is exception during Job handling
        uncompressedBackup.ifPresent(b -> utils.deleteFile(Path.of(b.getAbsolutePath())));
    }

    /**
     * Call the downloadbackupFile
     * @param externalClientProperties External client properties
     * @param listener parameter ignored, not used
     * @return String localBackupPath
     */
    @Override
    public String downloadBackupFile(final ExternalClientImportProperties externalClientProperties, final PropertyChangeListener listener) {
        return downloadBackupFile(externalClientProperties.getUri());
    }

    /**
     * Call the downloadbackupFile
     * @param externalClientProperties External client properties
     * @return String localBackupPath
     */
    public String downloadBackupFile(final ExternalClientImportProperties externalClientProperties) {
        return downloadBackupFile (externalClientProperties, null);
    }

}
