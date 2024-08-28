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
package com.ericsson.adp.mgmt.backupandrestore.rest.action;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_DEFAULT_BACKUP_MANAGER;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static org.junit.Assert.assertEquals;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.rest.error.ErrorResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestAgent;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestBackupFragment;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestWithAgents;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import java.util.Arrays;
import org.junit.Test;

public class RestoreErrorSystemTest extends SystemTestWithAgents {

    @Test
    public void restore_oneAgentNotAvailable_restoreActionFails() {
        final SystemTestAgent firstAgent = createTestAgent("agentA", API_V3_0);
        final SystemTestAgent secondAgent = createTestAgent("agentB", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String backupActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();

        waitUntil(firstAgent::isParticipatingInAction);
        waitUntil(secondAgent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        final SystemTestBackupFragment firstFragment = new SystemTestBackupFragment("agentA", backupId, "2", "backupFile", Arrays.asList("3"));
        final SystemTestBackupFragment secondFragment = new SystemTestBackupFragment("agentB", backupId, "555555", "qwe",
                Arrays.asList("aaaaaaaaaaaaaaa"));

        //Pre
        firstAgent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.PREPARATION);
        secondAgent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.PREPARATION);
        firstAgent.sendStageCompleteMessage(Action.BACKUP, true);
        secondAgent.sendStageCompleteMessage(Action.BACKUP, true);

        //Exec
        firstAgent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);
        secondAgent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);
        firstFragment.sendThroughAgent(firstAgent);
        firstAgent.closeDataChannel();
        secondFragment.sendThroughAgent(secondAgent);
        secondAgent.closeDataChannel();
        firstAgent.sendStageCompleteMessage(Action.BACKUP, true);
        secondAgent.sendStageCompleteMessage(Action.BACKUP, true);

        //Post
        firstAgent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.POST_ACTIONS);
        secondAgent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.POST_ACTIONS);
        firstAgent.sendStageCompleteMessage(Action.BACKUP, true);
        secondAgent.sendStageCompleteMessage(Action.BACKUP, true);

        waitForActionToFinish(backupActionId);
        secondAgent.shutdown();
        waitUntilAgentIsNotRegistered("agentB");
        firstAgent.clearBackupName(); // Mark agent as no longer participating in backup

        // perform restore
        request.setAction(ActionType.RESTORE);
        final String restoreActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();

        waitForActionToFinish(restoreActionId);

        assertActionFailed(restoreActionId);
    }

    @Test
    public void restore_agentSendsStageCompleteWithFailure_failsRestore() throws Exception {
        final SystemTestAgent agent = createTestAgent("firstAgentId", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String backupActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("firstAgentId", backupId, "2", "backupFile", Arrays.asList("3"));

        agent.sendStageCompleteMessage(Action.BACKUP, true);
        agent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);
        fragment.sendThroughAgent(agent);
        agent.closeDataChannel();
        agent.sendStageCompleteMessage(Action.BACKUP, true);
        agent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.POST_ACTIONS);
        agent.sendStageCompleteMessage(Action.BACKUP, true);

        waitForActionToFinish(backupActionId);

        // perform restore
        request.setAction(ActionType.RESTORE);
        final String restoreActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();

        waitUntil(agent::isParticipatingInAction);

        agent.sendStageCompleteMessage(Action.RESTORE, false);
        agent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);
        agent.sendStageCompleteMessage(Action.RESTORE, false);

        waitForActionToFinish(restoreActionId);

        final ActionResponse action = getAction(restoreActionId);
        assertEquals(ResultType.FAILURE, action.getResult());
        assertEquals(ActionStateType.FINISHED, action.getState());
        assertEquals(Double.valueOf(0.0), action.getProgressPercentage());
    }

    @Test
    public void restore_agentSendsOnErrorOnControlChannel_failsActionAndCorruptsRestore() {
        final SystemTestAgent agent = createTestAgent("firstAgentId", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String backupActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("firstAgentId", backupId, "2", "backupFile", Arrays.asList("3"));

        agent.sendStageCompleteMessage(Action.BACKUP, true);
        agent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);
        fragment.sendThroughAgent(agent);
        agent.closeDataChannel();
        agent.sendStageCompleteMessage(Action.BACKUP, true);
        agent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.POST_ACTIONS);
        agent.sendStageCompleteMessage(Action.BACKUP, true);

        waitForActionToFinish(backupActionId);

        // perform restore
        request.setAction(ActionType.RESTORE);
        final String restoreActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();

        waitUntil(agent::isParticipatingInAction);
        agent.sendOnErrorOnControlChannel();

        waitForActionToFinish(restoreActionId);

        final ActionResponse action = getAction(restoreActionId);
        assertEquals(ResultType.FAILURE, action.getResult());
        assertEquals(ActionStateType.FINISHED, action.getState());
        assertEquals(Double.valueOf(0.0), action.getProgressPercentage());
    }

    @Test
    public void restore_agentNotParticipatingInRestoreSendsOnErrorOnControlChannel_restoreFinishesSuccessfully() {
        final String scope = "abc";
        final SystemTestAgent firstAgent = createTestAgent("1", scope, API_V3_0);
        final SystemTestAgent secondAgent = createTestAgent("2", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(getScopedActionUrl(V1_BASE_URL, scope), request, CreateActionResponse.class).getActionId();

        waitUntil(firstAgent::isParticipatingInAction);
        waitUntilBackupExists(scope, backupId);

        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("1", backupId, "fragment_333", "backupFile", Arrays.asList("123"),
                scope);

        firstAgent.sendStageCompleteMessage(Action.BACKUP, true);
        firstAgent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);
        fragment.sendThroughAgent(firstAgent);
        firstAgent.closeDataChannel();

        firstAgent.sendStageCompleteMessage(Action.BACKUP, true);
        firstAgent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.POST_ACTIONS);
        firstAgent.sendStageCompleteMessage(Action.BACKUP, true);
        waitForActionToFinish(scope, actionId);
        assertActionIsSuccessfullyCompleted(scope, actionId);

        // perform restore
        request.setAction(ActionType.RESTORE);
        final String restoreActionId = restTemplate.postForObject(getScopedActionUrl(V1_BASE_URL, scope), request, CreateActionResponse.class).getActionId();
        waitUntil(firstAgent::isParticipatingInAction);

        secondAgent.sendOnErrorOnControlChannel();
        firstAgent.downloadFragments();
        waitForActionToFinish(scope, restoreActionId);

        assertActionIsSuccessfullyCompleted(scope, restoreActionId);

        assertEquals(1, firstAgent.getRestoredFragments().size());
        assertEquals(fragment.getExpectedRestoreMessages(), firstAgent.getRestoredFragments().get("fragment_333"));
    }

    @Test
    public void restore_inexistentBackup_unprocessableEntityResponse() {
        final CreateActionRequest request = getRandomCreateActionRequest(ActionType.RESTORE);
        final ErrorResponse response = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, ErrorResponse.class);
        assertEquals(422, response.getStatusCode());
    }

    @Test
    public void restore_agentOutsideScopeOpensBackupDataChannel_closesDataChannelAndContinuesWithBackup() {
        final SystemTestAgent agent = createTestAgent("1", "scopeA", API_V3_0);
        final SystemTestAgent outOfScopeAgent = createTestAgent("2", "outOfScope", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String backupActionId = restTemplate.postForObject(getScopedActionUrl(V1_BASE_URL, "scopeA"), request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists("scopeA", backupId);

        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("1", backupId, "2", "backupFile", Arrays.asList("3"), "scopeA");

        agent.sendStageCompleteMessage(Action.BACKUP, true);
        agent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);
        fragment.sendThroughAgent(agent);
        agent.closeDataChannel();
        agent.sendStageCompleteMessage(Action.BACKUP, true);
        agent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.POST_ACTIONS);
        agent.sendStageCompleteMessage(Action.BACKUP, true);

        waitForActionToFinish("scopeA", backupActionId);

        agent.clearBackupName(); // Mark agent as no longer participating in backup

        request.setAction(ActionType.RESTORE);
        final String restoreActionId = restTemplate.postForObject(getScopedActionUrl(V1_BASE_URL, "scopeA"), request, CreateActionResponse.class)
                .getActionId();
        waitUntil(agent::isParticipatingInAction);

        outOfScopeAgent.getBackupDataChannel();

        agent.downloadFragments();

        waitForActionToFinish("scopeA", restoreActionId);
        assertActionIsSuccessfullyCompleted("scopeA", restoreActionId);
    }

    @Test
    public void restore_noAgentRegistered_failedAction() throws Exception {
        final String scope = "backupManagerWithBackupToDelete";

        final CreateActionRequest request = getRestoreCreateActionRequest();
        final String backupActionId = restTemplate.postForObject(getScopedActionUrl(V1_BASE_URL, scope), request, CreateActionResponse.class).getActionId();

        waitForActionToFinish(scope, backupActionId);
        assertActionFailed(scope, backupActionId);

        final ActionResponse action = getAction(scope, backupActionId);

        assertEquals("Failing job for not having any registered agents", action.getAdditionalInfo());
    }

    @Test
    public void restore_corruptBackup_failsAction() throws Exception {
        final SystemTestAgent agent = createTestAgent("1", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        agent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.PREPARATION);
        agent.sendStageCompleteMessage(Action.BACKUP, true);

        agent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);
        new SystemTestBackupFragment("1", backupId, "fragment_333", "backupFile", Arrays.asList("123")).sendThroughAgent(agent);
        agent.sendStageCompleteMessage(Action.BACKUP, true);
        Thread.sleep(1000);
        agent.sendOnErrorOnDataChannel();

        agent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);
        agent.sendStageCompleteMessage(Action.BACKUP, true);

        waitForActionToFinish(actionId);
        assertActionFailed(actionId);

        request.setAction(ActionType.RESTORE);
        final String restoreActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();

        waitForActionToFinish(restoreActionId);
        assertActionFailed(restoreActionId);
    }

    private CreateActionRequest getRestoreCreateActionRequest() {
        final BackupNamePayload payload = new BackupNamePayload();
        payload.setBackupName("backupToKeep");
        final CreateActionRequest request = new CreateActionRequest();
        request.setAction(ActionType.RESTORE);
        request.setPayload(payload);
        return request;
    }
}
