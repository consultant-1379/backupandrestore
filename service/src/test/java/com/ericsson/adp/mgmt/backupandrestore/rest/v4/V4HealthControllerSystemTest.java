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

import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager.DEFAULT_BACKUP_MANAGER_ID;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.*;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static org.junit.Assert.*;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.rest.DisableMimeSniffingFilter;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.BackupManagerResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.BackupManagersResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.v4.V4BackupManagerResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.health.HealthResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestAgent;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestBackupFragment;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestWithAgents;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;

public class V4HealthControllerSystemTest extends SystemTestWithAgents {

    @Test
    public void getHealth_getRequest_getsHealth() throws Exception {
        final ResponseEntity<HealthResponse> responseEntity=
                restTemplate.getForEntity(V4_BASE_URL + "health", HealthResponse.class);
        final HealthResponse response = responseEntity.getBody();
        assertEquals("Healthy", response.getStatus());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getHealth_getRequest_getsHealthWhileOngoingBackupAction() {
        final SystemTestAgent agent = createTestAgent("TestAgent", API_V3_0);
        final CreateActionRequest request = getRandomBackupMessage();

        final String actionId = restTemplate
                .postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request,
                        CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        final ResponseEntity<HealthResponse> responseEntity =
                restTemplate.getForEntity(V4_BASE_URL + "health", HealthResponse.class);
        final ResponseEntity<V4BackupManagerResponse> resp = restTemplate.getForEntity(V4_BASE_URL + "backup-managers/DEFAULT", V4BackupManagerResponse.class);

        agent.sendStageCompleteMessage(Action.BACKUP, false);
        agent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);
        agent.sendStageCompleteMessage(Action.BACKUP, true);
        waitForActionToFinish(actionId);
        agent.shutdown();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(DEFAULT_BACKUP_MANAGER_ID, resp.getBody().getBackupManagerId());
        System.out.println(resp);
        final HealthResponse response = responseEntity.getBody();
        final V4BackupManagerResponse respon = resp.getBody();
        assertTrue(respon.getAvailableTasks().isEmpty());
        assertFalse(respon.getOngoingTasks().isEmpty());
        assertEquals("Healthy", response.getStatus());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));

    }

    @Test
    public void getHealth_getRequest_getsHealthWhileOngoingRestoreAction() {
        final SystemTestAgent agent = createTestAgent("firstAgentId", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("firstAgentId", backupId, "2", "backupFile", Arrays.asList("3"));

        backup(agent, request, backupId, fragment);
        agent.shutdown();

        // restore agent and perform restore
        final SystemTestAgent restoredAgent = createTestAgent("firstAgentId", API_V3_0);
        request.setAction(ActionType.RESTORE);
        final String restoreActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();

        waitUntil(restoredAgent::isParticipatingInAction);

        final ResponseEntity<HealthResponse> responseEntity =
                restTemplate.getForEntity(V4_BASE_URL + "health", HealthResponse.class);
        final ResponseEntity<V4BackupManagerResponse> resp = restTemplate.getForEntity(V4_BASE_URL + "backup-managers/DEFAULT", V4BackupManagerResponse.class);
        restoredAgent.downloadFragments();
        waitForActionToFinish(restoreActionId);
        restoredAgent.shutdown();

        assertActionIsSuccessfullyCompleted(restoreActionId);

        final HealthResponse response = responseEntity.getBody();
        assertEquals("Healthy", response.getStatus());
        final V4BackupManagerResponse respon = resp.getBody();
        assertTrue(respon.getAvailableTasks().isEmpty());
        assertFalse(respon.getOngoingTasks().isEmpty());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    private void backup(final SystemTestAgent agent, final CreateActionRequest request, final String backupId,
                        final SystemTestBackupFragment fragment) {
        final String backupActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();
        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);
        agent.sendStageCompleteMessage(Action.BACKUP, true);
        agent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);
        fragment.sendThroughAgent(agent);
        agent.closeDataChannel();

        agent.sendStageCompleteMessage(Action.BACKUP, true);
        agent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.POST_ACTIONS);
        agent.sendStageCompleteMessage(Action.BACKUP, true);
        waitForActionToFinish(backupActionId);
    }
}

