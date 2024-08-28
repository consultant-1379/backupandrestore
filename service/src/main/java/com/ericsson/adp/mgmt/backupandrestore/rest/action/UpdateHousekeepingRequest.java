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
package com.ericsson.adp.mgmt.backupandrestore.rest.action;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Generic Action request used to create or update
 */
public class UpdateHousekeepingRequest extends CreateActionRequest {

    @JsonProperty(value = "action")
    private ActionType action;

    @Override
    public ActionType getAction() {
        return action;
    }

    @Override
    public void setAction(final ActionType action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "UpdateHousekeepingRequest [auto-delete=" + getAutoDelete() + ", "
                + "max-stored-manual-backups=" + getMaximumManualBackupsNumberStored() + "]";
    }
}
