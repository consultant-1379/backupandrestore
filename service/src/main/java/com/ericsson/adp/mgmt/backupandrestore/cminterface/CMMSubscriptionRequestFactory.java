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

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SCHEMA_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.URLMAPPING;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.SchemaRequest;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMEricssonbrmJson;

/**
 * Creates an configuration HTTP Entity for CMM
 * Define the subscription request sent to CMM to create/update a subscription
 */
@Service
public class CMMSubscriptionRequestFactory extends MediatorRequestFactory<String> {

    protected JsonService jsonService;
    protected long leasedSeconds;

    @Override
    protected HttpEntity<String> buildHttpEntity() {
        return getHttpEntity (false);
    }

    /**
     * Create an HttpEntity used on Mediator to create/update requests
     * @param isUpdate boolean value true update does not includes the id.
     * @return return the HttpEntity for create or update
     */
    protected HttpEntity<String> getHttpEntity(final boolean isUpdate) {
        final UriComponentsBuilder urlResponse;
        final ConfigurationRequest body;
        final HttpHeaders headers = new HttpHeaders();

        headers.setContentType(APPLICATION_JSON);
        urlResponse = UriComponentsBuilder.newInstance();

        if (isGlobalTlsEnabled()) {
            urlResponse.scheme("https");
            urlResponse.port(getTlsCMMNotifPort());
        } else {
            urlResponse.scheme("http");
            urlResponse.port(getBroPort());
        }

        urlResponse.host(getBroServiceName()).path("/v3/" + URLMAPPING);

        final String[] events = {"configUpdated"};
        body = new ConfigurationRequest(SCHEMA_NAME, events,
                urlResponse.toUriString());
        body.setConfigName(SCHEMA_NAME);
        body.setUpdateNotificationFormat("patch");
        body.setLeaseSeconds(getLeasedSeconds());
        if (isUpdate) {
            body.setIdSubscription(null);
        }
        return new HttpEntity<>(toJson(body), headers);
    }

    @Autowired
    public void setJsonService(final JsonService jsonService) {
        this.jsonService = jsonService;
    }

    /**
     * Get a request to be handled on Mediator
     * @return Request to be push to Mediator
     */
    protected SchemaRequest getRequestToUpdateSchema() {
        return new SchemaRequest(SCHEMA_NAME, getHttpEntity(true));
    }

    private String toJson(final ConfigurationRequest configRequest) {
        return jsonService.toJsonString(configRequest);
    }

    /**
     * Parser a Json String into a SubscriptionRequest object
     * @param jsonString representing the object
     * @return Optional SubscriptionRequest
     */
    protected Optional<ConfigurationRequest> parseJsonStringToSubscriptionRequest(final String jsonString) {
        return jsonService.parseJsonString(jsonString, ConfigurationRequest.class);
    }

    /**
     * Parser a Json String into a BRMEricssonbrmJson configuration
     * @param jsonString representing the object
     * @return Optional BRMEricssonbrmJson
     */
    public Optional<BRMEricssonbrmJson> parseJsonStringToBRMConfiguration(final String jsonString) {
        return jsonService.parseJsonString(jsonString, BRMEricssonbrmJson.class);
    }

    protected long getLeasedSeconds() {
        return leasedSeconds;
    }

    @Value("${cm.mediator.subscribe.leasedSeconds:157680000}")
    protected void setLeasedSeconds(final long leasedSeconds) {
        this.leasedSeconds = leasedSeconds;
    }

}
