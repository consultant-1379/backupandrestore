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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import org.springframework.http.HttpEntity;

/**
 * Encapsulates a schema request to CM Mediator.
 * @param <T> Either the Multivalue request or Json request
 */
public class SchemaRequest<T> {

    private final String name;
    private final HttpEntity<T> httpEntity;

    /**
     * Creates request.
     * @param name schema's name.
     * @param httpEntity containing all the data to create a schema.
     */
    public SchemaRequest(final String name, final HttpEntity<T> httpEntity) {
        this.name = name;
        this.httpEntity = httpEntity;
    }

    public String getName() {
        return name;
    }

    public HttpEntity<T> getHttpEntity() {
        return httpEntity;
    }

}
