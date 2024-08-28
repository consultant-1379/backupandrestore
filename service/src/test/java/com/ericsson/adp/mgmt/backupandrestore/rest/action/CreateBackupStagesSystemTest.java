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
package com.ericsson.adp.mgmt.backupandrestore.rest.action;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_DEFAULT_BACKUP_MANAGER;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_DEFAULT_BACKUP_MANAGER_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V2_0;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.BackupResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestAgent;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestBackupFragment;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestBackupFragmentWithCustomMetadata;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestWithAgents;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class CreateBackupStagesSystemTest extends SystemTestWithAgents {

    public static final String TEST_AGENT_NAME_API2 = "AgentApiV2";
    public static final String TEST_AGENT_NAME_API3 = "AgentApiV3";

    @Test
    public void backup_bothSupportedMixedAgentApisParticipating_backupCompletesSuccessfully() {
        final SystemTestAgent agentV2 = createTestAgent(TEST_AGENT_NAME_API2, API_V2_0);
        final SystemTestAgent agentV3 = createTestAgent(TEST_AGENT_NAME_API3, API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agentV3::isParticipatingInAction);
        waitUntilBackupExists(backupId);
        agentV3.sendStageCompleteMessage(Action.BACKUP,true);
        waitUntil(agentV2::isParticipatingInAction);

        agentV2.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.PREPARATION);
        final SystemTestBackupFragment fragment = new SystemTestBackupFragmentWithCustomMetadata(TEST_AGENT_NAME_API2, backupId, "fragment_12",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"));
        fragment.sendThroughAgent(agentV2);
        agentV2.closeDataChannel();
        agentV2.sendStageCompleteMessage(Action.BACKUP,true);

        agentV3.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);

        new SystemTestBackupFragmentWithCustomMetadata(TEST_AGENT_NAME_API3, backupId, "fragment_32",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"))
        .sendThroughAgent(agentV3);
        agentV3.closeDataChannel();

        agentV3.sendStageCompleteMessage(Action.BACKUP,true);

        agentV3.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.POST_ACTIONS);

        agentV3.sendStageCompleteMessage(Action.BACKUP,true);
        waitForActionToFinish(actionId);

        assertActionIsSuccessfullyCompleted(actionId);
    }

    @Test
    public void backup_agentApiV2sParticipatingFails_corruptsBackupAndFailsAction() {
        final SystemTestAgent agentV2 = createTestAgent(TEST_AGENT_NAME_API2, API_V2_0);
        final SystemTestAgent agentV3 = createTestAgent(TEST_AGENT_NAME_API3, API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agentV3::isParticipatingInAction);
        waitUntilBackupExists(backupId);
        agentV3.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.PREPARATION);
        agentV3.sendStageCompleteMessage(Action.BACKUP,true);
        //pre done

        waitUntil(agentV2::isParticipatingInAction);
        agentV2.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.PREPARATION);
        agentV3.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);

        final SystemTestBackupFragment fragment = new SystemTestBackupFragmentWithCustomMetadata(TEST_AGENT_NAME_API2, backupId, "fragment_12",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"));
        fragment.sendThroughAgent(agentV2);
        agentV2.closeDataChannel();
        agentV2.sendStageCompleteMessage(Action.BACKUP,false);
        agentV3.sendStageCompleteMessage(Action.BACKUP, true);
        //exec done

        agentV3.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);
        //Response to cancel for V3, V2 does not handle cancel messages.
        agentV3.sendStageCompleteMessage(Action.BACKUP, true);


        waitForActionToFinish(actionId);
        assertActionFailed(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());
    }

    @Test
    public void backup_successfulBackupAfterV2agentFailsInPreviousBackup_successfulAction() {
        final SystemTestAgent agentV2 = createTestAgent(TEST_AGENT_NAME_API2, API_V2_0);
        final SystemTestAgent agentV3 = createTestAgent(TEST_AGENT_NAME_API3, API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agentV3::isParticipatingInAction);
        waitUntilBackupExists(backupId);
        agentV3.sendStageCompleteMessage(Action.BACKUP, true);
        waitUntil(agentV2::isParticipatingInAction);
        agentV2.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.PREPARATION);

        final SystemTestBackupFragment fragment = new SystemTestBackupFragmentWithCustomMetadata(TEST_AGENT_NAME_API2, backupId, "fragment_12",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"));
        fragment.sendThroughAgent(agentV2);
        agentV2.closeDataChannel();
        agentV2.sendStageCompleteMessage(Action.BACKUP, false);
        agentV3.sendStageCompleteMessage(Action.BACKUP, true);

        agentV3.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);
        agentV3.sendStageCompleteMessage(Action.BACKUP, true);

        waitForActionToFinish(actionId);
        assertActionFailed(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());

        //Second Action
        final CreateActionRequest newRequest = getRandomBackupMessage();
        final String newBackupId = ((BackupNamePayload) newRequest.getPayload()).getBackupName();
        final String newActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", newRequest, CreateActionResponse.class).getActionId();

        waitUntil(agentV3::isParticipatingInAction);
        waitUntilBackupExists(newBackupId);
        agentV3.sendStageCompleteMessage(Action.BACKUP, true);
        waitUntil(agentV2::isParticipatingInAction);
        agentV2.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.PREPARATION);

        new SystemTestBackupFragmentWithCustomMetadata(TEST_AGENT_NAME_API2, newBackupId, "fragment_12",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123")).sendThroughAgent(agentV2);
        agentV2.closeDataChannel();
        agentV2.sendStageCompleteMessage(Action.BACKUP, true);

        agentV3.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);

        new SystemTestBackupFragmentWithCustomMetadata(TEST_AGENT_NAME_API3, newBackupId, "fragment_32",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"))
        .sendThroughAgent(agentV3);
        agentV3.closeDataChannel();

        agentV3.sendStageCompleteMessage(Action.BACKUP, true);

        agentV3.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.POST_ACTIONS);

        agentV3.sendStageCompleteMessage(Action.BACKUP, true);
        waitForActionToFinish(newActionId);

        assertActionIsSuccessfullyCompleted(newActionId);
    }

    @Test
    public void backup_agentApiV3sParticipatingFailsInExecutionStage_corruptsBackupAndFailsAction() {
        final SystemTestAgent agentV2 = createTestAgent(TEST_AGENT_NAME_API2, API_V2_0);
        final SystemTestAgent agentV3 = createTestAgent(TEST_AGENT_NAME_API3, API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agentV3::isParticipatingInAction);
        waitUntilBackupExists(backupId);
        agentV3.sendStageCompleteMessage(Action.BACKUP, true);

        agentV2.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.PREPARATION);
        final SystemTestBackupFragment fragment = new SystemTestBackupFragmentWithCustomMetadata(TEST_AGENT_NAME_API2, backupId, "fragment_12",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"));
        fragment.sendThroughAgent(agentV2);
        agentV2.closeDataChannel();
        agentV2.sendStageCompleteMessage(Action.BACKUP, true);

        agentV3.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);

        //V2 Agent's are not participating until execution stage.
        waitUntil(agentV2::isParticipatingInAction);

        new SystemTestBackupFragmentWithCustomMetadata(TEST_AGENT_NAME_API3, backupId, "fragment_32",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"))
        .sendThroughAgent(agentV3);
        agentV3.closeDataChannel();

        agentV3.sendStageCompleteMessage(Action.BACKUP, false);

        agentV3.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);

        agentV3.sendStageCompleteMessage(Action.BACKUP, true);

        waitForActionToFinish(actionId);

        assertActionFailed(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());
    }

    @Test
    public void backup_successfulBackupAfterV3AgentFailsPreviousBackup_successfulAction() {
        final SystemTestAgent agentV2 = createTestAgent(TEST_AGENT_NAME_API2, API_V2_0);
        final SystemTestAgent agentV3 = createTestAgent(TEST_AGENT_NAME_API3, API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agentV3::isParticipatingInAction);
        waitUntilBackupExists(backupId);
        agentV3.sendStageCompleteMessage(Action.BACKUP, true);

        agentV2.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.PREPARATION);
        final SystemTestBackupFragment fragment = new SystemTestBackupFragmentWithCustomMetadata(TEST_AGENT_NAME_API2, backupId, "fragment_12",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"));
        fragment.sendThroughAgent(agentV2);
        agentV2.closeDataChannel();
        agentV2.sendStageCompleteMessage(Action.BACKUP, true);

        agentV3.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);

        //V2 Agent's are not participating until execution stage.
        waitUntil(agentV2::isParticipatingInAction);

        new SystemTestBackupFragmentWithCustomMetadata(TEST_AGENT_NAME_API3, backupId, "fragment_32",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"))
        .sendThroughAgent(agentV3);
        agentV3.closeDataChannel();

        agentV3.sendStageCompleteMessage(Action.BACKUP, false);

        agentV3.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);

        agentV3.sendStageCompleteMessage(Action.BACKUP, true);

        waitForActionToFinish(actionId);

        assertActionFailed(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());

        //Second Action
        final CreateActionRequest newRequest = getRandomBackupMessage();
        final String newBackupId = ((BackupNamePayload) newRequest.getPayload()).getBackupName();
        final String newActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", newRequest, CreateActionResponse.class).getActionId();

        waitUntil(agentV3::isParticipatingInAction);
        waitUntilBackupExists(newBackupId);
        agentV3.sendStageCompleteMessage(Action.BACKUP, true);

        agentV3.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);
        waitUntil(agentV2::isParticipatingInAction);

        new SystemTestBackupFragmentWithCustomMetadata(TEST_AGENT_NAME_API3, newBackupId, "fragment_32",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"))
        .sendThroughAgent(agentV3);
        agentV3.closeDataChannel();

        agentV3.sendStageCompleteMessage(Action.BACKUP, true);

        agentV2.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.PREPARATION);
        new SystemTestBackupFragmentWithCustomMetadata(TEST_AGENT_NAME_API2, newBackupId, "fragment_12",
            "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123")).sendThroughAgent(agentV2);
        agentV2.closeDataChannel();
        agentV2.sendStageCompleteMessage(Action.BACKUP, true);

        agentV3.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.POST_ACTIONS);

        agentV3.sendStageCompleteMessage(Action.BACKUP, true);
        waitForActionToFinish(newActionId);

        assertActionIsSuccessfullyCompleted(newActionId);
    }

    @Test
    public void backup_agentApiV3sParticipatingFailsInPreparationStage_corruptsBackupAndFailsAction() {
        createTestAgent(TEST_AGENT_NAME_API2, API_V2_0);
        final SystemTestAgent agentV3 = createTestAgent(TEST_AGENT_NAME_API3, API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil( agentV3::isParticipatingInAction);
        waitUntilBackupExists(backupId);
        agentV3.sendStageCompleteMessage(Action.BACKUP, false);

        agentV3.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);

        agentV3.sendStageCompleteMessage(Action.BACKUP, true);

        waitForActionToFinish(actionId);

        assertActionFailed(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());
    }

    @Test
    public void backup_oneAgentFailsInPreparationStage_failsBackupDoesNotMoveToExecutionStage() throws Exception {
        final SystemTestAgent agentOne = createTestAgent("agentOnePassesPrep", API_V3_0);
        final SystemTestAgent agentTwo = createTestAgent("agentTwoFailsPrep", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agentOne::isParticipatingInAction);
        waitUntil(agentTwo::isParticipatingInAction);
        agentOne.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.PREPARATION);

        waitUntilBackupExists(backupId);

        agentOne.sendStageCompleteMessage(Action.BACKUP, true);
        agentTwo.sendStageCompleteMessage(Action.BACKUP, false);

        agentOne.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);
        agentOne.sendStageCompleteMessage(Action.BACKUP, true);
        agentTwo.sendStageCompleteMessage(Action.BACKUP, true);

        waitForActionToFinish(actionId);
        assertActionFailed(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());
    }

    @Test
    public void backup_oneAgentFailsInExecutionStage_corruptsBackupAndFailsAction() throws Exception {
        final SystemTestAgent agentOne = createTestAgent("agentOnePassesExec", API_V3_0);
        final SystemTestAgent agentTwo = createTestAgent("agentTwoFailsExec", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agentOne::isParticipatingInAction);
        waitUntil(agentTwo::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        agentOne.sendStageCompleteMessage(Action.BACKUP, true);
        agentTwo.sendStageCompleteMessage(Action.BACKUP, true);
        agentTwo.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);

        final SystemTestBackupFragment fragment = new SystemTestBackupFragmentWithCustomMetadata("agentOnePassesExec", backupId, "fragment_12",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"));
        fragment.sendThroughAgent(agentOne);
        agentOne.closeDataChannel();
        agentOne.sendStageCompleteMessage(Action.BACKUP, true);
        agentTwo.sendStageCompleteMessage(Action.BACKUP, false);
        agentTwo.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);
        agentOne.sendStageCompleteMessage(Action.BACKUP, true);
        agentTwo.sendStageCompleteMessage(Action.BACKUP, false);

        waitForActionToFinish(actionId);
        assertActionFailed(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());
    }

    @Test
    public void backup_oneAgentFailsInPostActionStage_corruptsBackupAndFailsAction() throws Exception {
        final SystemTestAgent agentOne = createTestAgent("agentOnePassesPost", API_V3_0);
        final SystemTestAgent agentTwo = createTestAgent("agentTwoFailsPost", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agentOne::isParticipatingInAction);
        waitUntil(agentTwo::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        agentOne.sendStageCompleteMessage(Action.BACKUP, true);
        agentTwo.sendStageCompleteMessage(Action.BACKUP, true);
        agentOne.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);
        agentTwo.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);

        new SystemTestBackupFragmentWithCustomMetadata("agentOnePassesPost", backupId, "fragment_12",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123")).sendThroughAgent(agentOne);
        agentOne.closeDataChannel();

        new SystemTestBackupFragmentWithCustomMetadata("agentTwoFailsPost", backupId, "fragment_21",
                "backupFile", Arrays.asList("CBA", "FED"), "customMetadataFile", Arrays.asList("321")).sendThroughAgent(agentTwo);
        agentTwo.closeDataChannel();

        agentOne.sendStageCompleteMessage(Action.BACKUP, true);
        agentTwo.sendStageCompleteMessage(Action.BACKUP, true);
        agentTwo.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.POST_ACTIONS);
        agentOne.sendStageCompleteMessage(Action.BACKUP, true);
        agentTwo.sendStageCompleteMessage(Action.BACKUP, false);
        agentTwo.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);
        agentOne.sendStageCompleteMessage(Action.BACKUP, true);
        agentTwo.sendStageCompleteMessage(Action.BACKUP, false);
        waitForActionToFinish(actionId);
        assertActionFailed(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());
    }

    @Test
    public void backup_oneAgentDisconnectsInPreparationStage_corruptsBackupAndFailsAction() throws Exception {
        final SystemTestAgent agentOne = createTestAgent("agentOneDisconnects", API_V3_0);
        final SystemTestAgent agentTwo = createTestAgent("agentTwoFails", API_V3_0);
        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agentOne::isParticipatingInAction);
        waitUntil(agentTwo::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        agentOne.sendOnErrorOnControlChannel();
        agentTwo.sendStageCompleteMessage(Action.BACKUP, true);
        agentTwo.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);
        agentTwo.sendStageCompleteMessage(Action.BACKUP, true);

        waitForActionToFinish(actionId);

        assertActionFailed(actionId);
    }

    @Test
    public void backup_oneAgentOpensDataChannelInPreparationStage_rejectsFragmentButBackupRemainsOngoing() throws Exception {
        final SystemTestAgent agentOne = createTestAgent("agentOneOpensChannel", API_V3_0);
        final SystemTestAgent agentTwo = createTestAgent("agentTwoContinues", API_V3_0);
        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agentOne::isParticipatingInAction);
        waitUntil(agentTwo::isParticipatingInAction);

        waitUntilBackupExists(backupId);
        agentTwo.sendStageCompleteMessage(Action.BACKUP, true);
        agentOne.sendMetadata("agentOneOpensChannel", backupId, "fragment_63");
        waitUntil(() -> agentOne.getDataChannelError() != null);
        assertEquals(Status.ABORTED.getCode(), ((StatusRuntimeException) agentOne.getDataChannelError()).getStatus().getCode());

        agentOne.sendStageCompleteMessage(Action.BACKUP, true);
        agentOne.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);
        agentTwo.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);

        new SystemTestBackupFragmentWithCustomMetadata("agentOneOpensChannel", backupId, "fragment_333", "backupFile",
                Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123")).sendThroughAgent(agentOne);
        agentOne.closeDataChannel();

        new SystemTestBackupFragmentWithCustomMetadata("agentTwoContinues", backupId, "fragment_333", "backupFile",
                Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123")).sendThroughAgent(agentTwo);
        agentTwo.closeDataChannel();

        agentOne.sendStageCompleteMessage(Action.BACKUP, true);
        agentTwo.sendStageCompleteMessage(Action.BACKUP, true);
        agentOne.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.POST_ACTIONS);
        agentOne.sendStageCompleteMessage(Action.BACKUP, true);
        agentTwo.sendStageCompleteMessage(Action.BACKUP, true);

        waitForActionToFinish(actionId);

        assertActionIsSuccessfullyCompleted(actionId);
    }
}
