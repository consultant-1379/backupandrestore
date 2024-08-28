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

import java.nio.file.Path;

import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServer;

/**
 * ExternalClientExportProperties holds properties which helps in export operation
 */
public class ExternalClientExportProperties extends ExternalClientProperties {

    private final Path backupData;
    private final Path backupFile;
    private final String backupManagerId;
    private final Backup backup;

    /**
     * ExternalClientExportProperties sets the properties which helps in export operation
     *  @param uri
     *            uri
     * @param password
     *            password
     * @param backupData
     *            backupData
     * @param backupFile
     *            backupFile
     * @param backupManagerId
     *            backupManagerId
     * @param backup
     *          backup
     */
    public ExternalClientExportProperties(final String uri, final String password, final Path backupData, final Path backupFile,
                                          final String backupManagerId, final Backup backup) {

        super(uri, password);

        this.backupData = backupData;
        this.backupFile = backupFile;
        this.backupManagerId = backupManagerId;
        this.backup = backup;
    }

    /**
     * ExternalClientExportProperties creates and instance of the properties which helps in export operation
     * @param sftpServer
     *            the instance of sftp server
     * @param backupData
     *            backupData
     * @param backupFile
     *            backupFile
     * @param backupManagerId
     *            backupManagerId
     * @param backup
     *          backup
     */
    public ExternalClientExportProperties(final SftpServer sftpServer, final Path backupData, final Path backupFile,
                                          final String backupManagerId, final Backup backup) {
        super(sftpServer);
        this.uri = sftpServer.getURI();
        this.backupData = backupData;
        this.backupFile = backupFile;
        this.backupManagerId = backupManagerId;
        this.backup = backup;
    }

    /**
     * Gets the backupFile
     *
     * @return the backupFile
     */
    public Path getBackupFilePath() {
        return backupFile;
    }

    /**
     * @return the backupData
     */
    public Path getBackupDataPath() {
        return backupData;
    }

    /**
     * Gets the backupName
     *
     * @return the backupName
     */
    public String getBackupName() {
        return backup.getName();
    }

    /**
     * @return the backupManagerId
     */
    public String getBackupManagerId() {
        return backupManagerId;
    }

    /**
     * @return the backup
     */
    public Backup getBackup() {
        return backup;
    }
}
