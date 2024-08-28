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
package com.ericsson.adp.mgmt.backupandrestore.rest.action;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.EmptyPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ExportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ImportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.Payload;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * JSON request to create an action.
 */
public class CreateActionRequest {

    private ActionType action;
    @JsonProperty(value = "auto-delete")
    private String autoDelete;
    @JsonProperty(value = "max-stored-manual-backups")
    private Integer maximumManualBackupsNumberStored;
    @JsonIgnore
    private boolean executedAsTask;
    @JsonIgnore
    private boolean isScheduledEvent;

    @JsonTypeInfo(use = Id.NAME, property = "action", include = As.EXTERNAL_PROPERTY)
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = BackupNamePayload.class, name = "CREATE_BACKUP"),
            @JsonSubTypes.Type(value = BackupNamePayload.class, name = "DELETE_BACKUP"),
            @JsonSubTypes.Type(value = BackupNamePayload.class, name = "RESTORE"),
            @JsonSubTypes.Type(value = ImportPayload.class, name = "IMPORT"),
            @JsonSubTypes.Type(value = EmptyPayload.class, name = "HOUSEKEEPING"),
            @JsonSubTypes.Type(value = ExportPayload.class, name = "EXPORT")
        })
    private Payload payload;

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

    public String getAutoDelete() {
        return autoDelete;
    }

    public void setAutoDelete(final String autoDelete) {
        this.autoDelete = autoDelete;
    }

    public Integer getMaximumManualBackupsNumberStored() {
        return maximumManualBackupsNumberStored;
    }

    public void setMaximumManualBackupsNumberStored(final Integer maximumManualBackupsNumberStored) {
        this.maximumManualBackupsNumberStored = maximumManualBackupsNumberStored;
    }

    public boolean isScheduledEvent() {
        return isScheduledEvent;
    }

    public void setScheduledEvent(final boolean isScheduledEvent) {
        this.isScheduledEvent = isScheduledEvent;
    }

    @Override
    public String toString() {
        return "CreateActionRequest [action=" + action + ", payload=" + payload
                + ", AutoDelete=" + autoDelete + ", maximumManualBackupsNumberStored ="
                + maximumManualBackupsNumberStored + ", isScheduledEvent="
                + isScheduledEvent + "]";
    }

    public boolean isExecutedAstask() {
        return executedAsTask;
    }

    public void setExecuteAstask(final boolean executedAsTask) {
        this.executedAsTask = executedAsTask;
    }
}
