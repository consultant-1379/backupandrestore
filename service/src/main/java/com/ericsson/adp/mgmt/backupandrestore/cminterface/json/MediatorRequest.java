/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mediator request sending configuration changes
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MediatorRequest {

    @JsonProperty("configName")
    private String configName;
    @JsonProperty("event")
    private String event;
    @JsonProperty("configETag")
    private String configETag;
    @JsonProperty("baseETag")
    private String baseETag;
    @JsonProperty("patch")
    private List<MediatorRequestPatch> patch = Collections.emptyList();
    @JsonProperty("notifCreateTime")
    private String notifCreateTime;
    @JsonProperty("notifId")
    private Integer notifId;
    @JsonProperty("changedBy")
    private String changedBy;

    @JsonProperty("configName")
    public String getConfigName() {
        return configName;
    }

    @JsonProperty("configName")
    public void setConfigName(final String configName) {
        this.configName = configName;
    }

    @JsonProperty("changedBy")
    public String getChangedBy() {
        return changedBy;
    }

    @JsonProperty("changedBy")
    public void setChangedBy(final String changedBy) {
        this.changedBy = changedBy;
    }
    @JsonProperty("event")
    public String getEvent() {
        return event;
    }

    @JsonProperty("event")
    public void setEvent(final String event) {
        this.event = event;
    }

    @JsonProperty("configETag")
    public String getConfigETag() {
        return configETag;
    }

    @JsonProperty("configETag")
    public void setConfigETag(final String configETag) {
        this.configETag = configETag;
    }

    @JsonProperty("baseETag")
    public String getBaseETag() {
        return baseETag;
    }

    @JsonProperty("baseETag")
    public void setBaseETag(final String baseETag) {
        this.baseETag = baseETag;
    }

    @JsonProperty("patch")
    public List<MediatorRequestPatch> getPatch() {
        return patch;
    }

    @JsonProperty("patch")
    public void setPatch(final List<MediatorRequestPatch> patch) {
        this.patch = patch;
    }

    @JsonProperty("notifCreateTime")
    public String getNotifCreateTime() {
        return notifCreateTime;
    }

    @JsonProperty("notifCreateTime")
    public void setNotifCreateTime(final String notifCreateTime) {
        this.notifCreateTime = notifCreateTime;
    }

    @JsonProperty("notifId")
    public Integer getNotifId() {
        return notifId;
    }

    @JsonProperty("notifId")
    public void setNotifId(final Integer notifId) {
        this.notifId = notifId;
    }

    @Override
    public String toString() {
        return "CMMediator_Notification [configName=" + configName + ", event=" + event + ", configETag="
                + configETag + ", baseETag=" + baseETag + ", patch="
                + patch + ", notifCreateTime=" + notifCreateTime + ", notifId=" + notifId + ", changedBy=" + changedBy + "]";
    }
}
