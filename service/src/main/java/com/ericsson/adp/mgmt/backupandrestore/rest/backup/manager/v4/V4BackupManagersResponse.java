/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.v4;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON response holding multiple backupManagers.
 */
public class  V4BackupManagersResponse{

    private List<V4BackupManagerResponse> backupManagers = new ArrayList<>();

    /**
     * Default constructor, to be used by Jackson.
     */
    public V4BackupManagersResponse() {}

    /**
     * Creates the V4BackupManagersResponse
     * @param brmResponses the collection of individual backup manager responses
     */
    public V4BackupManagersResponse(final List<V4BackupManagerResponse> brmResponses) {
        brmResponses.forEach(this.backupManagers::add);
    }

    public List<V4BackupManagerResponse> getBackupManagers() {
        return backupManagers;
    }

    public void setBackupManagers(final List<V4BackupManagerResponse> backupManagers) {
        this.backupManagers = backupManagers;
    }

}
