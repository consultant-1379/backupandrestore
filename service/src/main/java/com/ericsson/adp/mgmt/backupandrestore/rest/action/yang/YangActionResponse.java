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
package com.ericsson.adp.mgmt.backupandrestore.rest.action.yang;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON response with the result of an action creation from a Yang Action.
 */
public class YangActionResponse {

    @JsonProperty("ericsson-brm:return-value")
    private int actionId;

    /**
     * Default constructor, to be used by Jackson.
     */
    public YangActionResponse() {}

    /**
     * Creates CreateActionResponse based on actionId.
     * @param actionId to be returned.
     */
    public YangActionResponse(final String actionId) {
        this.actionId = Integer.valueOf(actionId);
    }

    public int getActionId() {
        return actionId;
    }

    public void setActionId(final int actionId) {
        this.actionId = actionId;
    }

}
