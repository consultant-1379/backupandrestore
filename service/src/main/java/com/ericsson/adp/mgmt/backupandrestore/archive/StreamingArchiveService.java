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


import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_DATA_FOLDER_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.BACKUP_FILE_FOLDER_NAME;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.Deflater;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.env.Environment;

import com.ericsson.adp.mgmt.backupandrestore.SpringContext;
import com.ericsson.adp.mgmt.backupandrestore.exception.ExportException;
import com.ericsson.adp.mgmt.backupandrestore.exception.FileDirectoryException;
import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumHash64;
import com.ericsson.adp.mgmt.backupandrestore.util.CustomInputStream;
import com.ericsson.adp.mgmt.backupandrestore.util.CustomTarArchiveInputStream;


/**
 * Handler for packing and unpacking backups to/from streams
 *
 * */
public class StreamingArchiveService {
    private static final Logger log = LogManager.getLogger(StreamingArchiveService.class);

    private static final int BUFFER_BLOCK_SIZE = 128 * 1024;
    private static final int TAR_COMPRESS_SIZE_BUFFER = 32 * 512;

    private final ArchiveUtils utils;

    /**
     * Construct a streaming archive service
     * @param utils - the ArchiveUtils this will rely on
     * */
    public StreamingArchiveService(final ArchiveUtils utils) {
        this.utils = utils;
    }


    /**
     * Add backup metadata file and data files to tar output stream
     *
     * @param tos Tar ouput stream
     * @param backupFile backup metadata .json file location
     * @param backupData backup data directory (backups/< BRM >/< BACKUP >)
     * @param backupManagerId Manager Id
     * @param backupName Backup Name
     * @throws IOException Exception if backup doesn't exist
     * @throws InterruptedException Exception if the process is interrupted
     */
    public void addDataOutputStream(final TarArchiveOutputStream tos, final Path backupFile, final Path backupData,
                                    final String backupManagerId, final String backupName) throws IOException, InterruptedException {
        final ArchiveUtils.Prefix prefix = ArchiveUtils.prefix(backupManagerId).add(backupName);
        // The old import ArchiveService expects some "marker" pseudo-entry directories as a prelude to the actual data
        // We build the same prelude here to allow the old ArchiveService to import backups exported by the new ArchiveService
        tos.putArchiveEntry(new TarArchiveEntry(prefix.fork(BACKUP_FILE_FOLDER_NAME).build()));
        addFileToCompress(backupFile, backupFile.getParent(), tos, prefix.fork(BACKUP_FILE_FOLDER_NAME).build());
        tos.putArchiveEntry(new TarArchiveEntry(prefix.fork(BACKUP_DATA_FOLDER_NAME).build()));
        for (final Path filePath : utils.walk(backupData)) {
            addFileToCompress(filePath, backupData.getParent(), tos, prefix.fork(BACKUP_DATA_FOLDER_NAME).build());
        }
    }

    private void addFileToCompress(final Path source, final Path sourcePrefix, final TarArchiveOutputStream tos,
                                          final String destinationPrefix) throws IOException {

        final TarArchiveEntry entry = utils.newEntry(sourcePrefix, source, destinationPrefix);
        if (entry.isDirectory()) {
            tos.putArchiveEntry(entry);
        } else {
            utils.initEntry(entry, source);
            final byte[] buffer = new byte[ArchiveUtils.BLOCK_SIZE];
            log.info("Adding file {} to compress", source);
            try (InputStream fis = utils.getInputStream(source)) {
                tos.putArchiveEntry(entry);
                int len = fis.read(buffer);
                while (len != -1) {
                    tos.write(buffer, 0, len);
                    len = fis.read(buffer);
                }
            } catch (Exception e) {
                log.error("Error writing the entry file {} in tar process ", source.getFileName(), e);
            }
        }
        tos.closeArchiveEntry();
    }

    /**
     * Given an TarArchiveInput Stream, extracts that on required Path
     *
     * ENTRY_POINT: SFTP IMPORT - DECOMPRESSION OF STREAM AND WRITING TO DISK
     *
     * @param inputStream Tar input Stream to be extracted
     * @param dirBackupData local backup data path to extract the stream
     * @param dirBackupFile local backup file path to extract the stream
     * @param markSuccess - Consumer that will be passed "true" if this unpacking succeeds. Used as a hook for the progress monitor
     * @return a List of files being extracted
     */
    public List<Path> unpackTarStream(final TarArchiveInputStream inputStream,
                                      final Path dirBackupData,
                                      final Path dirBackupFile,
                                      final Consumer<Boolean> markSuccess) {
        final UnpackSession session = new UnpackSession(dirBackupFile, dirBackupData, inputStream, utils);
        try {
            TarArchiveEntry tarEntry = inputStream.getNextTarEntry();
            while ( tarEntry != null) {
                session.next(tarEntry);
                tarEntry = inputStream.getNextTarEntry();
            }
            ((CustomTarArchiveInputStream) inputStream).readRemainingBytes();
            markSuccess.accept(true);
        } catch (IOException e) {
            throw new FileDirectoryException("Error processing TarArchiveInputStream", e);
        }
        return session.created();
    }

    /**
     * Wrap the Input stream into a tar.gzip buffered stream
     * @param inputStream InputStream used as piped stream
     * @param hash64 inject the hash64 used for checksum
     * @return a Tar input stream
     * @throws IOException on IO file error
     */
    @SuppressWarnings("PMD.CloseResource")
    public TarArchiveInputStream openTarGzipInput(final InputStream inputStream, final ChecksumHash64 hash64) throws IOException {
        final CustomInputStream customInputStream = new CustomInputStream (inputStream, BUFFER_BLOCK_SIZE);
        final BufferedInputStream bufferedStream = new BufferedInputStream (customInputStream, BUFFER_BLOCK_SIZE);
        if (hash64 != null) {
            customInputStream.setHash64(hash64);
        }
        final GzipCompressorInputStream compressStream = new GzipCompressorInputStream(bufferedStream);
        return new CustomTarArchiveInputStream(compressStream, BUFFER_BLOCK_SIZE);
    }

    /**
     * Wrap the Output stream into a tar.gzip buffered stream
     * @param outputStream OutputStream used as piped stream
     * @param hash64 inject the hash64 used for checksum
     * @return a Tar output stream
     * @throws IOException on IO file error
     */
    @SuppressWarnings("PMD.CloseResource")
    public TarArchiveOutputStream openTarGzipOutput(final OutputStream outputStream,
                                                    final ChecksumHash64 hash64) throws IOException {
        final ChecksumBufferedOutputStream bufferedStream = new ChecksumBufferedOutputStream(outputStream, BUFFER_BLOCK_SIZE);
        final GzipParameters parameters = new GzipParameters();
        final Optional<Environment> environment = SpringContext.getBean(Environment.class);
        if (environment.isPresent()) {
            final String compressionLevel = environment.get().getProperty("sftp.archive.compressionLevel", "BEST_SPEED");
            final int compressionLevelValue = getCompressionLevelValue(compressionLevel);
            parameters.setCompressionLevel(compressionLevelValue);
        } else {
            throw new ExportException("Unable to load application properties");
        }

        bufferedStream.setHash64(hash64);
        final OutputStream compressStream = new GzipCompressorOutputStream(bufferedStream, parameters);
        final TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(compressStream, TAR_COMPRESS_SIZE_BUFFER);
        tarArchiveOutputStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
        tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        return tarArchiveOutputStream;
    }

    /**
     * Converts the String value of the compression level into the Deflater library's compressionLevel constant integer value
     * @param compressionLevel the compression level
     * @return the Deflater library's compressionLevel constant integer value
     */
    protected int getCompressionLevelValue(final String compressionLevel) {
        switch (compressionLevel.toUpperCase().trim()) {
            case "NO_COMPRESSION":
                return Deflater.NO_COMPRESSION;
            case "BEST_COMPRESSION":
                return Deflater.BEST_COMPRESSION;
            case "DEFAULT_COMPRESSION":
                return Deflater.DEFAULT_COMPRESSION;
            case "BEST_SPEED":
                return Deflater.BEST_SPEED;
            default:
                throw new ExportException("Unrecognized export compression level: " + compressionLevel);
        }
    }

    /**
     * Adds data to the storeLocation + fileName in TarArchiveOuputStream
     * @param storeLocation where file is stored in tar
     * @param gzOut the output stream
     * @param fileName name of file being created in tar
     * @param data content
     */
    public static void passDataToTarOutputStreamLocation(final Path storeLocation,
                                                        final TarArchiveOutputStream gzOut, final String fileName, final String data) {
        final String fullFileName = storeLocation.toFile().getPath() + File.separator + fileName;
        try {
            final TarArchiveEntry file = new TarArchiveEntry(fullFileName);
            final byte[] content = data.getBytes();
            file.setSize(content.length);
            gzOut.putArchiveEntry(file);
            gzOut.write(content);
            gzOut.closeArchiveEntry();
        } catch (final IOException e) {
            throw new FileDirectoryException("Error creating file" + fullFileName + " {}", e);
        }
    }
}
