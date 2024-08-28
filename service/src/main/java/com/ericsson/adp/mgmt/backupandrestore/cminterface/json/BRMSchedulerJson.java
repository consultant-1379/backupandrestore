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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.AdminState;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerInformation;
import com.ericsson.adp.mgmt.backupandrestore.util.YangEnabledDisabled;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Scheduler in the BRM model.
 */
@JsonInclude(Include.NON_NULL)
public class BRMSchedulerJson extends SchedulerInformation {
    private List<BRMPeriodicEventJson> periodicEvents = new ArrayList<>();
    private String brmSchedulerAdminState;
    private String brmSchedulerJsonAutoExport;
    private String brmSchedulerJsonAutoExportUri;
    private List<BRMProgressReportJson> progressReports = new ArrayList<>();

    /**
     * Default constructor, to be used by Jackson.
     */
    public BRMSchedulerJson() {}

    /**
     * Creates json object.
     *
     * @param scheduler
     *            scheduler to be represented.
     */
    public BRMSchedulerJson(final Scheduler scheduler) {
        super(scheduler);
        brmSchedulerAdminState = scheduler.getAdminState().getCmRepresentation();
        brmSchedulerJsonAutoExport = scheduler.getAutoExport().toString();
        if (scheduler.getAutoExportUri() != null) {
            brmSchedulerJsonAutoExportUri = scheduler.getAutoExportUri().toString();
        }
        sftpServerName = scheduler.getSftpServerName();
        addPeriodicEvents(scheduler);
    }

    /**
     * Creates json object.
     * @param scheduler scheduler to be represented.
     * @param actions to look for progressReport.
     */
    public BRMSchedulerJson(final Scheduler scheduler, final List<Action> actions) {
        this(scheduler);
        addLastActionBelongingToScheduler(actions);
    }

    public void setPeriodicEvents(final List<BRMPeriodicEventJson> periodicEvents) {
        this.periodicEvents = periodicEvents;
    }

    private void addPeriodicEvents(final Scheduler scheduler) {
        scheduler.getPeriodicEvents().stream().map(BRMPeriodicEventJson::new).forEach(periodicEvents::add);
    }

    @JsonProperty("progress-report")
    public List<BRMProgressReportJson> getProgressReports() {
        return progressReports.isEmpty() ? null : progressReports;
    }

    public void setProgressReports(final List<BRMProgressReportJson> progressReports) {
        this.progressReports = progressReports;
    }

    @JsonProperty("periodic-event")
    public List<BRMPeriodicEventJson> getPeriodicEvents() {
        return periodicEvents.isEmpty() ? null : periodicEvents;
    }

    @Override
    @JsonIgnore
    public AdminState getAdminState() {
        return super.getAdminState();
    }

    @JsonProperty("admin-state")
    public String getAdminStateString() {
        return brmSchedulerAdminState;
    }

    public void setAdminStateString(final String adminState) {
        this.brmSchedulerAdminState = adminState;
    }

    @Override
    @JsonIgnore
    public YangEnabledDisabled getAutoExport() {
        return super.getAutoExport();
    }

    @JsonProperty("auto-export")
    public String getAutoExportString() {
        return brmSchedulerJsonAutoExport;
    }

    public void setAutoExportString(final String autoExport) {
        this.brmSchedulerJsonAutoExport = autoExport;
    }

    @Override
    @JsonProperty("auto-export-password")
    public String getAutoExportPassword() {
        if ("".equals(super.getAutoExportPassword())) {
            return null;
        }
        return super.getAutoExportPassword();
    }

    @Override
    @JsonIgnore
    public URI getAutoExportUri() {
        return super.getAutoExportUri();
    }

    /**
     * @return string representation of AutoExportUri
     */
    @JsonProperty("auto-export-uri")
    public String getAutoExportUriString() {
        return brmSchedulerJsonAutoExportUri;
    }

    public void setAutoExportUriString(final String autoExportUri) {
        this.brmSchedulerJsonAutoExportUri = autoExportUri;
    }

    @Override
    @JsonProperty("most-recently-created-auto-backup")
    public String getMostRecentlyCreatedAutoBackup() {
        return super.getMostRecentlyCreatedAutoBackup();
    }

    @Override
    @JsonProperty("next-scheduled-time")
    public String getNextScheduledTime() {
        return super.getNextScheduledTime();
    }

    @Override
    @JsonProperty("scheduled-backup-name")
    public String getScheduledBackupName() {
        return super.getScheduledBackupName();
    }

    @Override
    @JsonProperty("sftp-server-name")
    public String getSftpServerName() {
        if ("".equals(super.getSftpServerName())) {
            return null;
        }
        return super.getSftpServerName();
    }

    @Override
    public String toString() {
        return "BRMSchedulerJson [admin-state=" + getAdminStateString() + ", auto-export=" + getAutoExportString() + ", auto-export-uri="
                + getAutoExportUriString() + ", most-recently-created-auto-backup=" + getMostRecentlyCreatedAutoBackup() + ", next-scheduled-time="
                + getNextScheduledTime() + ", scheduled-backup-name=" + getScheduledBackupName() + ", periodic-event=" + getPeriodicEvents()
                + ", sftp-server-name=" + getSftpServerName() + "]";
    }

    private void addLastActionBelongingToScheduler(final List<Action> actions) {
        final Optional<BRMProgressReportJson> lastProgressReport = actions
                .stream()
                .filter(a -> a.isScheduledEvent() && !a.isRestoreOrExport())
                .reduce((currentAction, nextAction) -> nextAction)
                .map(BRMProgressReportJson::new);
        if (lastProgressReport.isPresent()) {
            progressReports.clear(); // Clean progressReports to keep only the last one as defined in schema
            progressReports.add(lastProgressReport.get()); // Add the last progress report
        }
    }
}
