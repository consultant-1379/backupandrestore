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
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.scheduler;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerInformation;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidURIException;
import com.ericsson.adp.mgmt.backupandrestore.util.ESAPIValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;

import java.net.URI;

/**
 *JSON request to update a backupManager's scheduler.
 * This will be extended to handle updating a Scheduler
 */
public class SchedulerRequest extends SchedulerInformation {

    /**
     * Sets all member variables of the passed SchedulerInformation object to the value of all non-null
     * member variables of this request.
     * @param other - the scheduler information to be updated
     * */
    public void partialUpdate(final SchedulerInformation other) {
        validateAutoExportConfiguration(this.scheduledBackupName, this.autoExportUri);
        other.setAdminState(this.adminState != null ? this.adminState : other.getAdminState());
        other.setAutoExport(this.autoExport != null ? this.autoExport : other.getAutoExport());
        other.setAutoExportPassword(this.autoExportPassword != null && !this.autoExportPassword.isEmpty() ?
                this.autoExportPassword : other.getAutoExportPassword()); // Note - this assumes we don't support empty export passwords
        other.setAutoExportUri(this.autoExportUri != null ? this.autoExportUri : other.getAutoExportUri());
        other.setScheduledBackupName(this.scheduledBackupName != null && !this.scheduledBackupName.isEmpty() ?
                this.scheduledBackupName : other.getScheduledBackupName()); // Note - this assumes we don't support empty backup names
    }

    private static void validateAutoExportConfiguration(final String backupName, final URI uri) {
        final ESAPIValidator esapiValidator = new ESAPIValidator();
        final IdValidator idValidator = new IdValidator();
        if (backupName != null) {
            idValidator.validateId(backupName);
            esapiValidator.validateBackupName(backupName);
        }
        if (uri == null) {
            return;
        }
        if (uri.toString().isEmpty()) {
            throw new InvalidURIException("Invalid auto export configuration - No URI was provided");
        }
        esapiValidator.validateURI(uri);
    }

}
