/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.external.connection;

import static com.ericsson.adp.mgmt.backupandrestore.external.ImportFormat.LEGACY;
import static com.ericsson.adp.mgmt.backupandrestore.external.ImportFormat.TARBALL;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_DATA_FOLDER_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_FILE_FOLDER_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_MONITOR_TIMEOUT_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.PROGRESS_MONITOR_CURRENT_PERCENTAGE;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.archive.ArchiveUtils;
import com.ericsson.adp.mgmt.backupandrestore.archive.StreamingArchiveService;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.exception.ExportException;
import com.ericsson.adp.mgmt.backupandrestore.exception.FileDirectoryException;
import com.ericsson.adp.mgmt.backupandrestore.exception.ImportException;
import com.ericsson.adp.mgmt.backupandrestore.exception.ImportExportException;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientImportProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.ImportFormat;
import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumHash64;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

/**
 * SftpConnection helps to create or imports the backups to or from the Sftp
 * Server
 */
@SuppressWarnings({"PMD.CyclomaticComplexity"})
public class SftpConnection implements ExternalConnection, PropertyChangeListener {

    public static final String CHANNEL_TYPE_SFTP = "sftp";
    public static final String CONNECTION_ERROR_MESSAGE = "Unable to connect to sftp service";

    private static final Logger log = LogManager.getLogger(SftpConnection.class);
    private static final float RATIO_100_PERCENT = 100f;
    private static final int FILE_PERMISSION = Integer.parseInt("640", 8);
    private static final int DIRECTORY_PERMISSION = Integer.parseInt("750", 8);

    private final StreamingArchiveService archiveService;
    private final ArchiveUtils utils;
    private final SftpChannelManager manager;
    private int timeoutBytesReceivedSeconds;

    /**
     * Define a new sftp connection using the external properties#
     * @param manager - SFTP connection manager
     * @param utils - the ArchiveUtils this connection (and the archive service it uses) relies on for it's FS operations
     */
    public SftpConnection(
            final SftpChannelManager manager,
            final ArchiveUtils utils
    ) {
        this.archiveService = new StreamingArchiveService(utils);
        this.utils = utils;
        this.manager = manager;
    }

    /**
     * Request the connection to the server
     *
     * @throws JSchException exception is any parameter is invalid
     */
    public void connect() throws JSchException {
        manager.connect();
    }

    /**
     * This method exports backup to the Sftp Server.
     *
     * @param backupFile      the backupFile Path
     * @param backupData      backupData Path
     * @param remotePath      remotePath
     * @param backupName      backupName
     * @param backupManagerId backupManagerId
     */
    @Override
    public void exportBackup(final Path backupFile, final Path backupData, final String remotePath,
                             final String backupName, final String backupManagerId, final Backup backup,
                             final PropertyChangeListener listener) {
        final String remoteBackupPath = remotePath + File.separator + backupManagerId;
        final ChecksumHash64 hash64 = new ChecksumHash64();
        try {
            final String compressedBackupFilename = ArchiveUtils.getTarballName(backup);
            final String compressedChecksumName = getChecksumFileName(compressedBackupFilename);
            assertBackupNotOnRemote(remoteBackupPath, compressedBackupFilename, backupName);
            log.debug("successfully validated backup does not already exist in Sftp Server");

            manager.connect();
            createDirectories(remoteBackupPath);
            log.debug("successfully created backup directories in Sftp Server");
            changeDirectory(remoteBackupPath);
            log.debug("change directory to the destinationPath in Sftp Server {}", remoteBackupPath);
            final long pathFileSize = getPathFileSize(backupData);
            log.debug("Path size {}", pathFileSize);
            final AtomicBoolean finished = new AtomicBoolean();
            final ProgressMonitor progress = new ProgressMonitor(pathFileSize, finished::get);
            progress.addListener(this);
            progress.addListener(listener);
            try (TarArchiveOutputStream outputStream = archiveService.openTarGzipOutput(
                    manager.channel().put(compressedBackupFilename, progress,
                    ChannelSftp.OVERWRITE), hash64)) {
                log.debug("tar.gz OutputStream opened for {}", compressedBackupFilename);
                manager.channel().chmod(FILE_PERMISSION, compressedBackupFilename);
                manager.channel().setExtOutputStream(outputStream);
                archiveService.addDataOutputStream(outputStream, backupFile,
                        backupData, backupManagerId, backupName);
                outputStream.flush();
                finished.set(true);
                outputStream.finish();
            }
            uploadChecksum(hash64.getStringValue(), compressedChecksumName, remoteBackupPath);
            manager.channel().chmod(FILE_PERMISSION, compressedChecksumName);
        } catch (final InterruptedException exception) {
            log.warn("Exporting backup interrupted {}", backupName, exception);
            Thread.currentThread().interrupt();
        } catch (final Exception e) {
            throw new ExportException("Failed while trying to export backup in SFTP Server", e);
        }
    }

    /**
     * Returns the path global file size in bytes
     *
     * @param path path to walk for file size
     * @return a long value indicating the path size in bytes or -1 on error
     */
    private long getPathFileSize(final Path path) {
        try (Stream<Path> walk = utils.getProvider().walk(path, Integer.MAX_VALUE)) {
            return walk.filter(utils.getProvider()::isFile)
                    .mapToLong(p -> {
                        try {
                            return utils.getProvider().length(p);
                        } catch (IOException e) {
                            log.warn("Caught exception when getting file length {}, {}, treating as 0 length", p, e);
                            return 0L;
                        }
                    }).sum();
        } catch (final IOException e) {
            log.warn("Error getting the path file size for {} ", path.getFileName());
            throw new FileDirectoryException(e);
        }
    }

    /**
     * Gets the backup file from the Sftp Server and converts into String
     *
     * @param remotePath remotePath
     *
     * @return String content of backup file
     */
    @Override
    public String getBackupFileContent(final String remotePath, final ImportFormat importFormat) {

        final String sourcePath = buildPath(remotePath, BACKUP_FILE_FOLDER_NAME);
        try {
            if (TARBALL == importFormat) {
                return getFileContent(getBackupFileFromTempFolder(remotePath));
            } else {
                changeDirectory(sourcePath);
                return getFileContent(getBackupFileFromExportedFolder(sourcePath));
            }
        } catch (final Exception e) {
            throw new ImportException("Failed while trying to import backup from SFTP Server", e);
        }
    }

    @Override
    public String downloadBackupFile(final ExternalClientImportProperties externalClientProperties, final PropertyChangeListener listener) {
        final ChecksumHash64 hash64 = new ChecksumHash64();
        final Path localBackupData = externalClientProperties.getFolderToStoreBackupData();
        final Path localBackupFile = externalClientProperties.getFolderToStoreBackupFile();
        final String remoteBackupPath = externalClientProperties.getUri().getPath();
        log.info("The remote backup path is {}", remoteBackupPath);
        final String compressedChecksumName = getChecksumFileName(remoteBackupPath);
        String localBackupPath;
        if (!isFileExistsInSFTP(remoteBackupPath) || !isFileExistsInSFTP(compressedChecksumName)) {
            throw new ImportException("Failed to find backup or checksum file on SFTP Server");
        }
        final List<File> listFilesDownloaded = downloadRemotePath(remoteBackupPath, localBackupData, localBackupFile, hash64, listener);
        final String calculatedCheckSum;
        final Optional<File> backupFileName = getBackupFile(listFilesDownloaded, localBackupFile.toString());
        calculatedCheckSum = hash64.getStringValue();
        // If the backup persisted doesn't exist
        if (backupFileName.isEmpty()) {
            emptyBackupData(listFilesDownloaded, localBackupData);
            throw new ImportException("Backup json file is missing");
        }
        // If the checksum is invalid
        if (!checksumMatches(compressedChecksumName, calculatedCheckSum)) {
            log.error("The checksum did not match.");
            emptyBackupData(listFilesDownloaded, localBackupData);
            throw new ImportException("Invalid checksum retrieved from import");
        }
        localBackupPath = backupFileName.get().getAbsolutePath();
        return localBackupPath;
    }


    /**
     * Identify the backup file name from the downloaded files
     * @param filesDownloaded list of files downloaded
     * @param localBackupFile prefix used to look for the backup file
     * @return optional file downloaded
     */
    private Optional<File> getBackupFile(final List<File> filesDownloaded, final String localBackupFile) {
        return filesDownloaded
                .stream()
                .filter(file -> file.getAbsoluteFile().toString().startsWith(localBackupFile))
                .findAny();
    }


    private void emptyBackupData(final List<File> filesDownloaded, final Path localBackupData) {
        final String backupId = getBackupId(filesDownloaded, localBackupData);
        log.debug("Cleaning data backup id: {}", backupId);
        // Empty the backup data directory
        utils.deleteFile(localBackupData.resolve(backupId));
        // Delete any other file created
        filesDownloaded.stream().map(f -> Path.of(f.getAbsolutePath())).forEach(utils::deleteFile);
    }

    /**
     * Retrieves the backup ID from the local backup data path
     * @param filesDownloaded list of files downloaded
     * @param localBackupData path local backup path
     * @return String backup Id
     */
    private String getBackupId(final List<File> filesDownloaded, final Path localBackupData) {
        final Path file = getBackupDataReference(filesDownloaded, localBackupData).toPath();
        return localBackupData.relativize(file.toAbsolutePath()).getName(0).toString();
    }

    /**
     * Looks for a data file and returns the first file to retrieve their id
     * @param filesDownloaded Files list
     * @param localBackupData prefix to be filtered
     * @return the first file if exist
     */
    private File getBackupDataReference(final List<File> filesDownloaded, final Path localBackupData) {
        return filesDownloaded
                .stream()
                .filter(file -> file.getAbsoluteFile().toString().startsWith(localBackupData.toString()))
                .findAny()
                .orElseThrow(() -> new ImportException("Data downloaded in an invalid local path"));
    }

    /**
     * Compare the checksum from remote file with the calculated checksum
     * @param compressedChecksumName the checksum file name from remote
     * @param calculatedCheckSum the calculated checksum
     * @return true if matches
     */
    protected boolean checksumMatches(final String compressedChecksumName, final String calculatedCheckSum) {
        String expectedChecksum;
        try {
            expectedChecksum = getRemoteFileAsString(compressedChecksumName);
        } catch (Exception e) {
            log.error("Failed to get the expected checksum from sftp server", e);
            return false;
        }

        if (!calculatedCheckSum.contentEquals(expectedChecksum)) {
            log.warn("The checksum doesn't match. " +
                    "The imported checksum name is: {}, and its value is: {}", compressedChecksumName, expectedChecksum);
            return checksumMatchesFormatted(expectedChecksum, calculatedCheckSum);
        } else {
            return calculatedCheckSum.contentEquals(expectedChecksum);
        }
    }

    /**
     * Compare the formatted checksum from remote file with the formatted calculated checksum
     * @param expectedChecksum the checksum file name from remote
     * @param calculatedCheckSum the calculated checksum
     * @return true if matches
     */
    protected boolean checksumMatchesFormatted(final String expectedChecksum, final String calculatedCheckSum) {
        try {
            log.debug("Formatting imported checksum and calculated checksum");
            final String hashFormat = "%016x";
            long decimalValue = Long.parseUnsignedLong(expectedChecksum, 16);
            final String fExpectedChecksum = String.format(hashFormat, decimalValue);
            decimalValue = Long.parseUnsignedLong(calculatedCheckSum, 16);
            final String fCalculatedChecksum = String.format(hashFormat, decimalValue);
            log.debug("Checking if the formatted imported checksum and calculated match");
            log.debug("Formatted imported checksum is: {}", fExpectedChecksum);
            return fExpectedChecksum.contentEquals(fCalculatedChecksum);
        } catch (Exception e) {
            log.error("There was an error formatting the checksum", e);
            return false;
        }
    }

    /**
     * Download the remote tar.gz remote path from sftp
     * @param remoteBackupPath remote file tar.gz to be imported
     * @param localBackupData local path to contain the backup data
     * @param localBackupFile local path to contain the the backup persisted file
     * @param hash64 hash64 element to be calculated
     * @param listener Property listener used as observer for properties changes on Monitor
     * @return List<File> files being downloaded
     */
    protected List<File> downloadRemotePath(final String remoteBackupPath,
                                          final Path localBackupData,
                                          final Path localBackupFile,
                                          final ChecksumHash64 hash64,
                                          final PropertyChangeListener listener) {

        final long fileSize = getFileSizeInSFTP(remoteBackupPath);
        final AtomicBoolean finished = new AtomicBoolean();
        final ProgressMonitor progress = new ProgressMonitor(fileSize, RATIO_100_PERCENT, finished::get);
        progress.addListener(listener);
        // Perform operations with custom InputStream
        try (TarArchiveInputStream inputStream = archiveService.openTarGzipInput (
                manager.channel().get(remoteBackupPath, progress),
                hash64)) {
            // NOTE: map done here to avoid refactoring SftpConnection before finishing ArchiveService
            return archiveService.unpackTarStream(inputStream, localBackupData, localBackupFile, finished::set)
                    .stream()
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException | SftpException e) {
            throw new ImportException("Error transferring remote backup", e);
        }
    }

    private String getRemoteFileAsString(final String remotePath) throws IOException, SftpException {
        final StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = manager.channel().get(remotePath);
                InputStreamReader isr = new InputStreamReader(inputStream);
                BufferedReader bufferReader = new BufferedReader(isr)) {
            String line;
            while ((line = bufferReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public void importBackupData(final String remotePath, final Path backupDataFolder,
                                 final ImportFormat importFormat) {
        try {
            if (LEGACY == importFormat) {
                final String sourcePath = buildPath(remotePath, BACKUP_DATA_FOLDER_NAME);
                log.info("the remote path {} of backup data to be imported", sourcePath);
                changeDirectory(sourcePath);
                recursiveDownloadFromSftpServer(sourcePath, backupDataFolder);
                log.info("successfully imported backup data folder in destination path {}", backupDataFolder);
            }
        } catch (final Exception e) {
            throw new ImportException("Failed while trying to import backup from SFTP Server", e);
        }
    }

    private void recursiveDownloadFromSftpServer(final String sourcePath, final Path destination)
            throws SftpException {

        for (final LsEntry item : listFiles(sourcePath)) {

            final String sourceFinalPath = buildPath(sourcePath, item.getFilename());
            final Path destinationFinal = destination.resolve(item.getFilename());

            if (!item.getAttrs().isDir()) {
                manager.channel().get(sourceFinalPath, destinationFinal.toString());
                log.debug("downloaded file {} from Sftp Server to destination path {}", sourceFinalPath,
                        destinationFinal);
            } else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) {
                utils.getProvider().mkdirs(destinationFinal);
                log.debug("created the directory in destination path {}", destinationFinal);
                recursiveDownloadFromSftpServer(sourceFinalPath, destinationFinal);
            }
        }
    }

    /**
     * disconnects with Sftp Server
     */
    @Override
    public void close() {
        if (manager != null) {
            manager.close();
        }
    }

    private void changeDirectory(final String destinationPath) throws SftpException {
        manager.channel().cd(destinationPath);
        log.debug("changed the directory to {}", destinationPath);
    }

    private void assertBackupNotOnRemote(final String remotePath, final String compressedBackupName,
                                         final String backupPrefix) {
        final String BACKUP_DIRECTORY_ALREADY_EXISTS_ERROR_MESSAGE = "the backup %s already exists for remote path %s";
        final String backupTarballPath = remotePath + File.separator + compressedBackupName;
        final String backupLegacyPath = remotePath + File.separator + backupPrefix;

        log.info("Checking if backup exists {}", backupTarballPath);
        if (isFileExistsInSFTP(backupTarballPath)) {
            throw new ImportExportException(
                    String.format(BACKUP_DIRECTORY_ALREADY_EXISTS_ERROR_MESSAGE, compressedBackupName, remotePath));
        } else if (isDirectoryExists(backupLegacyPath)) {
            throw new ImportExportException(
                    String.format("A directory with this tarball prefix %s already exists", backupLegacyPath));
        }

    }

    /**
     * Retrieves the matching tarball names in a given source path
     *
     * @param sourcePath the absolute path to the backup file in the SFTP server
     * @return list of tarball names
     */
    public List<String> getMatchingBackUpTarballNames(final String sourcePath) {
        final File tmpFile = new File(sourcePath);
        List<String> result = new ArrayList<>();
        try {
            final List<ChannelSftp.LsEntry> files = listFiles(tmpFile.getParent());
            files.forEach(file -> log.info("Found file {}", file.getFilename()));

            result = files.stream()
                        .filter(file -> !file.getAttrs().isDir()
                                && file.getFilename().startsWith(tmpFile.getName() + "-")
                                && file.getFilename().endsWith(".tar.gz"))
                        .map(LsEntry::getFilename).collect(Collectors.toList());
        } catch (final SftpException e) {
            log.info("No files found");
        }

        return result;
    }

    private void uploadChecksum(final String checksum, final String sourceFileName, final String path) throws SftpException, IOException {
        changeDirectory(path);
        manager.channel().setExtOutputStream(null, false);
        final byte[] bytes = checksum.getBytes();
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        manager.channel().put(byteArrayInputStream, sourceFileName, manager.getTimeout());
        byteArrayInputStream.close();
    }

    /**
     * check if there is single backup in sftp server
     *
     * @param path the path
     * @return isDirectoryExists
     */
    public boolean isDirectoryExists(final String path) {
        SftpATTRS attributes = null;
        try {
            attributes = manager.channel().stat(path);
        } catch (final Exception e) {
            log.warn("path {} is not found in Sftp Server", path);
        }
        return null != attributes && attributes.isDir();
    }

    private boolean isFileExistsInSFTP(final String path) {
        SftpATTRS attributes = null;
        boolean isFileExists;
        try {
            attributes = manager.channel().lstat(path);
            isFileExists = true;
        } catch (final SftpException e) {
            isFileExists = (e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE);
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                log.info("The file does not exist in remotePath {}", path);
            } else {
                final String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                throw new ImportExportException(
                        String.format("Exception validating remote path: %s - %s", path, message));
            }
        }
        return null != attributes && isFileExists;
    }

    private long getFileSizeInSFTP(final String path) {
        try {
            return manager.channel().lstat(path).getSize();
        } catch (final SftpException e) {
            final String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw new ImportExportException(
                    String.format("Exception validating remote path: %s - %s", path, message));
        }
    }

    private String getFileContent(final InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            final byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8);
        }
    }

    private void createDirectories(final String remotePath) throws SftpException {

        final StringBuilder pathBuilder = new StringBuilder();
        final String[] remotePathElements = remotePath.split("/");

        for (final String directory : remotePathElements) {

            if (!directory.isEmpty()) {
                pathBuilder.append(File.separator).append(directory);

                if (!isDirectoryExists(pathBuilder.toString())) {
                    manager.channel().mkdir(pathBuilder.toString());
                    manager.channel().chmod(DIRECTORY_PERMISSION, pathBuilder.toString());
                    log.info("created the directory {} in Sftp Server", pathBuilder);
                } else {
                    log.info("the directory {} already exists in Sftp Server", pathBuilder);
                }

            }
        }

    }

    private InputStream getBackupFileFromExportedFolder(final String folder) throws SftpException {
        log.debug("Searching for backup file at {}", folder);

        final List<ChannelSftp.LsEntry> files = listFiles(folder);
        files.forEach(file -> log.debug("Found file {}", file));

        final LsEntry backupFile = files.stream()
                .filter(file -> !file.getAttrs().isDir() && file.getFilename().endsWith(".json")).findFirst()
                .orElseThrow(() -> new ImportExportException(
                        "backup file with .json extension doesn't exist in the path <" + folder + ">"));

        return manager.channel().get(buildPath(folder, backupFile.getFilename()));
    }

    private InputStream getBackupFileFromTempFolder(final String folder) throws IOException {
        final Path backup;
        log.info("Searching for backup file at {}", folder);
        try (Stream<Path> stream = utils.getProvider().walk(Path.of(folder), Integer.MAX_VALUE)) {
            backup = stream.peek(file -> log.info("Found file {}", file))
                    .filter(file -> (utils.getProvider().isFile(file) && file.toFile().getName().endsWith(".json")))
                    .findFirst().orElseThrow(() -> new ImportExportException(
                            "backup file with .json extension doesn't exist in the path <" + folder + ">"));
        }
        return utils.getProvider().newInputStream(backup.toAbsolutePath().normalize());
    }

    private String getChecksumFileName (final String baseName) {
        return String.format("%s.xxh64", baseName);
    }

    @SuppressWarnings("unchecked")
    private List<LsEntry> listFiles(final String sourcePath) throws SftpException {
        return new ArrayList<>(manager.channel().ls(sourcePath));
    }

    private String buildPath(final String sourcePath, final String destinationPath) {
        return sourcePath + File.separator + destinationPath;
    }

    /**
     * Utility class used to monitor the sftp progress transmission
     *
     */
    class ProgressMonitor implements SftpProgressMonitor {
        private static final float ESTIMATED_COMPRESS_RATE = 90.0f;

        private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);
        private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);

        private final long estimatedBytes;
        private long uploadedBytes;
        private long lastUploadedBytes;
        private long secondsAfterLastUploaded;
        private double lastPercent;

        private final Supplier<Boolean> didFinish;

        private long timeOfPreviousCMMPush;
        private double percentAtPreviousCMMPush;

        private final Logger log = LogManager.getLogger(ProgressMonitor.class);

        /**
         * Sftp Monitor constructor
         * @param totalBytes estimated bytes to be sent
         * @param didFinish - a provider which should return true when the transfer being monitored finished successfully
         */
        ProgressMonitor(final long totalBytes, final Supplier<Boolean> didFinish) {
            this(totalBytes, ESTIMATED_COMPRESS_RATE, didFinish);
        }

        /**
         * Sftp Monitor constructor
         * @param totalBytes to monitor
         * @param uncompress_rate expect the complete file, other value is a rate to expect
         * @param didFinish - a provider which should return true when the transfer being monitored finished successfully
         */
        ProgressMonitor(final long totalBytes, final float uncompress_rate, final Supplier<Boolean> didFinish) {
            this.didFinish = didFinish;
            estimatedBytes = (long) (totalBytes * (uncompress_rate / RATIO_100_PERCENT));
        }

        /**
         * Initial message to define what will monitored
         * @param operation Operation to be used
         * @param source source file
         * @param destination destination file
         * @param maximumSize maximum size
         */
        @Override
        public void init(final int operation, final String source,
                         final String destination, final long maximumSize) {
            log.debug("Init progress monitoring :{} src:{} dest:{} totalSize: {}", operation, source, destination, maximumSize);
            if (timeoutBytesReceivedSeconds > 0) {
                scheduledExecutor.scheduleWithFixedDelay(new TimeoutTask(), 1, 1, TimeUnit.SECONDS);
            }
        }

        /**
         * Add a Listener to react to properties changes
         * @param listener Observer to received the fields changes
         */
        public void addListener(final PropertyChangeListener listener) {
            propertySupport.addPropertyChangeListener(listener);
        }

        /**
         * Remove a Listener to react to properties changes
         * @param listener Observer to be deleted
         */
        public void removeListener(final PropertyChangeListener listener) {
            propertySupport.removePropertyChangeListener(listener);
        }

        /**
         * Number bytes being sent to sftp
         * @param bytes sent to sftp
         */
        @Override
        public boolean count(final long bytes) {
            uploadedBytes += bytes;
            final double percent = Math.round((RATIO_100_PERCENT * uploadedBytes ) / estimatedBytes);
            if (percent < 100 && lastPercent != percent) {
                final long currentTime = System.currentTimeMillis();
                if (percentAtPreviousCMMPush != percent && currentTime - 2000L >= timeOfPreviousCMMPush) {
                    try {
                        propertySupport.firePropertyChange(PROGRESS_MONITOR_CURRENT_PERCENTAGE, percent, lastPercent);
                        updateProgressTime(currentTime, percent);
                    } catch (Exception e) {
                        log.warn("Failed to update the progress report for progress percentage in CMM: ", e);
                    }
                }
                lastPercent = percent;
                log.info("Transferring: {}", getLastPercent());
            }
            return (true);
        }

        /**
         * Update progress time
         * @param currentTime - current time
         * @param percent - percent complete0
         */
        public void updateProgressTime(final long currentTime, final double percent) {
            timeOfPreviousCMMPush = currentTime;
            percentAtPreviousCMMPush = percent;
        }

        public String getLastPercent() {
            return String.format("%.0f%%", lastPercent);
        }

        /**
         * End of transmission
         */
        @Override
        public void end() {
            propertySupport.firePropertyChange(PROGRESS_MONITOR_CURRENT_PERCENTAGE, 100, lastPercent);
            scheduledExecutor.shutdown();
            if (didFinish.get()) {
                log.info("Transferring: 100% - Completed");
            } else {
                log.warn("Transfer not marked complete, may not have finished successfully");
            }
        }

        private class TimeoutTask implements Runnable {
            private final Logger log = LogManager.getLogger(TimeoutTask.class);

            public void run() {
                secondsAfterLastUploaded = (lastUploadedBytes == uploadedBytes &&
                        secondsAfterLastUploaded < timeoutBytesReceivedSeconds ) ? secondsAfterLastUploaded + 1 : 0;
                if ( lastUploadedBytes == uploadedBytes && secondsAfterLastUploaded >= timeoutBytesReceivedSeconds) {
                    log.warn("Bytes received has not been updated for {} seconds. Timeout!", timeoutBytesReceivedSeconds);
                    // BACKUP_MONITOR_TIMEOUT_NAME is an internal property name used by the observer to notify
                    // a timeout was identified when it was monitoring the bytes traffic with a sftp server
                    ProgressMonitor.this.propertySupport.firePropertyChange(BACKUP_MONITOR_TIMEOUT_NAME, false, true);
                    lastUploadedBytes = 0;
                    secondsAfterLastUploaded = 0;
                    end();
                } else {
                    lastUploadedBytes = uploadedBytes;
                }
            }
        }
    }

    @Override
    public String downloadBackupFile(final URI remoteUri) {
        throw new UnsupportedOperationException("Not supported in SftpConnection.");
    }

    @Override
    public void propertyChange(final PropertyChangeEvent propertyUpdated) {
        if (propertyUpdated.getPropertyName().equals(BACKUP_MONITOR_TIMEOUT_NAME)) {
            //a timeout means the communication fails
            if ((boolean) propertyUpdated.getNewValue()) {
                log.error("A timeout occurred in the SFTP communication, the communication channel will be terminated.");
                if (manager.channel() != null) {
                    manager.channel().disconnect();
                }
            }
        }
    }

    /**
     * Define the timeout for each chunk of bytes received
     * @param timeout_bytes_received_seconds seconds to expect for new chunk of bytes
     */
    public void setTimeoutBytesReceivedSeconds(final int timeout_bytes_received_seconds) {
        this.timeoutBytesReceivedSeconds = timeout_bytes_received_seconds;
    }
}
