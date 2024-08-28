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

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.BackupManagerResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.BackupManagersResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.UpdateBackupManagerRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responsible for backupManager specific endpoints.
 */
@RestController
public class BackupManagerController extends V1Controller {

    /**
     * Gets all backupManagers.
     * @return json response of all backupManagers.
     */
    @GetMapping("backup-manager")
    public BackupManagersResponse getBackupManagers() {
        return new BackupManagersResponse(backupManagerRepository.getBackupManagers());
    }

    /**
     * Gets one backupManager.
     * @param backupManagerId of backupManager to look for.
     * @return json response of requested backupManager.
     */
    @GetMapping("backup-manager/{backupManagerId}")
    public BackupManagerResponse getOneBackupManager(@PathVariable("backupManagerId") final String backupManagerId) {
        return new BackupManagerResponse(getBackupManager(backupManagerId));
    }

    /**
     * Updates one backupManager.
     * @param backupManagerId of backupManager to be updated.
     * @param request what to update.
     */
    @PostMapping("backup-manager/{backupManagerId}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void updateBackupManager(
            @PathVariable("backupManagerId") final String backupManagerId,
            @RequestBody final UpdateBackupManagerRequest request) {
        final BackupManager backupManager = getBackupManager(backupManagerId);
        backupManager.setBackupDomain(request.getBackupDomain());
        backupManager.setBackupType(request.getBackupType());
        backupManager.persist();
    }

}
