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
 * Holds Action's information.
 */
public class ActionInformation {
    protected String actionId;
    protected ActionType name;
    protected ResultType result;
    protected Payload payload;
    protected String additionalInfo;
    protected String progressInfo;
    protected String resultInfo;
    protected ActionStateType state;

    public String getActionId() {
        return actionId;
    }

    public ActionType getName() {
        return name;
    }

    public ResultType getResult() {
        return result;
    }

    public void setResult(final ResultType result) {
        this.result = result;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(final Payload  payload) {
        this.payload = payload;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(final String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public String getProgressInfo() {
        return progressInfo;
    }

    public void setProgressInfo(final String progressInfo) {
        this.progressInfo = progressInfo;
    }

    public String getResultInfo() {
        return resultInfo;
    }

    public void setResultInfo(final String resultInfo) {
        this.resultInfo = resultInfo;
    }


    public ActionStateType getState() {
        return state;
    }

    public void setState(final ActionStateType state) {
        this.state = state;
    }
}
