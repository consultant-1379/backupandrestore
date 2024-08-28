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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON response with the result of an action creation.
 */
public class CreateActionResponse {

    @JsonProperty("id")
    private String actionId;

    /**
     * Default constructor, to be used by Jackson.
     */
    public CreateActionResponse() {}

    /**
     * Creates CreateActionResponse based on actionId.
     * @param actionId to be returned.
     */
    public CreateActionResponse(final String actionId) {
        this.actionId = actionId;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(final String actionId) {
        this.actionId = actionId;
    }

}
