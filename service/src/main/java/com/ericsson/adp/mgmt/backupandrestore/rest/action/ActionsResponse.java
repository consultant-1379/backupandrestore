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

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON response of all actions of a backupManager
 */
public class ActionsResponse {

    private List<ActionResponse> actions = new ArrayList<>();

    /**
     * Default constructor, to be used by Jackson.
     */
    public ActionsResponse() {}

    /**
     * Creates ActionsResponse from list of actions.
     * @param actions to be returned
     */
    public ActionsResponse(final List<Action> actions) {
        for (final Action action : actions) {
            this.actions.add(action.toSimplifiedJson());
        }
    }

    public List<ActionResponse> getActions() {
        return actions;
    }

    public void setActions(final List<ActionResponse> actions) {
        this.actions = actions;
    }

}
