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
package com.ericsson.adp.mgmt.backupandrestore.backup;

import java.beans.PropertyChangeListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.exception.ExportException;
import com.ericsson.adp.mgmt.backupandrestore.exception.ImportExportException;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientExportProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.ExternalConnection;

/**
 * BackupExporter export backup to Sftp/Http Server from BRO
 */
@Service
public class BackupExporter {

    private ExternalConnectionFactory externalConnectionFactory;

    /**
     * exportBackup exports backup to Sftp/Http Server from the Orchestrator
     *
     * @param externalClientExportProperties
     *            externalClientExportProperties
     * @param listener
     *            Property listener used as observer for properties changes on Monitor
     */
    public void exportBackup(final ExternalClientExportProperties externalClientExportProperties, final PropertyChangeListener listener) {

        try (ExternalConnection connection = externalConnectionFactory.connect(externalClientExportProperties)) {
            String remotePath;
            if (externalClientExportProperties.isUsingHttpUriScheme()) {
                remotePath = externalClientExportProperties.getUri().toString();
            } else {
                remotePath = externalClientExportProperties.getExternalClientPath();
            }
            connection.exportBackup(externalClientExportProperties.getBackupFilePath(), externalClientExportProperties.getBackupDataPath(),
                    remotePath, externalClientExportProperties.getBackupName(), externalClientExportProperties.getBackupManagerId(),
                    externalClientExportProperties.getBackup(), listener);
        } catch (final ExportException | ImportExportException e) {
            throw e;
        } catch (final Exception e) {
            throw new ExportException("Failed while trying to export backup from Server", e);
        }
    }

    @Autowired
    public void setExternalConnectionFactory(final ExternalConnectionFactory externalConnectionFactory) {
        this.externalConnectionFactory = externalConnectionFactory;
    }

}