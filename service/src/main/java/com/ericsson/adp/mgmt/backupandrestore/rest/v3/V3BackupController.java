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
package com.ericsson.adp.mgmt.backupandrestore.rest.v3;

import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.BackupResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.BackupsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responsible for backup specific endpoints.
 */
@RestController
public class V3BackupController extends V3Controller {

    /**
     * Gets all backups under a backupManager.
     * @param backupManagerId of backupManager to look for.
     * @return json response of all backups of a backupManager.
     */
    @GetMapping("backup-managers/{backupManagerId}/backups")
    public BackupsResponse getBackupsForOneBackupManager(@PathVariable("backupManagerId") final String backupManagerId) {
        return new BackupsResponse(getBackupManager(backupManagerId).getBackups(Ownership.READABLE));
    }

    /**
     * Gets one backup.
     * @param backupManagerId of backupManager to look for.
     * @param backupId backup to look for.
     * @return json response of requested backup.
     */
    @GetMapping("backup-managers/{backupManagerId}/backups/{backupId}")
    public BackupResponse getOneBackupForOneBackupManager(
            @PathVariable("backupManagerId") final String backupManagerId,
            @PathVariable("backupId") final String backupId) {
        return getBackupManager(backupManagerId).getBackup(backupId, Ownership.READABLE).toJson();
    }

}
