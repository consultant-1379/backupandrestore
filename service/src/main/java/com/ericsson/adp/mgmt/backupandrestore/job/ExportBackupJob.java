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

import static com.ericsson.adp.mgmt.backupandrestore.archive.ArchiveUtils.getTarballName;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.PROGRESS_MONITOR_CURRENT_PERCENTAGE;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.action.payload.ExportPayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupExporter;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServer;
import com.ericsson.adp.mgmt.backupandrestore.exception.SftpServerNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientExportProperties;
import com.ericsson.adp.mgmt.backupandrestore.kms.CMKeyPassphraseService;

/**
 * ExportBackupJob helps to export backup to External Storage from Orchestrator
 */
public class ExportBackupJob extends Job implements PropertyChangeListener {

    private static final Logger log = LogManager.getLogger(ExportBackupJob.class);
    private BackupLocationService backupLocationService;
    private BackupFileService backupFileService;
    private BackupExporter backupExporter;
    private CMKeyPassphraseService cmKeyPassphraseService;


    @Override
    protected void triggerJob() {

        final String backupName = ((ExportPayload) action.getPayload()).getBackupName();

        final Backup backup = backupManager.getBackup(backupName, Ownership.OWNED);
        final Path backupDataPath = backupLocationService.getBackupFolder(backupManager, backupName).getBackupLocation();
        log.info("the backupdata path {} to be exported", backupDataPath);

        final Path backupFilePath = backupFileService.getBackupFilePath(backupManager.getBackupManagerId(), backupName);
        log.info("the backupfile path {} to be exported", backupFilePath);

        final ExternalClientExportProperties externalClientProperties = populateExternalClientProperties(backupDataPath, backupFilePath, backup);
        backupExporter.exportBackup(externalClientProperties, this);
    }

    private ExternalClientExportProperties populateExternalClientProperties(final Path backupDataLocation, final Path backupFile,
                                                                            final Backup backup) {

        final ExportPayload payload = ((ExportPayload) action.getPayload());
        if (payload.hasPassword() && action.isScheduledEvent() && cmKeyPassphraseService.isEnabled()) {
            payload.setPassword(cmKeyPassphraseService.getPassphrase(payload.getPassword()));
        }

        if (payload.hasSftpServerName()) {
            log.info("Creating client properties using the sftp server public key authentication credentials");
            final String sftpServerName = payload.getSftpServerName();
            final SftpServer sftpServer = backupManager.getSftpServer(sftpServerName)
                                              .orElseThrow(() -> new SftpServerNotFoundException(sftpServerName, backupManager.getBackupManagerId()));
            return new ExternalClientExportProperties(sftpServer, backupDataLocation, backupFile,
                    backupManager.getBackupManagerId(), backup);
        } else {
            log.info("Creating client properties using the sftp server password credential.");
            return new ExternalClientExportProperties(payload.getUri().toString(), payload.getPassword(), backupDataLocation, backupFile,
                    backupManager.getBackupManagerId(), backup);
        }
    }

    @Override
    protected boolean didFinish() {
        return true;
    }

    @Override
    protected void completeJob() {
        final String backupName = ((ExportPayload) action.getPayload()).getBackupName();

        final Backup backup = backupManager.getBackup(backupName, Ownership.OWNED);

        action.setAdditionalInfo(String.format("Exported Backup: %s", getTarballName(backup)));

        log.info("ExportBackup job successfully updated backup in external storage");
        resetCM(true);
    }

    @Override
    protected void fail() {
        log.info("ExportBackup job failed");
        resetCM(true);
    }

    protected void setBackupLocationService(final BackupLocationService backupLocationService) {
        this.backupLocationService = backupLocationService;
    }

    protected void setBackupFileService(final BackupFileService backupFileService) {
        this.backupFileService = backupFileService;
    }

    protected void setBackupExporter(final BackupExporter backupExporter) {
        this.backupExporter = backupExporter;
    }

    protected void setExportPasswordService(final CMKeyPassphraseService exportPasswordService) {
        this.cmKeyPassphraseService = exportPasswordService;
    }

    @Override
    public void propertyChange(final PropertyChangeEvent propertyUpdated) {
        if (propertyUpdated.getPropertyName().equals(PROGRESS_MONITOR_CURRENT_PERCENTAGE)) {
            action.setProgressPercentage((Double) propertyUpdated.getNewValue() / 100);
            log.debug("Pushing action progress report from export at " + propertyUpdated.getNewValue() + " %");
            actionRepository.enqueueProgressReport(action);
        }
    }

}
