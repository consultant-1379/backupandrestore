/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.v4;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V4_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V4_DEFAULT_BACKUP_MANAGER;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V4_DEFAULT_BACKUP_MANAGER_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V4_IMPORTS;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Arrays;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.rest.DisableMimeSniffingFilter;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.V4ActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.V4CreateBackupRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.V4ImportExportRequest;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestAgent;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestBackupFragment;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestWithAgents;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class V4ActionControllerSystemTest extends SystemTestWithAgents {
    public static final URI SFTP_USER_LOCALHOST_222_REMOTE = URI.create("sftp://user@localhost:222/remote");
    private static final String BRM_WITH_NO_ACTION = "backupManagerWithSchedulerInfo";

    @Test
    public void getAllTasks_taskExists_taskFound() {
        final SystemTestAgent agent = createTestAgent("testAgent", API_V3_0);
        final CreateActionRequest request = getRandomBackupMessage();
        final BackupNamePayload payload = (BackupNamePayload) request.getPayload();
        final String backupId = payload.getBackupName();
        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("testAgent", backupId, "2", "backupFile", Arrays.asList("3"));
        backup(agent, backupId, fragment);
        final ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity(V4_DEFAULT_BACKUP_MANAGER + "tasks", JsonNode.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
        final ArrayNode tasks = (ArrayNode) responseEntity.getBody();
        assertTrue(tasks.size() >= 1);
        final JsonNode firstTask = tasks.get(0);

        // The tasks endpoint only returns the task "id", "name", "result" and "startTime"
        assertNotNull(firstTask.get("id"));
        assertNotNull(firstTask.get("name"));
        assertNotNull(firstTask.get("result"));
        assertNotNull(firstTask.get("startTime"));

        // The tasks endpoint does not return "payload", "resultInfo", "completionTime",
        // "lastUpdateTime", and "resource"
        assertNull(firstTask.get("payload"));
        assertNull(firstTask.get("resultInfo"));
        assertNull(firstTask.get("completionTime"));
        assertNull(firstTask.get("lastUpdateTime"));
        assertNull(firstTask.get("resource"));

        agent.shutdown();
    }

    @Test
    public void getTaskByBackupManagerAndTaskId_taskExists_taskFound() {
        final SystemTestAgent agent = createTestAgent("testAgent", API_V3_0);
        final CreateActionRequest request = getRandomBackupMessage();
        final BackupNamePayload payload = (BackupNamePayload) request.getPayload();
        final String backupId = payload.getBackupName();
        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("testAgent", backupId, "2", "backupFile", Arrays.asList("3"));
        final String taskId = backup(agent, backupId, fragment);
        final ResponseEntity<JsonNode> getResponse = restTemplate
                .getForEntity(V4_DEFAULT_BACKUP_MANAGER + "tasks/" + taskId, JsonNode.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE, getResponse.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
        final ObjectNode getResponseBody = (ObjectNode) getResponse.getBody();
        assertEquals("SUCCESS", getResponseBody.get("result").textValue());
        assertEquals("/backup-restore/v4/backup-managers/DEFAULT/backups/" + backupId +"/", getResponseBody.get("resource").textValue());
        assertNotNull(getResponseBody.get("resultInfo"));
        assertNotNull(getResponseBody.get("startTime"));
        assertNotNull(getResponseBody.get("completionTime"));
        assertNotNull(getResponseBody.get("lastUpdateTime"));
        // The tasks endpoint does not return "id", and "payload"
        assertNull(getResponseBody.get("id"));
        assertNull(getResponseBody.get("payload"));
        agent.shutdown();
    }

    @Test
    public void getAction_ByBackupManageBackupIdAndTaskId_taskDoesNotExist_NotFoundStatusCode() {
        final String inexistentTaskId = "999999";
        final ResponseEntity<JsonNode> getResponse = restTemplate
                .getForEntity(V4_DEFAULT_BACKUP_MANAGER + "tasks/" + inexistentTaskId, JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE, getResponse.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getLastTaskByBackupManager_actionExists_actionFound() {
        final SystemTestAgent agent = createTestAgent("lastTask", API_V3_0);
        final CreateActionRequest request = getRandomBackupMessage();
        final BackupNamePayload payload = (BackupNamePayload) request.getPayload();
        final String backupId = payload.getBackupName();
        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("lastTask", backupId, "2", "backupFile", Arrays.asList("3"));
        backup(agent, backupId, fragment);

        final ResponseEntity<JsonNode> getResponse = restTemplate
                .getForEntity(V4_DEFAULT_BACKUP_MANAGER + "last-task", JsonNode.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE, getResponse.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
        final ObjectNode getResponseBody = (ObjectNode) getResponse.getBody();
        assertNull(getResponseBody.get("id"));
        assertNull(getResponseBody.get("payload"));
        assertNotNull(getResponseBody.get("resource"));
        agent.shutdown();
    }

    @Test
    public void getLastActionByBackupManager_actionDoesNotExists_throwsException() {
        final ResponseEntity<JsonNode> getResponse = restTemplate
                .getForEntity(V4_BASE_URL + "backup-managers/" + BRM_WITH_NO_ACTION + "/last-task", JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND,
                getResponse.getStatusCode());
    }

    @Test
    public void getImportByBackupManager_actionExists_actionFound() throws Exception {

        final V4ImportExportRequest request = new V4ImportExportRequest();

        final URI uri = URI.create("sftp://user@localhost:222/remote/DEFAULT/myBackup");
        request.setUri(uri);
        request.setPassword("password");

        final String actionId = restTemplate.postForObject(V4_IMPORTS.toString(), request, V4ActionResponse.class).getActionId();
        waitForActionToFinish("DEFAULT", actionId);

        //Get single import
        final ResponseEntity<JsonNode> actionResponse = restTemplate.getForEntity(V4_IMPORTS + "/" + actionId, JsonNode.class);
        assertEquals(HttpStatus.OK, actionResponse.getStatusCode());
        final JsonNode action = actionResponse.getBody();
        assertEquals(ResultType.FAILURE.toString(), action.get("task").get("result").textValue());
        assertEquals(actionId, action.get("id").textValue());
        assertEquals(uri.toString(), action.get("uri").textValue());
        assertEquals("/backup-restore/v4/backup-managers/DEFAULT/backups/myBackup",
                action.get("backup").textValue());

        //Get imports
        final ResponseEntity<JsonNode> actionsResponse = restTemplate.getForEntity(V4_IMPORTS.toString(), JsonNode.class);
        assertEquals(HttpStatus.OK, actionsResponse.getStatusCode());
        final ArrayNode getResponseBody = (ArrayNode) actionsResponse.getBody();
        assertEquals(actionId, getResponseBody.get(getResponseBody.size() -1 ).get("id").textValue());
        assertEquals(uri.toString(), getResponseBody.get(getResponseBody.size() - 1).get("uri").textValue());
        assertEquals("/backup-restore/v4/backup-managers/DEFAULT/backups/myBackup",
                getResponseBody.get(getResponseBody.size() - 1).get("backup").textValue());
    }

    @Test
    public void getImportByBackupManager_actionDoesNotExists_throwsException() {
        final ResponseEntity<JsonNode> getResponse = restTemplate
                .getForEntity(V4_BASE_URL + "backup-managers/" + BRM_WITH_NO_ACTION + "/imports", JsonNode.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals(0, getResponse.getBody().size());

        final ResponseEntity<JsonNode> getResponse2 = restTemplate
                .getForEntity(V4_BASE_URL + "backup-managers/" + BRM_WITH_NO_ACTION + "/imports" + "abcd", JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse2.getStatusCode());
    }

    @Test
    public void getExportByBackupManager_actionExists_actionFound() throws Exception {

        final String BACKUP_MANAGER_ID = "backupManagerWithBackupToDelete";
        final String BACKUP_MANAGER_EXPORT_URL = V4_BASE_URL + "backup-managers/" + BACKUP_MANAGER_ID + "/backups/" + "backupToKeep" + "/exports";


        final V4ImportExportRequest request = new V4ImportExportRequest();
        request.setUri(SFTP_USER_LOCALHOST_222_REMOTE);
        request.setPassword("password");

        V4ActionResponse response = restTemplate.postForObject(BACKUP_MANAGER_EXPORT_URL, request, V4ActionResponse.class);
        assertNotNull(response.getUri());
        waitForActionToFinish(BACKUP_MANAGER_ID, response.getActionId());

        //Get an export action
        final ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity(BACKUP_MANAGER_EXPORT_URL + "/" + response.getActionId(), JsonNode.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        final JsonNode task = responseEntity.getBody().get("task");
        assertEquals(ResultType.FAILURE.toString(), task.get("result").textValue());
        assertNotNull(responseEntity.getBody().get("uri").textValue());

        //Get exports
        final ResponseEntity<JsonNode> actionsResponse = restTemplate.getForEntity(BACKUP_MANAGER_EXPORT_URL, JsonNode.class);
        assertEquals(HttpStatus.OK, actionsResponse.getStatusCode());
        final ArrayNode getResponseBody = (ArrayNode) actionsResponse.getBody();
        assertEquals(response.getActionId(), getResponseBody.get(getResponseBody.size() - 1).get("id").textValue());
        assertNotNull(getResponseBody.get(getResponseBody.size() - 1).get("uri").textValue());
    }

    @Test
    public void getRestoresByBackupManager_actionExists_actionFound() throws Exception {
        final SystemTestAgent agent = createTestAgent("Restores", API_V3_0);
        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("Restores", backupId, "2", "backupFile", Arrays.asList("3"));
        backup(agent, backupId, fragment);

        // perform restore
        String restoreActionId = restTemplate.postForObject(V4_DEFAULT_BACKUP_MANAGER_BACKUP + backupId + "/restores", null, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        agent.downloadFragments();
        waitForActionToFinish(restoreActionId);

        //Get an restore action
        final ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity(V4_DEFAULT_BACKUP_MANAGER_BACKUP + backupId + "/restores/" + restoreActionId, JsonNode.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        final JsonNode task = responseEntity.getBody().get("task");
        assertEquals(ResultType.SUCCESS.toString(), task.get("result").textValue());
        System.out.println(task.toPrettyString());

        //Get restores
        final ResponseEntity<JsonNode> actionsResponse = restTemplate.getForEntity(V4_DEFAULT_BACKUP_MANAGER_BACKUP + backupId + "/restores", JsonNode.class);
        assertEquals(HttpStatus.OK, actionsResponse.getStatusCode());
        System.out.println(actionsResponse.getBody().toPrettyString());
        final ArrayNode getResponseBody = (ArrayNode) actionsResponse.getBody();
        assertEquals(restoreActionId, getResponseBody.get(getResponseBody.size() - 1).get("id").textValue());
        agent.shutdown();
    }

    @Test
    public void getRestoresByBackupManager_actionDoesNotExists_throwsException() {
        final SystemTestAgent agent = createTestAgent("noRestore", API_V3_0);
        final CreateActionRequest request = getRandomBackupMessage();
        final BackupNamePayload payload = (BackupNamePayload) request.getPayload();
        final String backupId = payload.getBackupName();
        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("noRestore", backupId, "2", "backupFile", Arrays.asList("3"));
        backup(agent, backupId, fragment);

        final ResponseEntity<JsonNode> getResponse = restTemplate
                .getForEntity(V4_DEFAULT_BACKUP_MANAGER_BACKUP + backupId + "/restores", JsonNode.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals(0, getResponse.getBody().size());

        final ResponseEntity<JsonNode> getResponse2 = restTemplate
                .getForEntity(V4_DEFAULT_BACKUP_MANAGER_BACKUP + backupId + "/restores" + "/abcd", JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse2.getStatusCode());
        agent.shutdown();
    }

    @Test
    public void getExportsByBackupManager_actionDoesNotExists_throwsException() {
        final SystemTestAgent agent = createTestAgent("noExports", API_V3_0);
        final CreateActionRequest request = getRandomBackupMessage();
        final BackupNamePayload payload = (BackupNamePayload) request.getPayload();
        final String backupId = payload.getBackupName();
        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("noExports", backupId, "2", "backupFile", Arrays.asList("3"));
        backup(agent, backupId, fragment);

        final ResponseEntity<JsonNode> getResponse = restTemplate
                .getForEntity(V4_DEFAULT_BACKUP_MANAGER_BACKUP + backupId + "/exports", JsonNode.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals(0, getResponse.getBody().size());

        final ResponseEntity<JsonNode> getResponse2 = restTemplate
                .getForEntity(V4_DEFAULT_BACKUP_MANAGER_BACKUP + backupId + "/exports" + "/abcd", JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse2.getStatusCode());
        agent.shutdown();
    }

    @Test
    public void getExportsByBackupManager_backupDoesNotExists_throwsException() {
        final ResponseEntity<JsonNode> getResponse = restTemplate
                .getForEntity(V4_DEFAULT_BACKUP_MANAGER_BACKUP + "nonExistingBackupId" + "/exports", JsonNode.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());

        final ResponseEntity<JsonNode> getResponse2 = restTemplate
                .getForEntity(V4_DEFAULT_BACKUP_MANAGER_BACKUP + "nonExistingBackupId" + "/exports" + "/abcd", JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse2.getStatusCode());
    }

    @Test
    public void getRestoresByBackupManager_backupDoesNotExists_throwsException() {
        final ResponseEntity<JsonNode> getResponse = restTemplate
                .getForEntity(V4_DEFAULT_BACKUP_MANAGER_BACKUP + "nonExistingBackupId" + "/restores", JsonNode.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());

        final ResponseEntity<JsonNode> getResponse2 = restTemplate
                .getForEntity(V4_DEFAULT_BACKUP_MANAGER_BACKUP + "nonExistingBackupId" + "/restores" + "/abcd", JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse2.getStatusCode());
    }

    @Test
    public void getRestoresByBackupManager_backupManagerDoesNotExists_throwsException() {
        final ResponseEntity<JsonNode> getResponse = restTemplate
                .getForEntity(V4_BASE_URL + "backup-managers/" + "nonExistingBrm" + "/backups/" + "nonExistingBackupId" + "/restores", JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());

        final ResponseEntity<JsonNode> getResponse2 = restTemplate
                .getForEntity(V4_BASE_URL + "backup-managers/" + "nonExistingBrm" + "/backups/" + "nonExistingBackupId" + "/restores" + "/abcd", JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse2.getStatusCode());
    }

    @Test
    public void getExportsByBackupManager_backupManagerDoesNotExists_throwsException() {
        final ResponseEntity<JsonNode> getResponse = restTemplate
                .getForEntity(V4_BASE_URL + "backup-managers/" + "nonExistingBrm" + "/backups/" + "nonExistingBackupId" + "/exports", JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());

        final ResponseEntity<JsonNode> getResponse2 = restTemplate
                .getForEntity(V4_BASE_URL + "backup-managers/" + "nonExistingBrm" + "/backups/" + "nonExistingBackupId" + "/exports" + "/abcd", JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse2.getStatusCode());
    }


    private String backup(final SystemTestAgent agent, final String backupId,
                          final SystemTestBackupFragment fragment) {
        final V4CreateBackupRequest request = new V4CreateBackupRequest();
        request.setName(backupId);
        final String backupActionId = restTemplate.postForObject(V4_DEFAULT_BACKUP_MANAGER + "backups", request, CreateActionResponse.class).getActionId();

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
        return backupActionId;
    }
}
