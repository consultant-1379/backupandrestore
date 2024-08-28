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
package com.ericsson.adp.mgmt.backupandrestore.rest.action;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.Payload;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Full JSON Response of a Task
 *
 */
@JsonInclude(Include.NON_DEFAULT)
public class TaskResponse extends ActionResponse {

    private String resource;

    /**
     * Default constructor, to be used by Jackson.
     */
    public TaskResponse() {}

    /**
     * Represents the JSON response of a task
     * @param action the corresponding action of the task
     */
    public TaskResponse(final Action action) {
        startTime = DateTimeUtils.convertToString(action.getStartTime().toLocalDateTime());
        lastUpdateTime = DateTimeUtils.convertToString(action.getLastUpdateTime().toLocalDateTime());
        additionalInfo = action.getAdditionalInfo();
        if (action.getCompletionTime() != null) {
            completionTime = DateTimeUtils.convertToString(action.getCompletionTime().toLocalDateTime());
        }
        lastUpdateTime = DateTimeUtils.convertToString(action.getLastUpdateTime().toLocalDateTime());
        if (action.hasMessages()) {
            resultInfo = action.getAllMessagesAsSingleString();
        }
        progressInfo = action.getProgressInfo();
        progressPercentage = action.getProgressPercentage();
        result = action.getResult();
        state = action.getState();
    }

    /**
     * Represents the JSON response of a task
     * @param action the corresponding action of the task
     * @param resourceURL resource URL of ongoing action
     */
    public TaskResponse(final Action action, final String resourceURL) {
        this(action);
        resource = resourceURL;
    }

    @Override
    @JsonIgnore
    public String getActionId() {
        return super.getActionId();
    }

    @Override
    @JsonIgnore
    public ActionType getName() {
        return super.getName();
    }

    @Override
    @JsonIgnore
    public Payload getPayload() {
        return super.getPayload();
    }

    public String getResource() {
        return resource;
    }

    public void setResource(final String resource) {
        this.resource = resource;
    }
}
