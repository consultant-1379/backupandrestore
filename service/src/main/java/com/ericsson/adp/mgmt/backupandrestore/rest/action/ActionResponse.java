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

import com.ericsson.adp.mgmt.backupandrestore.action.ActionInformation;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.EmptyPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ExportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ImportPayload;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.Payload;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Full JSON response of an action
 */
@JsonInclude(Include.NON_NULL)
public class ActionResponse extends ActionInformation {

    protected Double progressPercentage;
    protected String startTime;
    protected String completionTime;
    protected String lastUpdateTime;

    @Override
    @JsonProperty("id")
    public String getActionId() {
        return super.getActionId();
    }

    public void setActionId(final String actionId) {
        this.actionId = actionId;
    }

    public void setName(final ActionType name) {
        this.name = name;
    }

    @Override
    @JsonTypeInfo(use = Id.NAME, property = "name", include = As.EXTERNAL_PROPERTY)
    @JsonSubTypes(value = { @JsonSubTypes.Type(value = BackupNamePayload.class, name = "CREATE_BACKUP"),
            @JsonSubTypes.Type(value = BackupNamePayload.class, name = "DELETE_BACKUP"),
            @JsonSubTypes.Type(value = BackupNamePayload.class, name = "HOUSEKEEPING_DELETE_BACKUP"),
            @JsonSubTypes.Type(value = BackupNamePayload.class, name = "RESTORE"),
            @JsonSubTypes.Type(value = ImportPayload.class, name = "IMPORT"),
            @JsonSubTypes.Type(value = ExportPayload.class, name = "EXPORT"),
            @JsonSubTypes.Type(value = EmptyPayload.class, name = "HOUSEKEEPING")})
    public Payload getPayload() {
        return payload;
    }

    @Override
    public void setPayload(final Payload payload) {
        this.payload = payload;
    }

    public Double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(final Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(final String startTime) {
        this.startTime = startTime;
    }

    public String getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(final String completionTime) {
        this.completionTime = completionTime;
    }

    public String getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(final String lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

}
