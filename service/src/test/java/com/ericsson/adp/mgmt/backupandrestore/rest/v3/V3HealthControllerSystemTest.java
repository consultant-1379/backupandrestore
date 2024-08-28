/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.backupandrestore.rest.v3;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_DEFAULT_BACKUP_MANAGER;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V3_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.rest.DisableMimeSniffingFilter;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.health.HealthResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestAgent;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestWithAgents;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public class V3HealthControllerSystemTest extends SystemTestWithAgents {

    private static final Logger log = LogManager.getLogger(V3HealthControllerSystemTest.class);

    @Test
    public void getHealth_getRequest_getsHealth() throws Exception {
        final ResponseEntity<HealthResponse> responseEntity=
                restTemplate.getForEntity(V3_BASE_URL + "health", HealthResponse.class);

        final HealthResponse response = responseEntity.getBody();
        log.info("response.toString() = {}", response.toString());
        log.info("response.getRegisteredAgent() = {}", response.getRegisteredAgents());

        assertEquals("Healthy", response.getStatus());
        assertEquals("Available", response.getAvailability());
        assertTrue(response.getOngoingAction().isEmpty());
        assertTrue(response.getRegisteredAgents().isEmpty());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getHealth_getRequest_getsHealthWhileOngoingAction() {
        final SystemTestAgent agent = createTestAgent("busyTestAgent", API_V3_0);
        final CreateActionRequest request = getRandomBackupMessage();

        final String actionId = restTemplate
                .postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request,
                        CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);

        final ResponseEntity<HealthResponse> responseEntity =
                restTemplate.getForEntity(V3_BASE_URL + "health", HealthResponse.class);
        final HealthResponse response = responseEntity.getBody();
        final Map<String,String> ongoingAction = response.getOngoingAction();

        assertEquals("Healthy", response.getStatus());
        assertEquals("Busy", response.getAvailability());
        assertFalse(ongoingAction.isEmpty());
        assertEquals("DEFAULT",ongoingAction.get("backupManagerId"));
        assertFalse(response.getRegisteredAgents().isEmpty());
        assertEquals(actionId,ongoingAction.get("actionId"));
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));

        agent.sendStageCompleteMessage(Action.BACKUP, false);
        agent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);
        agent.sendStageCompleteMessage(Action.BACKUP, true);

        waitForActionToFinish(actionId);
    }
}

