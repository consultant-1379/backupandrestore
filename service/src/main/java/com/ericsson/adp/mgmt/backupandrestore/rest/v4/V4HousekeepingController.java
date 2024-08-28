/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.v4;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionService;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.V4BRMHousekeepingJson;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.V4UpdateHousekeepingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Responsible for housekeeping specific V4 endpoint.
 */
@RestController
public class V4HousekeepingController extends V4Controller {

    @Autowired
    private ActionService actionService;

    /**
     * Gets housekeeping information for one backupManager.
     * Returns maximum number of backups allowed.
     * and auto delete current value
     * @param backupManagerId of backupManager to be retrieved.
     * @return json response of housekeeping information.
     */

    @GetMapping("backup-managers/{backupManagerId}/configuration/housekeeping")
    public V4BRMHousekeepingJson getHousekeeping(@PathVariable("backupManagerId") final String backupManagerId) {
        return new V4BRMHousekeepingJson(getBackupManager(backupManagerId).getHousekeeping());
    }

    /**
     * Put housekeeping information for one backupManager.
     * @param backupManagerId of backupManager to be updated.
     * @param request what to update.
     */
    @PutMapping("backup-managers/{backupManagerId}/configuration/housekeeping")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void putHousekeeping(
            @PathVariable("backupManagerId") final String backupManagerId,
            @Valid @RequestBody final V4UpdateHousekeepingRequest request) {
        patchHousekeeping(backupManagerId, request);
    }

    /**
     * Patch housekeeping information for one backupManager.
     * @param backupManagerId of backupManager to be updated.
     * @param request what to update.
     */
    @PatchMapping("backup-managers/{backupManagerId}/configuration/housekeeping")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void patchHousekeeping(
            @PathVariable("backupManagerId") final String backupManagerId,
            @RequestBody final V4UpdateHousekeepingRequest request) {
        request.setAction(ActionType.HOUSEKEEPING);
        actionService.handleActionRequest(backupManagerId, request);
    }
}
