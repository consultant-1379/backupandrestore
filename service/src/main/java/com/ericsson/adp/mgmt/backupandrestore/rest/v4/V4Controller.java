/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.v4;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupRepository;
import com.ericsson.adp.mgmt.backupandrestore.rest.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

/**
 * Parent of controllers for v4 endpoints.
 */
@RequestMapping("backup-restore/v4")
public abstract class V4Controller extends BaseController {

    protected static final String BACKUP_MANAGERS_PATH = "/backup-restore/v4/backup-managers/";
    private BackupRepository backupRepository;

    /**
     * Get Resource URL for ongoing action
     * @param action onGoing action
     * @return resource URL
     */
    protected String buildResourceUrl(final Action action) {
        final StringBuilder resourceURL = new StringBuilder(BACKUP_MANAGERS_PATH)
                .append(action.getBackupManagerId());

        switch (action.getName()) {
            case CREATE_BACKUP:
            case DELETE_BACKUP:
                updateResourceURL(resourceURL, action, "/");
                break;
            case EXPORT:
                updateResourceURL(resourceURL, action, "/exports");
                break;
            case IMPORT:
                resourceURL.append("/imports");
                break;
            case RESTORE:
                updateResourceURL(resourceURL, action, "/restores");
                break;
            default:
                resourceURL.setLength(0);
        }
        return resourceURL.toString();
    }

    /**
     * This method updates the resourceURL by appending backupId if the action contains backupName.
     * @param resourceURL to be updated.
     * @param action action object containing backupName.
     * @param operation string to be appended to the URL.
     */
    private void updateResourceURL(final StringBuilder resourceURL, final Action action, final String operation) {
        final Optional<String> backupId = backupRepository.getBackups(action.getBackupManagerId()).stream()
                .filter(backup -> backup.getName().equals(action.getBackupName()))
                .findFirst()
                .map(Backup::getBackupId);

        if (backupId.isPresent()) {
            resourceURL.append("/backups/").append(backupId.get()).append(operation);
        } else {
            resourceURL.setLength(0);
        }
    }

    @Autowired
    public void setBackupRepository(final BackupRepository backupRepository) {
        this.backupRepository = backupRepository;
    }
}
