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

import com.ericsson.adp.mgmt.backupandrestore.action.ActionService;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMHousekeepingJson;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.UpdateHousekeepingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responsible for housekeeping specific V3 endpoint.
 */
@RestController
public class V3HousekeepingController extends V3Controller {

    @Autowired
    private ActionService actionService;


    /**
     * Gets housekeeping information for one backupManager.
     * Returns maximum number of backups allowed.
     * and auto delete current value
     * @param backupManagerId of backupManager to be retrieved.
     * @return json response of housekeeping information.
     */

    @GetMapping("backup-managers/{backupManagerId}/housekeeping")
    public BRMHousekeepingJson getHousekeeping(@PathVariable("backupManagerId") final String backupManagerId) {
        return new BRMHousekeepingJson(getBackupManager(backupManagerId).getHousekeeping());
    }

    /**
     * Updates housekeeping information for one backupManager.
     * @param backupManagerId of backupManager to be updated.
     * @param request what to update.
     * @return id of action.
     */
    @PostMapping("backup-managers/{backupManagerId}/housekeeping")
    @ResponseStatus(value = HttpStatus.CREATED)
    public CreateActionResponse updateHousekeeping(
                                                   @PathVariable("backupManagerId") final String backupManagerId,
                                                   @RequestBody final UpdateHousekeepingRequest request) {
        request.setAction(ActionType.HOUSEKEEPING);
        return new CreateActionResponse(actionService.handleActionRequest(backupManagerId, request).getActionId());
    }

}
