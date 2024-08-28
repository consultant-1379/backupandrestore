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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler;

import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;
import com.ericsson.adp.mgmt.backupandrestore.util.YangEnabledDisabled;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotNull;
import java.net.URI;

import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerConstants.ADMIN_STATE;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerConstants.AUTO_EXPORT;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerConstants.AUTO_EXPORT_PASSWORD;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerConstants.AUTO_EXPORT_URI;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerConstants.SCHEDULED_BACKUP_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerConstants.SFTP_SERVER_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.YangEnabledDisabled.DISABLED;

/**
 * Holds full scheduler information.
 */
public class SchedulerInformation implements Versioned<SchedulerInformation> {

    protected String mostRecentlyCreatedAutoBackup;
    protected String nextScheduledTime;

    @NotNull(message = "Missing required field adminState")
    protected AdminState adminState;

    @NotNull(message = "Missing required field scheduledBackupName")
    protected String scheduledBackupName;

    @NotNull(message = "Missing required field autoExport")
    protected YangEnabledDisabled autoExport = DISABLED;

    @NotNull(message = "Missing required field autoExportPassword")
    protected String autoExportPassword = "";

    @NotNull(message = "Missing required field autoExportUri")
    protected URI autoExportUri;

    protected String sftpServerName = "";

    private Version<SchedulerInformation> version;

    /**
     * To be used by Jackson
     */
    public SchedulerInformation() {}

    /**
     * Copy constructor of scheduler information
     * @param scheduler {@link Scheduler}
     */
    public SchedulerInformation(final SchedulerInformation scheduler) {
        adminState = scheduler.getAdminState();
        mostRecentlyCreatedAutoBackup = scheduler.getMostRecentlyCreatedAutoBackup();
        nextScheduledTime = scheduler.getNextScheduledTime();
        scheduledBackupName = scheduler.getScheduledBackupName();
        autoExport = scheduler.getAutoExport();
        autoExportPassword = scheduler.getAutoExportPassword();
        autoExportUri = scheduler.getAutoExportUri();
        sftpServerName = scheduler.getSftpServerName();
        version = scheduler.getVersion();
    }

    public AdminState getAdminState() {
        return adminState;
    }

    public String getMostRecentlyCreatedAutoBackup() {
        return mostRecentlyCreatedAutoBackup;
    }

    public String getNextScheduledTime() {
        return nextScheduledTime;
    }

    public String getScheduledBackupName() {
        return scheduledBackupName;
    }

    public YangEnabledDisabled getAutoExport() {
        return autoExport;
    }

    public String getAutoExportPassword() {
        return autoExportPassword;
    }

    public URI getAutoExportUri() {
        return autoExportUri;
    }

    public String getSftpServerName() {
        return sftpServerName;
    }

    public void setAdminState(final AdminState adminState) {
        this.adminState = adminState;
    }

    public void setMostRecentlyCreatedAutoBackup(final String mostRecentlyCreatedAutoBackup) {
        this.mostRecentlyCreatedAutoBackup = mostRecentlyCreatedAutoBackup;
    }

    public void setNextScheduledTime(final String nextScheduledTime) {
        this.nextScheduledTime = nextScheduledTime;
    }

    public void setScheduledBackupName(final String scheduledBackupName) {
        this.scheduledBackupName = scheduledBackupName;
    }

    public void setAutoExport(final YangEnabledDisabled autoExport) {
        this.autoExport = autoExport;
    }

    public void setAutoExportPassword(final String autoExportPassword) {
        this.autoExportPassword = autoExportPassword;
    }

    /**
     * Sets the autoExportUri
     * If autoExportUri is not null then, sets sftpServerName to empty
     * @param autoExportUri the autoExportUri
     */
    public void setAutoExportUri(final URI autoExportUri) {
        this.autoExportUri = autoExportUri;
        if (this.autoExportUri != null && !this.sftpServerName.isEmpty()) {
            setSftpServerName("");
        }
    }

    /**
     * Sets the sftpServerName
     * If sftpServerName is not empty then, sets autoExportUri to null and autoExportPassword to empty
     * @param sftpServerName the sftpServerName
     */
    public void setSftpServerName(final String sftpServerName) {
        this.sftpServerName = sftpServerName;
        if (this.autoExportUri != null && !this.sftpServerName.isEmpty()) {
            setAutoExportUri(null);
            setAutoExportPassword("");
        }
    }

    /**
     * Updates the value of the scheduler property
     * @param updatedProperty the name of the field based on the Yang model
     * @param newValue the new value of the field
     */
    public void updateProperty(final String updatedProperty, final String newValue) {
        if (updatedProperty.equalsIgnoreCase(ADMIN_STATE.toString())) {
            setAdminState(AdminState.valueOf(newValue.toUpperCase()));
        } else if (updatedProperty.equalsIgnoreCase(SCHEDULED_BACKUP_NAME.toString())) {
            setScheduledBackupName(newValue);
        } else if (updatedProperty.equalsIgnoreCase(AUTO_EXPORT.toString())) {
            setAutoExport(YangEnabledDisabled.caseSafeOf(newValue));
        } else if (updatedProperty.equalsIgnoreCase(AUTO_EXPORT_URI.toString())) {
            setAutoExportUri(URI.create(newValue));
        } else if (updatedProperty.equalsIgnoreCase(AUTO_EXPORT_PASSWORD.toString())) {
            setAutoExportPassword(newValue);
        } else if (updatedProperty.equalsIgnoreCase(SFTP_SERVER_NAME.toString())) {
            setSftpServerName(newValue);
        }
    }

    /**
     * Clears the value of the scheduler property.
     * This method only applies to auto-export-uri, auto-export-password and sftp-server-name.
     * @param property the name of the field based on the Yang model
     */
    public void clearProperty(final String property) {
        if (property.equalsIgnoreCase(AUTO_EXPORT_URI.toString())) {
            setAutoExportUri(null);
        } else if (property.equalsIgnoreCase(AUTO_EXPORT_PASSWORD.toString())) {
            setAutoExportPassword("");
        } else if (property.equalsIgnoreCase(SFTP_SERVER_NAME.toString())) {
            setSftpServerName("");
        }
    }

    @Override
    @JsonIgnore
    public Version<SchedulerInformation> getVersion() {
        return version;
    }

    @Override
    @JsonIgnore
    public void setVersion(final Version<SchedulerInformation> version) {
        this.version = version;
    }
}
