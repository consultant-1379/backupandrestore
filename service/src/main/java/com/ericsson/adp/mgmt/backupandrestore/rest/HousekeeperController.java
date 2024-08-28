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

import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMHousekeepingJson;
import com.ericsson.adp.mgmt.backupandrestore.exception.NotImplementedException;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.UpdateHousekeepingRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responsible for housekeeper specific endpoints.
 */
@RestController
public class HousekeeperController extends V1Controller {

    /**
     * Gets housekeeping information for one backupManager.
     * @param backupManagerId of backupManager to be updated.
     * @return json response of housekeeping information.
     */
    @GetMapping("backup-manager/{backupManagerId}/housekeeper")
    public BRMHousekeepingJson getHousekeeper(@PathVariable("backupManagerId") final String backupManagerId) {
        throw new NotImplementedException("Not implemented");
    }

    /**
     * Updates housekeeping information for one backupManager.
     * @param backupManagerId of backupManager to be updated.
     * @param request what to update.
     */
    @PostMapping("backup-manager/{backupManagerId}/housekeeper")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void updateHousekeeper(
                                  @PathVariable("backupManagerId") final String backupManagerId,
                                  @RequestBody final UpdateHousekeepingRequest request) {
        throw new NotImplementedException("Not implemented");
    }

}
