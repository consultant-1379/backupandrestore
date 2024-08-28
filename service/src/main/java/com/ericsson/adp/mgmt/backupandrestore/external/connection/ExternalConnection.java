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
package com.ericsson.adp.mgmt.backupandrestore.external.connection;

import java.beans.PropertyChangeListener;
import java.net.URI;
import java.nio.file.Path;

import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientImportProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.ImportFormat;

/**
 * ExternalConnection interface
 */
public interface ExternalConnection extends AutoCloseable {

    /**
     * exportBackup exports backup to Server
     *
     * @param backupFile
     *            backupFile path
     * @param backupData
     *            backupData path
     * @param remotePath
     *            remotePath
     * @param backupName
     *            backupName
     * @param backupManagerId
     *            backupManagerId
     * @param backup
     *            backup
     * @param listener
     *            Progress Monitor listener
     */
    void exportBackup(Path backupFile, Path backupData, String remotePath, String backupName,
            String backupManagerId, Backup backup, PropertyChangeListener listener);

    /**
     * importBackupData imports backup to Orchestrator from Server
     *
     * @param remotePath
     *            remotePath
     * @param backupData
     *            backupData Path
     * @param importFormat
     *            importFormat
     */
    void importBackupData(String remotePath, Path backupData, ImportFormat importFormat);

    /**
     * getBackupFileContent reads the backupfile from Server and returns its content
     *
     * @param remotePath
     *            remotePath
     * @param importFormat
     *            importFormat
     * @return String
     *
     */
    String getBackupFileContent(String remotePath, ImportFormat importFormat);

    /**
     * downloadBackupFile downloads backup file from server
     *
     * @param remoteUri
     *            remoteUri
     * @return String
     *
     */
    String downloadBackupFile(final URI remoteUri);

    /**
     * downloadBackupFile downloads backup file from server to backupData
     *
     * @param externalClientProperties client properties
     * @param listener An observer
     * @return String
     *
     */
    String downloadBackupFile(final ExternalClientImportProperties externalClientProperties, PropertyChangeListener listener);
}
