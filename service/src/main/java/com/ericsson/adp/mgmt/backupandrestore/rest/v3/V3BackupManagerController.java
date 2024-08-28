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

import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.BackupManagerResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.BackupManagersResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responsible for backupManager specific endpoints.
 */
@RestController
public class V3BackupManagerController extends V3Controller {

    /**
     * Gets all backupManagers.
     * @return json response of all backupManagers.
     */
    @GetMapping("backup-managers")
    public BackupManagersResponse getBackupManagers() {
        return new BackupManagersResponse(backupManagerRepository.getBackupManagers());
    }

    /**
     * Gets one backupManager.
     * @param backupManagerId of backupManager to look for.
     * @return json response of requested backupManager.
     */
    @GetMapping("backup-managers/{backupManagerId}")
    public BackupManagerResponse getOneBackupManager(@PathVariable("backupManagerId") final String backupManagerId) {
        return new BackupManagerResponse(getBackupManager(backupManagerId));
    }
}
