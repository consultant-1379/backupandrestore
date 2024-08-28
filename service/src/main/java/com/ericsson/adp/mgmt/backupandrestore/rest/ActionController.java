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

import com.ericsson.adp.mgmt.backupandrestore.action.ActionService;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.ActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.ActionsResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responsible for action specific endpoints.
 */
@RestController
public class ActionController extends V1Controller {

    @Autowired
    private ActionService actionService;

    /**
     * Creates an action.
     * @param backupManagerId backupManager on which the action will be performed.
     * @param request action input.
     * @return id of action.
     */
    @PostMapping("backup-manager/{backupManagerId}/action")
    @ResponseStatus(value = HttpStatus.CREATED)
    public CreateActionResponse createAction(
             @PathVariable("backupManagerId") final String backupManagerId,
             @RequestBody final CreateActionRequest request) {
        return new CreateActionResponse(actionService.handleActionRequest(backupManagerId, request).getActionId());
    }

    /**
     * Gets all actions on a backupManager.
     * @param backupManagerId backupManager to get all actions from.
     * @return all actions from backupManager.
     */
    @GetMapping("backup-manager/{backupManagerId}/action")
    public ActionsResponse getActions(@PathVariable("backupManagerId") final String backupManagerId) {
        return new ActionsResponse(getBackupManager(backupManagerId).getActions());
    }

    /**
     * Get specific action from a backupManager.
     * @param backupManagerId backupManager in which to look for the action.
     * @param actionId action to look for.
     * @return request action.
     */
    @GetMapping("backup-manager/{backupManagerId}/action/{actionId}")
    public ActionResponse getAction(
            @PathVariable("backupManagerId") final String backupManagerId,
            @PathVariable("actionId") final String actionId) {
        return getBackupManager(backupManagerId).getAction(actionId).toJson();
    }

}
