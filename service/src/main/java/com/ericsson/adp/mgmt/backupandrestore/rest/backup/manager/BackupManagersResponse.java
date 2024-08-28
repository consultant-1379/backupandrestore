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
package com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON response holding multiple backupManagers.
 */
public class BackupManagersResponse {

    private List<BackupManagerResponse> backupManagers = new ArrayList<>();

    /**
     * Default constructor, to be used by Jackson.
     */
    public BackupManagersResponse() {}

    /**
     * Creates BackupManagersResponse from a list of BackupManagers.
     * @param backupManagers list of backupManagers.
     */
    public BackupManagersResponse(final List<BackupManager> backupManagers) {
        backupManagers
            .stream()
            .map(BackupManagerResponse::new)
            .forEach(this.backupManagers::add);
    }

    public List<BackupManagerResponse> getBackupManagers() {
        return backupManagers;
    }

    public void setBackupManagers(final List<BackupManagerResponse> backupManagers) {
        this.backupManagers = backupManagers;
    }

}
