/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.backupandrestore.cminterface;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;


/**
 * Factory class for CM Schemas.
 */
@Service
public class SchemaRequestFactory extends MediatorRequestFactory<MultiValueMap<String, Object>> {

    private static final String SCHEMA_NAME = "ericsson-brm";

    /**
     * Build a new HTTP request for configuration
     * @return HTTP Entity with Configuration submit
     */
    @Override
    protected HttpEntity<MultiValueMap<String, Object>> buildHttpEntity() {
        final HttpHeaders headers = new HttpHeaders();
        final StringBuilder urlPath = new StringBuilder();
        final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        UriComponentsBuilder.newInstance();

        body.add("name", SCHEMA_NAME);
        body.add("title", SCHEMA_NAME);
        body.add("file", new ClassPathResource("ericsson-brm.json"));
        body.add("yangArchive", new ClassPathResource("brm-yangmodels.tar.gz"));

        if (globalTlsEnabled) {
            urlPath.append("https://")
            .append(broServiceName)
            .append(":").append(getBroTlsPort());
        } else {
            urlPath.append("http://")
            .append(broServiceName)
            .append(":").append(broPort);
        }
        urlPath.append("/v2");
        body.add("actions", urlPath.toString());
        return new HttpEntity<>(body, headers);
    }

}
