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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contains the specific properties for configuration subscription
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ConfigurationRequest extends SubscriptionRequest {

    @JsonProperty("configName")
    private String configName;
    @JsonProperty("configNames")
    private String[] configNames;

    /**
     * Default constructor required to deserialize
     */
    public ConfigurationRequest() {
        super();
    }

    /**
     * Creates a configuration request with the required values
     * @param idSubscription Id for the subscription request
     * @param event Kind of event to be subscribed
     * @param callback handler for each update received
     */
    public ConfigurationRequest(final String idSubscription, final String[] event, final String callback) {
        super(idSubscription, event, callback);
        configNames = new String[0];
    }

    @JsonProperty("configName")
    protected String getConfigName() {
        return configName;
    }

    @JsonProperty("configName")
    protected void setConfigName(final String configName) {
        this.configName = configName;
    }

    @JsonProperty("configNames")
    protected String[] getConfigNames() {
        return configNames;
    }

    @JsonProperty("configNames")
    protected void setConfigNames(final String[] configNames) {
        this.configNames = configNames;
    }
}
