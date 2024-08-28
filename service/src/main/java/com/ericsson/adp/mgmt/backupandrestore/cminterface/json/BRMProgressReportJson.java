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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import java.util.Objects;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.ActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents a progress report in the BRM model.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BRMProgressReportJson extends ActionResponse {

    private String cmRepresentationOfResult;
    private String cmRepresentationOfState;

    /**
     * Default constructor, to be used by Jackson.
     */
    public BRMProgressReportJson() {}

    /**
     * Creates ProgressReport from Action.
     * @param action to be reported.
     */
    public BRMProgressReportJson(final Action action) {
        actionId = action.getActionId();
        setName(action.getName());
        setAdditionalInfo(action.getAdditionalInfo());
        setProgressInfo(action.getProgressInfo());
        setProgressPercentage(action.getProgressPercentage());
        cmRepresentationOfResult = action.getResult().getCmRepresentation();
        cmRepresentationOfState = action.getState().getCmRepresentation();
        setStartTime(DateTimeUtils.convertToString(action.getStartTime()));
        setLastUpdateTime(DateTimeUtils.convertToString(action.getLastUpdateTime()));
        if (action.getCompletionTime() != null) {
            setCompletionTime(DateTimeUtils.convertToString(action.getCompletionTime()));
        }
        if (action.hasMessages()) {
            setResultInfo(action.getAllMessagesAsSingleString());
        } else {
            setResultInfo(action.getResultInfo());
        }
    }

    @Override
    @JsonIgnore
    public String getActionId() {
        return super.getActionId();
    }

    @Override
    @JsonProperty("action-id")
    public void setActionId(final String actionId) {
        this.actionId = actionId;
    }

    @JsonProperty("action-id")
    public Integer getId() {
        return Integer.valueOf(getActionId());
    }

    @Override
    @JsonProperty("action-name")
    public ActionType getName() {
        return super.getName();
    }

    @Override
    @JsonIgnore
    public String getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    /**
     * Returns additionalInfo as an array of Strings.
     * @return additionalInfo as an array of Strings.
     */
    @JsonProperty("additional-info")
    @JsonInclude(Include.NON_NULL)
    public String[] getAdditionalInfoAsArray() {
        return getAdditionalInfo() != null ? new String[] { getAdditionalInfo() } : null ;
    }

    @Override
    @JsonProperty("progress-info")
    public String getProgressInfo() {
        return super.getProgressInfo();
    }

    @Override
    @JsonIgnore
    public Double getProgressPercentage() {
        return super.getProgressPercentage();
    }

    @JsonProperty("progress-percentage")
    public int getProgressPercentageAsInteger() {
        return (int) (super.getProgressPercentage() * 100);
    }

    @Override
    @JsonIgnore
    public ResultType getResult() {
        return super.getResult();
    }

    @JsonProperty("result")
    public String getCmRepresentationOfResult() {
        return cmRepresentationOfResult;
    }

    @Override
    @JsonProperty("result-info")
    public String getResultInfo() {
        return super.getResultInfo();
    }

    @Override
    @JsonIgnore
    public ActionStateType getState() {
        return super.getState();
    }

    @JsonProperty("state")
    public String getCmRepresentationOfState() {
        return cmRepresentationOfState;
    }

    @Override
    @JsonProperty("time-action-completed")
    public String getCompletionTime() {
        return super.getCompletionTime();
    }

    @Override
    @JsonProperty("time-action-started")
    public String getStartTime() {
        return super.getStartTime();
    }

    @Override
    @JsonProperty("time-of-last-status-update")
    public String getLastUpdateTime() {
        return super.getLastUpdateTime();
    }

    public void setCmRepresentationOfResult(final String cmRepresentationOfResult) {
        this.cmRepresentationOfResult = cmRepresentationOfResult;
    }

    public void setCmRepresentationOfState(final String cmRepresentationOfState) {
        this.cmRepresentationOfState = cmRepresentationOfState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cmRepresentationOfResult,
                            cmRepresentationOfState,
                            progressPercentage,
                            startTime,
                            completionTime,
                            lastUpdateTime,
                            actionId,
                            name,
                            result,
                            additionalInfo,
                            progressInfo,
                            resultInfo,
                            state);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BRMProgressReportJson other = (BRMProgressReportJson) obj;
        return Objects.equals(this.toString(), other.toString());
    }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder("BRMProgressReportJson{");
        stringBuilder.append("cmRepresentationOfResult='").append(cmRepresentationOfResult).append('\'');
        stringBuilder.append(", cmRepresentationOfState='").append(cmRepresentationOfState).append('\'');
        stringBuilder.append(", progressPercentage=").append(progressPercentage);
        stringBuilder.append(", startTime='").append(startTime).append('\'');
        stringBuilder.append(", completionTime='").append(completionTime).append('\'');
        stringBuilder.append(", lastUpdateTime='").append(lastUpdateTime).append('\'');
        stringBuilder.append(", actionId='").append(actionId).append('\'');
        stringBuilder.append(", name=").append(name);
        stringBuilder.append(", result=").append(result);
        stringBuilder.append(", additionalInfo='").append(additionalInfo).append('\'');
        stringBuilder.append(", progressInfo='").append(progressInfo).append('\'');
        stringBuilder.append(", resultInfo='").append(resultInfo).append('\'');
        stringBuilder.append(", state=").append(state);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
