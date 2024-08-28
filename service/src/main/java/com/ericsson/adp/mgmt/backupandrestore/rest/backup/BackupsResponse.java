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
package com.ericsson.adp.mgmt.backupandrestore.rest.backup;

import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON response of all backups of a backupManager
 */
public class BackupsResponse {

    private List<BackupResponse> backups = new ArrayList<>();

    /**
     * Default constructor, to be used by Jackson.
     */
    public BackupsResponse() {}

    /**
     * Creates BackupsResponse from list of backups.
     * @param backups to be returned
     */
    public BackupsResponse(final List<Backup> backups) {
        backups.stream().map(Backup::toSimplifiedJson).forEach(this.backups::add);
    }

    public List<BackupResponse> getBackups() {
        return backups;
    }

    public void setBackups(final List<BackupResponse> backups) {
        this.backups = backups;
    }

}
