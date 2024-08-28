/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.cminterface;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Requested parameters for either a configuration or schema subscription
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SubscriptionRequest {

    @JsonProperty("id")
    private String idSubscription;
    @JsonProperty("event")
    private String[] event;
    @JsonProperty("callback")
    private String callback;
    @JsonProperty("updateNotificationFormat")
    private String updateNotificationFormat;
    @JsonProperty("leaseSeconds")
    private long leaseSeconds = 1;

    /**
     * Default constructor required to deserialized
     */
    public SubscriptionRequest() {
        super();
    }
    /**
     * Constructor to create a basic subscription
     * @param idSubscription Subscription identifier
     * @param event Events to be subscribed
     * @param callback handler for each update received.
     */
    public SubscriptionRequest(final String idSubscription, final String[] event, final String callback) {
        super();
        this.idSubscription = idSubscription;
        this.event = event;
        this.callback = callback;
    }
    @JsonProperty("updateNotificationFormat")
    protected String getUpdateNotificationFormat() {
        return updateNotificationFormat;
    }
    @JsonProperty("updateNotificationFormat")
    protected void setUpdateNotificationFormat(final String updateNotificationFormat) {
        this.updateNotificationFormat = updateNotificationFormat;
    }

    @JsonProperty("id")
    protected String getIdSubscription() {
        return idSubscription;
    }

    @JsonProperty("id")
    protected void setIdSubscription(final String idSubscription) {
        this.idSubscription = idSubscription;
    }

    @JsonProperty("event")
    protected String[] getEvent() {
        return event;
    }
    @JsonProperty("event")
    protected void setEvent(final String[] event) {
        this.event = event;
    }

    @JsonProperty("callback")
    protected String getCallback() {
        return callback;
    }

    @JsonProperty("callback")
    protected void setCallback(final String callback) {
        this.callback = callback;
    }

    @JsonProperty("leaseSeconds")
    protected long getLeaseSeconds() {
        return leaseSeconds;
    }

    @JsonProperty("leaseSeconds")
    protected void setLeaseSeconds(final long leaseSeconds) {
        this.leaseSeconds = leaseSeconds;
    }

}
