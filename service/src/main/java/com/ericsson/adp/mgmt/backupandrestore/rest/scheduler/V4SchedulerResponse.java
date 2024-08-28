/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.scheduler;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Full JSON response of a Scheduler.
 */
public class V4SchedulerResponse extends SchedulerResponse {

    @JsonIgnore
    private String v4ScheduledBackupName;

    /**
     * Default constructor used by Jackson
     */
    public V4SchedulerResponse() {}

    /**
     * @param scheduler {@link Scheduler} instance
     */
    public V4SchedulerResponse(final Scheduler scheduler) {
        adminState = scheduler.getAdminState();
        this.v4ScheduledBackupName = scheduler.getScheduledBackupName();
        mostRecentlyCreatedAutoBackup = scheduler.getMostRecentlyCreatedAutoBackup();
        nextScheduledTime = scheduler.getNextScheduledTime();
        autoExport = scheduler.getAutoExport();
        autoExportPassword = scheduler.getAutoExportPassword().isEmpty() ? scheduler.getAutoExportPassword() : "*****";
        autoExportUri = scheduler.getAutoExportUri();
    }

    public void setScheduledBackupNamePrefix(final String scheduledBackupName) {
        this.v4ScheduledBackupName = scheduledBackupName;
    }

    public String getScheduledBackupNamePrefix() {
        return v4ScheduledBackupName;
    }
}
