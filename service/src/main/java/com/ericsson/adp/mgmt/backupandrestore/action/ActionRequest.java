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
package com.ericsson.adp.mgmt.backupandrestore.action;

import com.ericsson.adp.mgmt.backupandrestore.action.payload.Payload;

/**
 * Information needed to create an action.
 */
public class ActionRequest {

    private String actionId;
    private ActionType action;
    private Payload payload;
    private String backupManagerId;
    private boolean executeAsTask;
    private String autoDelete;
    private int maximumManualBackupsStored;
    private boolean isScheduledEvent;

    public String getActionId() {
        return actionId;
    }

    public void setActionId(final String actionId) {
        this.actionId = actionId;
    }

    public ActionType getAction() {
        return action;
    }

    public void setAction(final ActionType action) {
        this.action = action;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(final Payload payload) {
        this.payload = payload;
    }

    public String getBackupManagerId() {
        return backupManagerId;
    }

    public void setBackupManagerId(final String backupManagerId) {
        this.backupManagerId = backupManagerId;
    }

    public boolean isExecutedAsTask() {
        return executeAsTask;
    }

    public void setExecuteAsTask(final boolean executeAsTask) {
        this.executeAsTask = executeAsTask;
    }

    public String getAutoDelete() {
        return autoDelete;
    }

    public int getMaximumManualBackupsStored() {
        return maximumManualBackupsStored;
    }

    public void setAutoDelete(final String autoDelete) {
        this.autoDelete = autoDelete;
    }

    public void setMaximumManualBackupsStored(final int maximumManualBackupsStored) {
        this.maximumManualBackupsStored = maximumManualBackupsStored;
    }

    public boolean isScheduledEvent() {
        return isScheduledEvent;
    }

    public void setScheduledEvent(final boolean isScheduledEvent) {
        this.isScheduledEvent = isScheduledEvent;
    }

}
