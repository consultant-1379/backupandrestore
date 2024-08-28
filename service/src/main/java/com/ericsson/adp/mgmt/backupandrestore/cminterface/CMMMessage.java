/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2022
 * <p>
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */
package com.ericsson.adp.mgmt.backupandrestore.cminterface;

import java.util.Optional;
import java.util.function.BiFunction;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.ConfigurationPatch;

/**
 * It represents all the message that BRO sends to CMM server
 */
public class CMMMessage {
    private final String resource;
    private final ConfigurationPatch configurationPatch;
    private final HttpMethod httpMethod;
    private final BiFunction<CMMMessage, Exception, Optional<CMMMessage>> fallback;

    private int retry;
    private HttpEntity httpEntity;

    /**
     * The constructor of CMM message
     * @param resource the resource to be manipulated
     * @param httpEntity the input as httpEntity.
     * @param patch the input as configuration patch
     * @param httpMethod the http method
     */
    public CMMMessage(final String resource, final HttpEntity httpEntity,
                      final ConfigurationPatch patch, final HttpMethod httpMethod) {
        this(resource, httpEntity, patch, httpMethod, 0);
    }

    /**
     * The constructor of CMM message
     * @param resource the resource to be manipulated
     * @param httpEntity the input as httpEntity.
     * @param patch the input as configuration patch
     * @param httpMethod the http method
     * @param retry number of times to retry sending message in the case of a failure
     */
    public CMMMessage(final String resource, final HttpEntity httpEntity,
                      final ConfigurationPatch patch, final HttpMethod httpMethod, final int retry) {
        this(resource, httpEntity, patch, httpMethod, (m, e) -> Optional.empty(), retry);
    }

    /**
     * The constructor of CMM message
     * @param resource the resource to be manipulated
     * @param httpEntity the input as httpEntity.
     * @param patch the input as configuration patch
     * @param httpMethod the http method
     * @param fallback fallback method when message fails
     * @param retry number of times to retry sending message in the case of a failure
     */
    public CMMMessage(final String resource, final HttpEntity httpEntity,
                      final ConfigurationPatch patch, final HttpMethod httpMethod, final BiFunction fallback,
                      final int retry) {
        this.resource = resource;
        this.httpEntity = httpEntity;
        configurationPatch = patch;
        this.httpMethod = httpMethod;
        this.fallback = fallback;
        this.retry = retry;
    }

    public BiFunction<CMMMessage, Exception, Optional<CMMMessage>> getFallback() {
        return fallback;
    }

    public int getRetry() {
        return retry;
    }

    public int getRetryAndDecrement() {
        return retry--;
    }

    public void setRetry(final int retry) {
        this.retry = retry;
    }

    public HttpEntity getHttpEntity() {
        return httpEntity;
    }

    public ConfigurationPatch getConfigurationPatch() {
        return configurationPatch;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public String getResource() {
        return resource;
    }

    public void setHttpEntity(final HttpEntity httpEntity) {
        this.httpEntity = httpEntity;
    }

    @Override
    public String toString() {
        return "CMMMessage [resource=" + resource +
                ", httpEntity=" + Optional.ofNullable(httpEntity).map(httpentity -> httpentity.toString()).orElse("null")
                + ", configurationPatch="
                + Optional.ofNullable(configurationPatch).map(patch -> patch.toString()).orElse("null")
                + ", httpMethod=" + httpMethod + ", fallback=" + fallback + ", retry=" + retry + "]";
    }
}
