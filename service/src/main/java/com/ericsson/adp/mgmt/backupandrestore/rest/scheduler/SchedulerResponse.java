/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
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
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerInformation;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Full JSON response of a Scheduler.
 */
public class SchedulerResponse extends SchedulerInformation {

    /**
     * Default constructor used by Jackson
     */
    public SchedulerResponse() {}

    /**
     * @param scheduler {@link Scheduler} instance
     */
    public SchedulerResponse(final Scheduler scheduler) {
        adminState = scheduler.getAdminState();
        scheduledBackupName = scheduler.getScheduledBackupName();
        mostRecentlyCreatedAutoBackup = scheduler.getMostRecentlyCreatedAutoBackup();
        nextScheduledTime = scheduler.getNextScheduledTime();
        autoExport = scheduler.getAutoExport();
        autoExportPassword = scheduler.getAutoExportPassword().isEmpty() ? scheduler.getAutoExportPassword() : "*****";
        autoExportUri = scheduler.getAutoExportUri();
    }

    @Override
    @JsonIgnore
    public String getSftpServerName() {
        return super.getSftpServerName();
    }
}
