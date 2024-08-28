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

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupCreationType;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.backup.SoftwareVersion;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.BackupResponse;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Backup in the BRM model.
 */
@JsonInclude(Include.NON_NULL)
public class BRMBackupJson extends BackupResponse {

    private List<BRMProgressReportJson> progressReports = new ArrayList<>();
    private List<BRMSoftwareVersionJson> softwareVersionJsons = new ArrayList<>();
    private String cmRepresentationOfStatus;
    private String cmRepresentationOfCreationType;

    /**
     * Default constructor, to be used by Jackson.
     */
    public BRMBackupJson() {}

    /**
     * Creates json object.
     * @param backup to be represented.
     */
    public BRMBackupJson(final Backup backup) {
        super(backup);
        backup
            .getSoftwareVersions()
            .stream()
            .map(BRMSoftwareVersionJson::new)
            .distinct() // Model requires software version list is unique, we store duplicates as we track which agent they came from
            .forEach(this.softwareVersionJsons::add);
        this.cmRepresentationOfStatus = backup.getStatus().getCmRepresentation();
        this.cmRepresentationOfCreationType = backup.getCreationType().getCmRepresentation();
        setCreationTime(DateTimeUtils.convertToString(backup.getCreationTime()));
    }

    /**
     * Creates json object.
     * @param backup to be represented.
     * @param actions to look for progressReport.
     */
    public BRMBackupJson(final Backup backup, final List<Action> actions) {
        this(backup);
        addLastActionBelongingToBackup(actions);
    }

    @Override
    @JsonProperty("backup-name")
    public String getName() {
        return super.getName();
    }

    @Override
    @JsonProperty("creation-time")
    public String getCreationTime() {
        return super.getCreationTime();
    }

    @Override
    @JsonIgnore
    public BackupCreationType getCreationType() {
        return super.getCreationType();
    }

    @Override
    @JsonIgnore
    public String getUserLabel() {
        return super.getUserLabel();
    }

    @Override
    @JsonIgnore
    public BackupStatus getStatus() {
        return super.getStatus();
    }

    @Override
    @JsonIgnore
    public List<SoftwareVersion> getSoftwareVersions() {
        return super.getSoftwareVersions();
    }

    @JsonProperty("progress-report")
    public List<BRMProgressReportJson> getProgressReports() {
        return progressReports.isEmpty() ? null : progressReports;
    }

    public void setProgressReports(final List<BRMProgressReportJson> progressReports) {
        this.progressReports = progressReports;
    }

    @JsonProperty("sw-version")
    public List<BRMSoftwareVersionJson> getSoftwareVersionJsons() {
        return softwareVersionJsons;
    }

    public void setSoftwareVersionJsons(final List<BRMSoftwareVersionJson> softwareVersionJsons) {
        this.softwareVersionJsons = softwareVersionJsons;
    }

    @JsonProperty("status")
    public String getCmRepresentationOfStatus() {
        return cmRepresentationOfStatus;
    }

    public void setCmRepresentationOfStatus(final String cmRepresentationOfStatus) {
        this.cmRepresentationOfStatus = cmRepresentationOfStatus;
    }

    @JsonProperty("creation-type")
    public String getCmRepresentationOfCreationType() {
        return cmRepresentationOfCreationType;
    }

    public void setCmRepresentationOfCreationType(final String cmRepresentationOfCreationType) {
        this.cmRepresentationOfCreationType = cmRepresentationOfCreationType;
    }

    private void addLastActionBelongingToBackup(final List<Action> actions) {
        actions
            .stream()
            .filter(action -> action.belongsToBackup(getBackupId()))
            .reduce((currentAction, nextAction) -> nextAction)
            .map(BRMProgressReportJson::new)
            .ifPresent(progressReports::add);
    }

}
