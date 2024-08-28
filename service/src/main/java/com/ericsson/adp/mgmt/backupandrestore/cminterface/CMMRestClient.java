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


import static com.ericsson.adp.mgmt.backupandrestore.util.RestTemplateFactory.Security.MTLS;
import static com.ericsson.adp.mgmt.backupandrestore.util.RestTemplateFactory.Security.NONE;

import java.util.Optional;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.ericsson.adp.mgmt.backupandrestore.caching.Cached;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchRequest;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.ConfigurationPatch;
import com.ericsson.adp.mgmt.backupandrestore.util.RestTemplateFactory;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * The old CMMClient contains CMM Logic and RestClient functionality. So It's better to separate the basic RestClient
 * functionality out (single responsibility principle), and it's good to do the rest-action retry.
 * This class is responsible to send the rest action to the CMM server.
 */
@Service
public class CMMRestClient {
    private static final String REST_TEMPLATE_ID = "CMM";
    private static final Logger log = LogManager.getLogger(CMMRestClient.class);
    // use regex to validate if a pwd is valid
    private static final String REGEX_VALID = "'(.*)'\\s";
    private String cmUrl;
    private Cached<RestTemplate> templateCache;
    private EtagNotifIdBase notifIdBase;

    /**
     * Makes sure the password is hidden
     *
     * @param errorMessage the error message with the password present
     * @return the error message to be logged after password was hidden
     */
    protected static String hidePassword(final String errorMessage) {
        if (errorMessage.contains("is not a 'eric-adp-cm-secret'")) {
            log.debug("Request failed with response <{}>", errorMessage);
            return (errorMessage.replaceFirst(REGEX_VALID, "'supplied information' "));
        } else {
            return errorMessage;
        }
    }

    /**
     * Gets from CM using exchange to recover header elements
     *
     * @param resource to get
     * @return ResourceEtag including body and etag
     */
    public String exchange(final String resource) {
        log.info("GET CMM resource using exchange: <{}>", resource);
        execute(() -> {
            final ResponseEntity<String> response = getRestTemplate().exchange(
                    getUrl(resource),
                    HttpMethod.GET,
                    null,
                    String.class);
            notifIdBase.setConfiguration(response.getBody().toString());
            notifIdBase.updateEtag(response.getHeaders().getETag());
            log.debug("Get response: <{}> - <{}> - etag:<{}>", response.getStatusCode(),
                    notifIdBase.getConfiguration(), notifIdBase.getEtag());
        });
        return notifIdBase.getConfiguration();
    }

    /**
     * Gets from CM
     *
     * @param resource to get
     * @return String with result output
     */
    public String get(final String resource) {
        log.info("GET CMM resource: <{}>", resource);
        return execute(() -> getRestTemplate().getForObject(getUrl(resource), String.class));
    }

    /**
     * Posts to CM.
     *
     * @param resource      to be posted to.
     * @param requestEntity request entity to be sent.
     */
    public void post(final String resource, final HttpEntity<?> requestEntity) {
        log.info("Post CMM resource: <{}>", resource);
        log.debug("Post CMM resource Body: <{}>", requestEntity);
        execute(() -> {
            final String response = getRestTemplate().postForObject(getUrl(resource), requestEntity, String.class);
            log.debug("Post response: <{}>", response);
        });
    }

    /**
     * Retrieve the ConfigEtag from a CMM URL Resource
     * @param configResource URL Path to CMMediator
     * @return Optional String if ConfigETag is found
     */
    public Optional<String> retrieveConfigETag(final String configResource) {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getUrl(configResource));
        final ResponseEntity<JsonNode> response = getRestTemplate().getForEntity(builder.toUriString(), JsonNode.class);

        final Optional<JsonNode> body = Optional.ofNullable(response.getBody());
        if (body.isPresent()) {
            notifIdBase.setConfiguration(body.get().toString());
        }
        notifIdBase.updateEtag(response.getHeaders().getFirst("ETag"));

        log.debug("Get etag body: <{}>", notifIdBase.getConfiguration());
        return Optional.of(response.getHeaders().getFirst("ETag"));
    }

    /**
     * Posts to CM.
     * @param resource      to be posted to.
     * @param requestEntity request entity to be sent.
     */
    public void put(final String resource, final HttpEntity<?> requestEntity) {
        log.info("Put CMM resource: <{}>", resource);
        log.debug("Put CMM resource Body: <{}>", requestEntity);
        execute(() -> getRestTemplate().put(getUrl(resource), requestEntity, String.class));
    }

    /**
     * Patches CM.
     *
     * @param resource to be patched.
     * @param patch    what to patch.
     */
    public void patch(final String resource, final ConfigurationPatch patch) {
        log.info("Patch CMM resource: <{}>", resource);
        log.debug("Patch CMM resource Body: {}", patch);
        execute(() -> {
            final String response = getRestTemplate().patchForObject(getUrl(resource), patch.toJson(), String.class);
            log.debug("Patch response: <{}>", response);
        });
    }

    /**
     * Patches CM.
     *
     * @param resource to be patched.
     * @param patch    what to patch.
     * @param httpEntity customized headers is need it
     */
    public void exchange(final String resource, final ConfigurationPatch patch, final HttpEntity httpEntity) {
        log.info("Exchange CMM resource: <{}>", resource);
        log.debug("Exchange CMM resource Body: {}", patch);
        final HttpHeaders headers = new HttpHeaders();
        headers.addAll(httpEntity.getHeaders());
        final HttpEntity<PatchRequest> newEntity = new HttpEntity<PatchRequest>(patch.toJson(), headers);
        execute(() -> {
            final ResponseEntity<String> response  = getRestTemplate().exchange(
                    getUrl(resource),
                    HttpMethod.PATCH,
                    newEntity,
                    String.class);
            log.debug("Patch response: <{}> - <{}>", response.getStatusCode(),
                    response.getBody());
        });
    }

    /**
     * Deletes a resource in CM.
     *
     * @param resource to be deleted.
     */
    public void delete(final String resource) {
        log.info("Delete CMM resource: <{}>", resource);
        execute(() -> getRestTemplate().delete(getUrl(resource)));
    }

    private void execute(final Runnable operation) {
        execute(() -> {
            operation.run();
            return "";
        });
    }

    private String execute(final Supplier<String> operation) {
        try {
            return operation.get();
        } catch (final HttpClientErrorException e) {
            log.error("Request failed with response <{}>, due to HttpClientErrorException", e.getStatusCode());
            throw e;
        } catch (final Exception e) {
            log.error("Request failed with response <{}>, due to <{}>", hidePassword(e.getMessage()), e.getClass());
            throw e;
        }
    }

    public RestTemplate getRestTemplate() {
        return templateCache.get();
    }

    private String getUrl(final String resource) {
        return cmUrl + "/" + resource;
    }

    @Autowired
    public void setCmUrl(final String cmUrl) {
        this.cmUrl = cmUrl;
    }

    /**
     * Setup the rest template used to talk to CMM
     *
     * @param restTemplateFactory - the template factory to use
     * @param tlsEnabled          - true if TLS is enabled, else false
     */
    @Autowired
    public void setRestTemplateConfiguration(final RestTemplateFactory restTemplateFactory, @Value("${global.tls:true}") final boolean tlsEnabled) {
        templateCache = restTemplateFactory.getRestTemplate(REST_TEMPLATE_ID, tlsEnabled ? MTLS : NONE);
    }

    /**
     * Setup the NotifIdbase to keep the value of etag and last configuration
     *
     * @param notifIdBase - keep the last Etag, notification and configuration from Mediator
     */
    @Autowired
    public void setEtagNotifidBase(final EtagNotifIdBase notifIdBase) {
        this.notifIdBase = notifIdBase;
    }
}
