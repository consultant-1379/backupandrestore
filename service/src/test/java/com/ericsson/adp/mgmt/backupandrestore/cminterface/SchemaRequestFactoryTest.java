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
package com.ericsson.adp.mgmt.backupandrestore.cminterface;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.SchemaRequest;

public class SchemaRequestFactoryTest {

    private static final String SCHEMA_NAME = "ericsson-brm";
    private static final String SCHEMA_FILE = "ericsson-brm.json";
    private static final String SCHEMA_YANG_FILE = "brm-yangmodels.tar.gz";
    private static final String BRO_SERVICE_NAME = "localhost";

    private SchemaRequestFactory schemaRequestFactory;

    @Before
    public void init() {
        schemaRequestFactory = new SchemaRequestFactory();
        schemaRequestFactory.setBroPort("7001");
        schemaRequestFactory.setTlsPort("7002");
    }

    @Test
    public void getRequestToCreateSchema_tlsDisabled_requestWithHttpEntityWithNecessaryFilesAndInformation() {
        schemaRequestFactory.setBroServiceName(BRO_SERVICE_NAME);

        final SchemaRequest request = schemaRequestFactory.getRequestToCreateSchema();
        assertEquals(SCHEMA_NAME, request.getName());

        final HttpEntity<MultiValueMap<String, Object>> httpEntity = request.getHttpEntity();

        assertEquals(MediaType.MULTIPART_FORM_DATA, httpEntity.getHeaders().getContentType());
        assertEquals(SCHEMA_NAME, httpEntity.getBody().getFirst("name"));
        assertEquals(SCHEMA_NAME, httpEntity.getBody().getFirst("title"));

        final ClassPathResource file = (ClassPathResource) httpEntity.getBody().getFirst("file");
        assertEquals(SCHEMA_FILE, file.getFilename());

        final ClassPathResource yangArchive = (ClassPathResource) httpEntity.getBody().getFirst("yangArchive");
        assertEquals(SCHEMA_YANG_FILE, yangArchive.getFilename());

        assertEquals("http://localhost:7001/v2", httpEntity.getBody().getFirst("actions"));
    }

    @Test
    public void getRequestToCreateSchema_tlsEnabled_requestWithHttpEntityWithNecessaryFilesAndInformation() {
        schemaRequestFactory.setGlobalTlsEnabled(true);
        schemaRequestFactory.setBroServiceName(BRO_SERVICE_NAME);

        final SchemaRequest request = schemaRequestFactory.getRequestToCreateSchema();
        assertEquals(SCHEMA_NAME, request.getName());

        final HttpEntity<MultiValueMap<String, Object>> httpEntity = request.getHttpEntity();

        assertEquals(MediaType.MULTIPART_FORM_DATA, httpEntity.getHeaders().getContentType());
        assertEquals(SCHEMA_NAME, httpEntity.getBody().getFirst("name"));
        assertEquals(SCHEMA_NAME, httpEntity.getBody().getFirst("title"));

        final ClassPathResource file = (ClassPathResource) httpEntity.getBody().getFirst("file");
        assertEquals(SCHEMA_FILE, file.getFilename());

        final ClassPathResource yangArchive = (ClassPathResource) httpEntity.getBody().getFirst("yangArchive");
        assertEquals(SCHEMA_YANG_FILE, yangArchive.getFilename());

        assertEquals("https://localhost:7002/v2", httpEntity.getBody().getFirst("actions"));
    }

}