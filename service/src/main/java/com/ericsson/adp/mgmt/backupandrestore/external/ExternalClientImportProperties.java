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
package com.ericsson.adp.mgmt.backupandrestore.external;

import static com.ericsson.adp.mgmt.backupandrestore.external.ImportFormat.LEGACY;
import static com.ericsson.adp.mgmt.backupandrestore.external.ImportFormat.TARBALL;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.web.util.UriComponentsBuilder;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServer;

/**
 * ExternalClientImportProperties holds properties which helps in import operation
 */
public class ExternalClientImportProperties extends ExternalClientProperties {
    private ImportFormat importFormat;
    private final Path folderToStoreBackupData;
    private final Path folderToStoreBackupFile;

    /**
     * ExternalClientImportProperties creates an instance of the properties which helps in import operation
     *
     * @param uri
     *            uri
     * @param password
     *            password
     * @param folderToStoreBackupData
     *            folder to store the backup data
     * @param folderToStoreBackupFile
     *            folder to store the backup files
     */
    public ExternalClientImportProperties(final String uri, final String password, final Path folderToStoreBackupData,
                                          final Path folderToStoreBackupFile) {
        super(uri, password);
        this.folderToStoreBackupData = folderToStoreBackupData;
        this.folderToStoreBackupFile = folderToStoreBackupFile;
        importFormat = getImportFormat(this.uri);
    }

    /**
     * ExternalClientImportProperties creates an instance of the properties which helps in import operation
     * @param sftpServer the SFTP Server name
     * @param backupManagerId the ID of the backup manager
     * @param backupPath the path to the backup or the backup tarball relative to to the remote-path configured in the SFTP Server
     * @param folderToStoreBackupData folder to store the backup data
     * @param folderToStoreBackupFile folder to store the backup files
     */
    public ExternalClientImportProperties(final SftpServer sftpServer, final String backupManagerId, final String backupPath,
                                          final Path folderToStoreBackupData, final Path folderToStoreBackupFile) {
        super(sftpServer);
        this.folderToStoreBackupData = folderToStoreBackupData;
        this.folderToStoreBackupFile = folderToStoreBackupFile;
        this.uri = getImportUri(sftpServer, backupManagerId, backupPath);
        importFormat = getImportFormat(this.uri);
    }

    private URI getImportUri(final SftpServer sftpServer, final String backupManagerId, final String backupPath) {
        if (Paths.get(backupPath).getNameCount() <= 1) {
            return UriComponentsBuilder.fromUri(sftpServer.getURI()).pathSegment(backupManagerId, backupPath).build().toUri();
        } else {
            return UriComponentsBuilder.fromUri(sftpServer.getURI()).pathSegment(backupPath).build().toUri();
        }
    }

    private ImportFormat getImportFormat(final URI uri) {
        return !uri.toString().endsWith(".tar.gz") ? LEGACY : TARBALL;
    }

    /**
     * Gets the FolderToStoreBackupFile
     *
     * @return the folderToStoreBackupFile
     */
    public Path getFolderToStoreBackupFile() {
        return folderToStoreBackupFile;
    }

    /**
     * Gets the FolderToStoreBackupData
     *
     * @return the folderToStoreBackupData
     */
    public Path getFolderToStoreBackupData() {
        return folderToStoreBackupData;
    }

    /**
     * Gets importFormat
     *
     * @return importFormat
     */
    public ImportFormat getImportFormat() {
        return importFormat;
    }

    /**
     * @param format
     *           format
     */
    public void setImportFormat(final ImportFormat format) {
        this.importFormat = format;
    }

}
