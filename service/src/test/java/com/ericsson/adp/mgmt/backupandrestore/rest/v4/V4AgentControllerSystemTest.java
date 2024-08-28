/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.backupandrestore.rest.v4;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V4_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V4_DEFAULT_BACKUP_MANAGER;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V4_DEFAULT_BACKUP_MANAGER_AGENTS;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.rest.DisableMimeSniffingFilter;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestWithAgents;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class V4AgentControllerSystemTest extends SystemTestWithAgents {

    @Test
    public void getAgents_registeredAgents() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        createTestAgent("newTestAgent", BackupManager.DEFAULT_BACKUP_MANAGER_ID, API_V3_0);

        final ResponseEntity<List<String>> responseEntity=
                restTemplate.exchange(V4_DEFAULT_BACKUP_MANAGER + "agents", HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>() {});

        final List<String> response1 = responseEntity.getBody();

        Stream<String> response = response1.stream();
        assertFalse(response.findAny().isEmpty());
        response = response1.stream();
        assertEquals("newTestAgent", response.findFirst().get());

        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getAgents_noRegisteredAgent() throws Exception {
        final ResponseEntity<List<String>> responseEntity=
                restTemplate.exchange(V4_DEFAULT_BACKUP_MANAGER + "agents", HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>() {});

        final List<String> response = responseEntity.getBody();

        assertTrue(response.isEmpty());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getAgents_inexistentBackupManager_notFound() {
        final ResponseEntity<JsonNode> responseEntity = restTemplate
                .getForEntity(V4_BASE_URL + "inexistentBackupManager/agents", JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                    responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }
}

