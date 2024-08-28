/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext;

import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HypertextControlTest {

    private HypertextControl control;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        // initialize ObjectMapper
        objectMapper = new ObjectMapper();
        control = new HypertextControl("/my/resource", HttpMethod.GET);
    }

    @Test
    public void testGetHref() {
        Assert.assertEquals("/my/resource", control.getHref());
    }

    @Test
    public void testGetMethod() {
        Assert.assertEquals(HttpMethod.GET, control.getMethod());
    }
    @Test
    public void testHypertextControlSerialization() throws Exception {
        HypertextControl control = new HypertextControl("/test/href", HttpMethod.POST);

        // Serialize the object to JSON
        String jsonResult = objectMapper.writeValueAsString(control);

        // validate the results of Json
        assertTrue(jsonResult.contains("\"href\":\"/test/href\""));
        assertTrue(jsonResult.contains("\"method\":\"POST\""));
    }
}

