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
package com.ericsson.adp.mgmt.backupandrestore.rest;

import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.BackupResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.BackupsResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.UpdateBackupRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responsible for backup specific endpoints.
 */
@RestController
public class BackupController extends V1Controller {

    /**
     * Gets all backups under a backupManager.
     * @param backupManagerId of backupManager to look for.
     * @return json response of all backups of a backupManager.
     */
    @GetMapping("backup-manager/{backupManagerId}/backup")
    public BackupsResponse getBackupsForOneBackupManager(@PathVariable("backupManagerId") final String backupManagerId) {
        return new BackupsResponse(getBackupManager(backupManagerId).getBackups(Ownership.READABLE));
    }

    /**
     * Gets one backup.
     * @param backupManagerId of backupManager to look for.
     * @param backupId backup to look for.
     * @return json response of requested backup.
     */
    @GetMapping("backup-manager/{backupManagerId}/backup/{backupId}")
    public BackupResponse getOneBackupForOneBackupManager(
            @PathVariable("backupManagerId") final String backupManagerId,
            @PathVariable("backupId") final String backupId) {
        return getBackupManager(backupManagerId).getBackup(backupId, Ownership.READABLE).toJson();
    }

    /**
     * Updates one backup.
     * @param backupManagerId of backupManager to be updated.
     * @param backupId backup to updated.
     * @param request what to update.
     */
    @PostMapping("backup-manager/{backupManagerId}/backup/{backupId}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void updateOneBackupForOneBackupManager(
            @PathVariable("backupManagerId") final String backupManagerId,
            @PathVariable("backupId") final String backupId,
            @RequestBody final UpdateBackupRequest request) {
        final Backup backup = getBackupManager(backupManagerId).getBackup(backupId, Ownership.READABLE);
        backup.setUserLabel(request.getUserLabel());
        backup.persist();
    }

}
