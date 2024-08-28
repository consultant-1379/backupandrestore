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
package com.ericsson.adp.mgmt.backupandrestore.job;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.PROGRESS_MONITOR_CURRENT_PERCENTAGE;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.nio.file.Path;

import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServer;
import com.ericsson.adp.mgmt.backupandrestore.exception.SftpServerNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.action.payload.ImportPayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupImporter;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientImportProperties;

/**
 * ImportBackupJob helps to import backup in Orchestrator from external storage
 */
public class ImportBackupJob extends Job implements PropertyChangeListener{

    private static final Logger log = LogManager.getLogger(ImportBackupJob.class);

    private BackupLocationService backupLocationService;
    private BackupFileService backupFileService;
    private Backup backup;
    private BackupImporter backupImporter;

    @Override
    protected void triggerJob() {

        final Path backupDataLocation = backupLocationService.getBackupManagerLocation(backupManager.getBackupManagerId());
        log.info("the backupdata location {} to be imported in Orchestrator", backupDataLocation);

        final Path backupFileLocation = backupFileService.getBackupFolder(backupManager.getBackupManagerId());
        log.info("the backupfile location {} to be imported in Orchestrator", backupFileLocation);

        final ExternalClientImportProperties externalClientProperties = populateExternalClientProperties(backupDataLocation, backupFileLocation);

        backup = backupImporter.importBackup(externalClientProperties, backupManager, this);

    }

    @Override
    protected boolean didFinish() {
        return backupManager.getBackups(Ownership.READABLE).contains(backup);
    }

    @Override
    protected void completeJob() {
        log.info("Import job {} complete backupManager <{}>", getActionId() , backupManager.getBackupManagerId());
        resetCM(true);
    }

    @Override
    protected void fail() {
        if (backup != null) {
            backup.setStatus(BackupStatus.CORRUPTED);
            backup.persist();
        }
        resetCM(true);
    }

    protected void setBackupLocationService(final BackupLocationService backupLocationService) {
        this.backupLocationService = backupLocationService;
    }

    protected void setBackupFileService(final BackupFileService backupFileService) {
        this.backupFileService = backupFileService;
    }

    protected void setBackupImporter(final BackupImporter backupImporter) {
        this.backupImporter = backupImporter;
    }

    private ExternalClientImportProperties populateExternalClientProperties(final Path backupDataLocation, final Path backupFileLocation) {

        final ImportPayload payload = ((ImportPayload) action.getPayload());

        if (payload.hasSftpServerName()) {
            log.info("Creating client properties using the sftp server public key authentication credentials");
            final String sftpServerName = payload.getSftpServerName();
            final SftpServer sftpServer = backupManager.getSftpServer(sftpServerName)
                                              .orElseThrow(() -> new SftpServerNotFoundException(sftpServerName, backupManager.getBackupManagerId()));
            return new ExternalClientImportProperties(sftpServer, backupManager.getBackupManagerId(), payload.getBackupPath(),
                                                        backupDataLocation, backupFileLocation);
        } else {
            log.info("Creating client properties using the sftp server password credential.");
            final String password = payload.getPassword();
            final URI uri = payload.getUri();
            return new ExternalClientImportProperties(uri.toString(), password, backupDataLocation, backupFileLocation);
        }

    }

    @Override
    public void propertyChange(final PropertyChangeEvent propertyUpdated) {
        if (propertyUpdated.getPropertyName().equals(PROGRESS_MONITOR_CURRENT_PERCENTAGE)) {
            action.setProgressPercentage((Double) propertyUpdated.getNewValue() / 100);
            log.debug("Pushing action progress report from import at " + propertyUpdated.getNewValue() + " %");
            actionRepository.enqueueProgressReport(action);
        }
    }
}
