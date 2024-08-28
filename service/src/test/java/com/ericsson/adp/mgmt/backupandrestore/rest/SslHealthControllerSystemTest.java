/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;

import com.ericsson.adp.mgmt.backupandrestore.rest.health.HealthResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SslIntegrationTest;

public class SslHealthControllerSystemTest extends SslIntegrationTest {

    private final String HEALTH_URL = BASE_URL + "/v1/health";
    private final String UNTRUSTED_HEALTH_URL = "http://localhost:7001/v1/health";

//    ADPPRG-171470 - Test failing as .key .crt in created in genCerts all created in one folder and cert select will be first .key found
//    needs to be cleaned up
//    @Test
//    public void trustedTest() {
//        ResponseEntity<HealthResponse> responseEntity = trusted.getForEntity(HEALTH_URL, HealthResponse.class);
//        HealthResponse healthResponse = responseEntity.getBody();
//        assertEquals("Healthy", healthResponse.getStatus());
//        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
//                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
//    }

    @Test
    public void untrustedTest() {
        ResponseEntity<HealthResponse> responseEntity = untrusted.getForEntity(UNTRUSTED_HEALTH_URL, HealthResponse.class);
        HealthResponse healthResponse = responseEntity.getBody();
        assertEquals("Healthy", healthResponse.getStatus());
    }

}
