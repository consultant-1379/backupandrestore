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

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.exception.ActionNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.ActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.ActionsResponse;
import java.text.MessageFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responsible for action specific endpoints.
 */
@RestController
public class V3ActionController extends V3Controller {

    /**
     * Gets all actions on a backupManager.
     * @param backupManagerId backupManager to get all actions from.
     * @return all actions from backupManager.
     */
    @GetMapping("backup-managers/{backupManagerId}/actions")
    public ActionsResponse getActions(@PathVariable("backupManagerId") final String backupManagerId) {
        return new ActionsResponse(getBackupManager(backupManagerId).getActions());
    }

    /**
     * Get specific action from a backupManager.
     * @param backupManagerId backupManager in which to look for the action.
     * @param actionId action to look for.
     * @return request action.
     */
    @GetMapping("backup-managers/{backupManagerId}/actions/{actionId}")
    public ActionResponse getAction(
        @PathVariable("backupManagerId") final String backupManagerId,
        @PathVariable("actionId") final String actionId) {
        return getBackupManager(backupManagerId).getAction(actionId).toJson();
    }

    /**
     * Get specific action from a backupManager and backupId.
     * @param backupManagerId backupManager in which to look for the action.
     * @param backupId backup look for the action in
     * @param actionId action to look for.
     * @return request action.
     */
    @GetMapping("backup-managers/{backupManagerId}/backups/{backupId}/actions/{actionId}")
    public ActionResponse getAction(
        @PathVariable("backupManagerId") final String backupManagerId,
        @PathVariable("backupId") final String backupId,
        @PathVariable("actionId") final String actionId) {
        final Action action = getBackupManager(backupManagerId).getAction(actionId);
        if (action.getBackupName().equals(backupId)) {
            return action.toJson();
        }
        throw new ActionNotFoundException(
            MessageFormat.format("Action <{0}> not found or Action<{0}> not associated with Backup <{1}>.", actionId, backupId));
    }
}
