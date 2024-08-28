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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import java.util.ArrayList;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerInformation;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.BackupManagerResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a BackupManager in the BRM model.
 *
 * Configured to ignore parentId and agents as EOI model does not have vBRMs
 */
@JsonIgnoreProperties(value = {"parentId", "agents"})
public class BRMBackupManagerJson extends BackupManagerResponse {

    private List<BRMBackupJson> backups = new ArrayList<>();
    private List<BRMProgressReportJson> progressReports = new ArrayList<>();
    private List<SftpServerInformation> sftpServers = new ArrayList<>();
    private BRMHousekeepingJson housekeeping;
    private BRMSchedulerJson scheduler;

    /**
     * Default constructor, to be used by Jackson.
     */
    public BRMBackupManagerJson() {}

    /**
     * Creates a backupManager representation.
     * @param backupManager original backupManager.
     */
    public BRMBackupManagerJson(final BackupManager backupManager) {
        super(backupManager);
        addBackups(backupManager);
        addLastActionAsProgressReport(backupManager);
        addSftpServers(backupManager);
        housekeeping = new BRMHousekeepingJson(backupManager.getHousekeeping());
        scheduler = new BRMSchedulerJson(backupManager.getScheduler(), backupManager.getActions());
    }

    @Override
    @JsonProperty("backup-domain")
    public String getBackupDomain() {
        if (null == super.getBackupDomain()) {
            return "";
        }
        return super.getBackupDomain();
    }

    @Override
    @JsonProperty("backup-type")
    public String getBackupType() {
        if (null == super.getBackupType()) {
            return "";
        }
        return super.getBackupType();
    }

    @JsonProperty("backup")
    public List<BRMBackupJson> getBackups() {
        return backups.isEmpty() ? null : backups;
    }

    public void setBackups(final List<BRMBackupJson> backups) {
        this.backups = backups;
    }

    @JsonProperty("progress-report")
    public List<BRMProgressReportJson> getProgressReports() {
        return progressReports.isEmpty() ? null : progressReports;
    }

    public void setProgressReports(final List<BRMProgressReportJson> progressReports) {
        this.progressReports = progressReports;
    }

    @JsonProperty("housekeeping")
    public BRMHousekeepingJson getHousekeeping() {
        return housekeeping;
    }

    public void setHousekeeping(final BRMHousekeepingJson housekeeping) {
        this.housekeeping = housekeeping;
    }

    @JsonProperty("scheduler")
    public BRMSchedulerJson getScheduler() {
        return scheduler;
    }

    public void setScheduler(final BRMSchedulerJson scheduler) {
        this.scheduler = scheduler;
    }

    private void addBackups(final BackupManager backupManager) {
        backupManager
        .getBackups(Ownership.READABLE)
        .stream()
        .map(backup -> new BRMBackupJson(backup, backupManager.getActions())).forEach(backups::add);
    }

    private void addLastActionAsProgressReport(final BackupManager backupManager) {
        backupManager
        .getActions()
        .stream()
        .filter(action -> !action.isRestoreOrExport())
        .filter(action -> !action.isPartOfHousekeeping())
        .reduce((currentAction, nextAction) -> nextAction)
        .map(BRMProgressReportJson::new).ifPresent(progressReports::add);
    }

    private void addSftpServers(final BackupManager backupManager) {
        backupManager.getSftpServers()
            .stream()
            .map(SftpServerInformation::new)
            .forEach(sftpServers::add);
    }

    @JsonProperty("sftp-server")
    public List<SftpServerInformation> getSftpServers() {
        return sftpServers.isEmpty() ? null : sftpServers;
    }

    public void setSftpServers(final List<SftpServerInformation> sftpServers) {
        this.sftpServers = sftpServers;
    }

    @Override
    public String toString() {
        return "BRMBackupManagerJson [backups=" + backups + ", progressReports=" + progressReports + ", sftpServers=" + sftpServers
                + ", housekeeping=" + housekeeping + ", scheduler =" + scheduler + ", backupManagerId=" + backupManagerId
                + ", backupType=" + backupType + ", backupDomain=" + backupDomain + "]";
    }

}
