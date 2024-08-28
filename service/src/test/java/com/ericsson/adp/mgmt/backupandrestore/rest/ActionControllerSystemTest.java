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
package com.ericsson.adp.mgmt.backupandrestore.rest;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_DEFAULT_BACKUP_MANAGER;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V3_ACTION;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V3_BASE_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.ActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.ActionsResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

public class ActionControllerSystemTest extends SystemTest /*WithAgents*/ {

    private String actionId;

    @After
    public void teardown() {
        if (actionId != null) {
            waitForActionToFinish(actionId);
            actionId = null;
        }
    }

    @Test
    public void getActions_backupManagerId_getsAllActions() throws Exception {
        waitForActionToFinish(restTemplate.postForEntity(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", getRandomBackupMessage(), CreateActionResponse.class)
                .getBody().getActionId());
        waitForActionToFinish(restTemplate.postForEntity(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", getRandomBackupMessage(), CreateActionResponse.class)
                .getBody().getActionId());
        waitForActionToFinish(restTemplate.postForEntity(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", getRandomBackupMessage(), CreateActionResponse.class)
                .getBody().getActionId());

        final ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", JsonNode.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
        final ArrayNode actions = (ArrayNode) responseEntity.getBody().get("actions");

        assertTrue(actions.size() >= 3);

        //API V3 Test - should produce same result as V1 test.
        final ResponseEntity<JsonNode> responseEntityV3 = restTemplate.getForEntity(V3_ACTION.toString(), JsonNode.class);

        assertEquals(HttpStatus.OK, responseEntityV3.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
            responseEntityV3.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
        final ArrayNode actionsV3 = (ArrayNode) responseEntityV3.getBody().get("actions");

        assertTrue(actionsV3.size() >= 3);
    }

    @Test
    public void getActions_inexistingBackupManagerId_notFound() throws Exception {
        final ResponseEntity<ActionsResponse> responseEntity = restTemplate.getForEntity(V1_BASE_URL + "backup-manager/qwe/action",
                ActionsResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getAction_backupManagerIdAndActionId_getAction() throws Exception {
        actionId = restTemplate.postForEntity(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", getRandomBackupMessage(), CreateActionResponse.class).getBody()
                .getActionId();

        final ResponseEntity<ActionResponse> responseEntity = restTemplate.getForEntity((V1_DEFAULT_BACKUP_MANAGER.toString() + "action/") + actionId,
                ActionResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(actionId, responseEntity.getBody().getActionId());
        assertEquals(ActionType.CREATE_BACKUP, responseEntity.getBody().getName());
        assertTrue(responseEntity.getBody().getPayload() instanceof BackupNamePayload);
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getAction_inexistingActionId_notFound() throws Exception {
        final ResponseEntity<ActionResponse> responseEntity = restTemplate.getForEntity((V1_DEFAULT_BACKUP_MANAGER.toString() + "action/") + "notFound",
                ActionResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getAction_inexistingBackupManagerId_notFound() throws Exception {
        final ResponseEntity<ActionResponse> responseEntity = restTemplate.getForEntity(V1_BASE_URL + "backup-manager/qwe/action/notFound",
                ActionResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getActionV3_inexistingBackupManagerId_notFound() throws Exception {
        final ResponseEntity<ActionResponse> responseEntity = restTemplate.getForEntity(V3_BASE_URL + "backup-managers/qwe/actions/notFound",
            ActionResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
            responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getAction_actionWasStillRunningWhenSystemStarted_actionIsFinishedAndFailed() throws Exception {
        final ActionResponse action = restTemplate.getForObject(getScopedActionUrl(V1_BASE_URL, "backupManagerWithBackupToDelete") + "/runningAction",
                ActionResponse.class);

        assertEquals(ResultType.FAILURE, action.getResult());
        assertEquals(ActionStateType.FINISHED, action.getState());
    }

    @Test
    public void getActionV3_actionWasStillRunningWhenSystemStarted_actionIsFinishedAndFailed() throws Exception {
        final ActionResponse action = restTemplate.getForObject(getScopedActionUrl(V3_BASE_URL, "backupManagerWithBackupToDelete") + "/runningAction",
            ActionResponse.class);

        assertEquals(ResultType.FAILURE, action.getResult());
        assertEquals(ActionStateType.FINISHED, action.getState());
    }

    @Test
    public void createAction_badRequest_badRequest() throws Exception {
        final MultiValueMap<String, String> headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        final HttpEntity<String> requestEntity = new HttpEntity<>("null", headers);

        final ResponseEntity<String> responseEntity = restTemplate.exchange(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", HttpMethod.POST, requestEntity,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void createAction_noAction_unprocessableEntity() throws Exception {
        final CreateActionRequest request = new CreateActionRequest();

        final ResponseEntity<CreateActionResponse> responseEntity = restTemplate.postForEntity(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request,
                CreateActionResponse.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void createAction_noPayload_unprocessableEntity() throws Exception {
        final CreateActionRequest request = new CreateActionRequest();
        request.setAction(ActionType.CREATE_BACKUP);

        final ResponseEntity<CreateActionResponse> responseEntity = restTemplate.postForEntity(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request,
                CreateActionResponse.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void createAction_invalidAction_badRequest() throws Exception {
        final MultiValueMap<String, String> headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        final HttpEntity<String> requestEntity = new HttpEntity<>("{ \"action\": \"Q\" }", headers);

        final ResponseEntity<String> responseEntity = restTemplate.exchange(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", HttpMethod.POST, requestEntity,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

}
