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

import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_DEFAULT_BACKUP_MANAGER;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_DEFAULT_BACKUP_MANAGER_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V2_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V2_0;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.EXECUTION;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.POST_ACTIONS;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.PREPARATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;

import com.ericsson.adp.mgmt.backupandrestore.SpringContext;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.BackupResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestAgent;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestBackupFragment;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestBackupFragmentWithCustomMetadata;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestWithAgents;
import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumCalculator;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import io.micrometer.core.instrument.MeterRegistry;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Test;

public class CreateBackupSystemTest extends SystemTestWithAgents {


    @Test
    public void backup_agentCreatesBackupWithSameId_UnsupportedOperationExceptionNotThrown() throws Exception {
        final SystemTestAgent agent = createTestAgent("v2Agent", API_V2_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        new SystemTestBackupFragmentWithCustomMetadata("v2Agent", backupId, "fragment_12",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123")).sendThroughAgent(agent);
        agent.closeDataChannel();
        agent.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(actionId);
        assertActionIsSuccessfullyCompleted(actionId);

        try {
            restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();
            waitUntil(agent::isParticipatingInAction);
        } catch (final UnsupportedOperationException e) {
            fail();
        } catch (final ConditionTimeoutException e) {
            //  Do Nothing
        }
    }

    @Test
    public void backup_oneRegisteredAgent_performsBackupOnAgent() throws Exception {
        final SystemTestAgent agent = createTestAgent("oneRegisteredAgent", API_V2_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        agent.waitUntilStageIsReached(BACKUP, PREPARATION);
        final SystemTestBackupFragment fragment = new SystemTestBackupFragmentWithCustomMetadata("oneRegisteredAgent", backupId, "fragment_333",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"));

        fragment.sendThroughAgent(agent);
        agent.sendStageCompleteMessage(BACKUP, true);
        agent.closeDataChannel();

        waitForActionToFinish(actionId);
        assertActionIsSuccessfullyCompleted(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.COMPLETE, backup.getStatus());
        assertEquals(2, backup.getSoftwareVersions().size());

        assertEquals(getMetadata("fragment_333"), fragment.getBackedUpMetadata());
        assertEquals("ABCDEF", fragment.getBackedUpFileContent());
        assertEquals(Optional.of("123"), fragment.getBackedUpCustomMetadataContent());
    }

    @Test
    public void backup_oneRegisteredAgent_performsBackupOnAgentOutOfOrderMessages() throws Exception {
        final SystemTestAgent agent = createTestAgent("oneRegisteredAgent", API_V2_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        agent.waitUntilStageIsReached(BACKUP, PREPARATION);
        agent.sendStageCompleteMessage(BACKUP, true);
        final SystemTestBackupFragment fragment = new SystemTestBackupFragmentWithCustomMetadata("oneRegisteredAgent", backupId, "fragment_333",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"));

        fragment.sendThroughAgent(agent);
        agent.closeDataChannel();

        waitForActionToFinish(actionId);
        assertActionIsSuccessfullyCompleted(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.COMPLETE, backup.getStatus());
        assertEquals(2, backup.getSoftwareVersions().size());

        assertEquals(getMetadata("fragment_333"), fragment.getBackedUpMetadata());
        assertEquals("ABCDEF", fragment.getBackedUpFileContent());
        assertEquals(Optional.of("123"), fragment.getBackedUpCustomMetadataContent());
    }


    @Test
    public void backup_backToBackSuccessfulBackupWithV2Agents_actionSucceeds() {
        final SystemTestAgent agentOne = createTestAgent("v2AgentOne", API_V2_0);
        final SystemTestAgent agentTwo = createTestAgent("v2AgentTwo", API_V2_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agentOne::isParticipatingInAction);
        waitUntil(agentTwo::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        new SystemTestBackupFragmentWithCustomMetadata("v2AgentOne", backupId, "fragment_12",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123")).sendThroughAgent(agentOne);
        agentOne.closeDataChannel();
        agentOne.sendStageCompleteMessage(BACKUP, true);

        new SystemTestBackupFragmentWithCustomMetadata("v2AgentTwo", backupId, "fragment_12",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123")).sendThroughAgent(agentTwo);
        agentTwo.closeDataChannel();
        agentTwo.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(actionId);
        assertActionIsSuccessfullyCompleted(actionId);

        //Second backup
        final CreateActionRequest request2 = getRandomBackupMessage();
        final String backupId2 = ((BackupNamePayload) request2.getPayload()).getBackupName();
        final String actionId2 = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request2, CreateActionResponse.class).getActionId();

        waitUntil(agentOne::isParticipatingInAction);
        waitUntil(agentTwo::isParticipatingInAction);
        waitUntilBackupExists(backupId2);

        new SystemTestBackupFragmentWithCustomMetadata("v2AgentOne", backupId2, "fragment_12",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123")).sendThroughAgent(agentOne);
        agentOne.closeDataChannel();
        agentOne.sendStageCompleteMessage(BACKUP, true);

        new SystemTestBackupFragmentWithCustomMetadata("v2AgentTwo", backupId2, "fragment_12",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123")).sendThroughAgent(agentTwo);
        agentTwo.closeDataChannel();

        agentTwo.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(actionId2);
        assertActionIsSuccessfullyCompleted(actionId2);
    }

    @Test
    public void backup_v2Endpoint_performsBackup() throws Exception {
        MeterRegistry registry = SpringContext.getBean(MeterRegistry.class).get();
        final SystemTestAgent agent = createTestAgent("oneRegisteredAgent", API_V2_0);

        final String backupId = "yangCreatedBackup";
        final YangActionResponse response = put(V2_BASE_URL + "ericsson-brm:brm::backup-manager::create-backup", getYangNamePayload("DEFAULT", backupId), YangActionResponse.class);
        final String actionId = String.valueOf(response.getActionId());

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        final SystemTestBackupFragment fragment = new SystemTestBackupFragmentWithCustomMetadata("oneRegisteredAgent", backupId, "fragment_333",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"));

        agent.waitUntilStageIsReached(BACKUP, PREPARATION);
        fragment.sendThroughAgent(agent);
        agent.closeDataChannel();

        agent.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(actionId);
        assertActionIsSuccessfullyCompleted(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.COMPLETE, backup.getStatus());
        assertEquals(2, backup.getSoftwareVersions().size());

        assertEquals(getMetadata("fragment_333"), fragment.getBackedUpMetadata());
        assertEquals("ABCDEF", fragment.getBackedUpFileContent());
        assertEquals(Optional.of("123"), fragment.getBackedUpCustomMetadataContent());

        // Verify that last action is present in bro.operation.info
        // bro.operation.transferred.bytes and bro.operation.end.time metrics
        assertNotNull(registry
                .find("bro.operation.info")
                .tag("action", ActionType.CREATE_BACKUP.name())
                .tag("backup_type", "DEFAULT")
                .tag("backup_name", backupId)
                .gauge());
        assertNotNull(registry
                .find("bro.operation.end.time")
                .tag("action", ActionType.CREATE_BACKUP.name())
                .tag("backup_type", "DEFAULT")
                .gauge());
        assertNotNull(registry
                .find("bro.operation.transferred.bytes")
                .tag("action", ActionType.CREATE_BACKUP.name())
                .tag("agent", "oneRegisteredAgent")
                .tag("backup_type", "DEFAULT")
                .gauge());
    }

    @Test
    public void backup_agentDoesNotSendCustomMetadata_performsBackupWithoutCustomMetadata() throws Exception {
        final SystemTestAgent agent = createTestAgent("noMetadata", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("noMetadata", backupId, "otherFragmentId", "backupFile",
                Arrays.asList("QWE", "ASD"));

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        fragment.sendThroughAgent(agent);
        agent.closeDataChannel();

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, POST_ACTIONS);
        agent.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(actionId);
        assertEquals(ResultType.SUCCESS, restTemplate.getForObject((V1_DEFAULT_BACKUP_MANAGER.toString() + "action/") + actionId, ActionResponse.class).getResult());
        assertActionIsSuccessfullyCompleted(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.COMPLETE, backup.getStatus());
        assertEquals(2, backup.getSoftwareVersions().size());

        assertEquals(getMetadata("otherFragmentId"), fragment.getBackedUpMetadata());
        assertEquals("QWEASD", fragment.getBackedUpFileContent());
        assertEquals(Optional.empty(), fragment.getBackedUpCustomMetadataContent());
    }

    @Test
    public void backup_agentSendsFragmentWithCustomInformation_persistsCustomInformation() throws Exception {
        final SystemTestAgent agent = createTestAgent("customInformation", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("customInformation", backupId, "otherFragmentId", "backupFile",
                Arrays.asList("QWE", "ASD"));

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        fragment.addCustomInformation("a", "1");
        fragment.addCustomInformation("b", "qweasd");

        fragment.sendThroughAgent(agent);
        agent.closeDataChannel();
        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, POST_ACTIONS);
        agent.sendStageCompleteMessage(BACKUP, true);
        //agent.finishAction(Action.BACKUP);

        waitForActionToFinish(actionId);
        assertEquals(ResultType.SUCCESS, restTemplate.getForObject((V1_DEFAULT_BACKUP_MANAGER.toString() + "action/") + actionId, ActionResponse.class).getResult());
        assertActionIsSuccessfullyCompleted(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.COMPLETE, backup.getStatus());
        assertEquals(2, backup.getSoftwareVersions().size());

        assertEquals("{\"fragmentId\":\"otherFragmentId\",\"version\":\"version\",\"sizeInBytes\":\"bytes\",\"customInformation\":{\"a\":\"1\",\"b\":\"qweasd\"}}", fragment.getBackedUpMetadata());
        assertEquals("QWEASD", fragment.getBackedUpFileContent());
        assertEquals(Optional.empty(), fragment.getBackedUpCustomMetadataContent());
    }

    @Test
    public void backup_oneRegisteredAgentWithSubAgents_performsBackupReceivingMultipleFragments() throws Exception {
        final SystemTestAgent agent = createTestAgent("hasSubAgents", API_V3_0);
        final SystemTestAgent firstSubAgent = createSubAgent();
        final SystemTestAgent secondSubAgent = createSubAgent();

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        final SystemTestBackupFragment firstFragment = new SystemTestBackupFragmentWithCustomMetadata("hasSubAgents", backupId, "1", "backupFile",
                Arrays.asList("1"), "custom", Arrays.asList("2"));
        final SystemTestBackupFragment secondFragment = new SystemTestBackupFragment("hasSubAgents", backupId, "2", "backupFile", Arrays.asList("3"));

        agent.sendStageCompleteMessage(BACKUP, true);
        //Pre -> Exec
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        firstFragment.sendThroughAgent(firstSubAgent);
        secondFragment.sendThroughAgent(secondSubAgent);
        firstSubAgent.closeDataChannel();
        secondSubAgent.closeDataChannel();

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, POST_ACTIONS);
        agent.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(actionId);
        assertEquals(ResultType.SUCCESS, restTemplate.getForObject((V1_DEFAULT_BACKUP_MANAGER.toString() + "action/") + actionId, ActionResponse.class).getResult());
        assertActionIsSuccessfullyCompleted(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.COMPLETE, backup.getStatus());
        assertEquals(2, backup.getSoftwareVersions().size());

        assertEquals(getMetadata("1"), firstFragment.getBackedUpMetadata());
        assertEquals("1", firstFragment.getBackedUpFileContent());
        assertEquals(Optional.of("2"), firstFragment.getBackedUpCustomMetadataContent());

        assertEquals(getMetadata("2"), secondFragment.getBackedUpMetadata());
        assertEquals("3", secondFragment.getBackedUpFileContent());
        assertEquals(Optional.empty(), secondFragment.getBackedUpCustomMetadataContent());
    }

    @Test
    public void backup_twoRegisteredAgentsWithFirstAgentSendingAllDataBeforeSecond_performsBackupOnAgents() throws Exception {
        final SystemTestAgent agentOne = createTestAgent("firstAgent", API_V3_0);
        final SystemTestAgent agentTwo = createTestAgent("secondAgent", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agentOne::isParticipatingInAction);
        waitUntil(agentTwo::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        final SystemTestBackupFragment firstFragment = new SystemTestBackupFragmentWithCustomMetadata("firstAgent", backupId, "A", "backupFile",
                Arrays.asList("b", "c"), "customMetadataFile", Arrays.asList("basd"));
        final SystemTestBackupFragment secondFragment = new SystemTestBackupFragment("secondAgent", backupId, "B", "backupFile",
                Arrays.asList("ddddddddddddddddddddd"));

        //Prep
        agentOne.sendStageCompleteMessage(BACKUP, true);
        agentTwo.sendStageCompleteMessage(BACKUP, true);

        //Exec
        agentTwo.waitUntilStageIsReached(BACKUP, EXECUTION);
        firstFragment.sendThroughAgent(agentOne);
        agentOne.closeDataChannel();
        secondFragment.sendThroughAgent(agentTwo);
        agentTwo.closeDataChannel();
        agentOne.sendStageCompleteMessage(BACKUP, true);
        agentTwo.sendStageCompleteMessage(BACKUP, true);

        //Post
        agentTwo.waitUntilStageIsReached(BACKUP, POST_ACTIONS);
        agentOne.sendStageCompleteMessage(BACKUP, true);
        agentTwo.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(actionId);
        assertEquals(ResultType.SUCCESS, restTemplate.getForObject((V1_DEFAULT_BACKUP_MANAGER.toString() + "action/") + actionId, ActionResponse.class).getResult());
        assertActionIsSuccessfullyCompleted(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.COMPLETE, backup.getStatus());
        assertEquals(3, backup.getSoftwareVersions().size());

        assertEquals(getMetadata("A"), firstFragment.getBackedUpMetadata());
        assertEquals("bc", firstFragment.getBackedUpFileContent());
        assertEquals(Optional.of("basd"), firstFragment.getBackedUpCustomMetadataContent());

        assertEquals(getMetadata("B"), secondFragment.getBackedUpMetadata());
        assertEquals("ddddddddddddddddddddd", secondFragment.getBackedUpFileContent());
        assertEquals(Optional.empty(), secondFragment.getBackedUpCustomMetadataContent());
    }

    @Test
    public void backup_twoRegisteredAgentsSendingDataAtTheSameTime_performsBackupOnAgents() throws Exception {
        final SystemTestAgent agentOne = createTestAgent("otherFirstAgent", API_V3_0);
        final SystemTestAgent agentTwo = createTestAgent("otherSecondAgent", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agentOne::isParticipatingInAction);
        waitUntil(agentTwo::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        final SystemTestBackupFragment firstFragment = new SystemTestBackupFragmentWithCustomMetadata("otherFirstAgent", backupId, "A", "backupFile",
                Arrays.asList("b", "c"), "customMetadataFile", Arrays.asList("basd"));
        final SystemTestBackupFragment secondFragment = new SystemTestBackupFragmentWithCustomMetadata("otherSecondAgent", backupId, "B",
                "backupFile", Arrays.asList("ddddddddddddddddddddd"), "S", Arrays.asList("cfncfncvbn"));

        //Pre Finished
        agentOne.sendStageCompleteMessage(BACKUP, true);
        agentTwo.sendStageCompleteMessage(BACKUP, true);

        agentTwo.waitUntilStageIsReached(BACKUP, EXECUTION);
        agentOne.sendMetadata("otherFirstAgent", agentOne.getBackupName(), "A");
        agentTwo.sendMetadata("otherSecondAgent", agentTwo.getBackupName(), "B");
        agentOne.sendDataFileName(firstFragment.getFileName());
        agentTwo.sendDataFileName(secondFragment.getFileName());
        agentOne.sendData(firstFragment.getFileChunks());
        agentTwo.sendData(secondFragment.getFileChunks());
        agentTwo.sendDataChecksum(getChecksum(secondFragment.getFileChunks()));
        agentOne.sendDataChecksum(getChecksum(firstFragment.getFileChunks()));
        agentTwo.sendCustomMetadataFileName(secondFragment.getCustomMetadataFileName().get());
        agentTwo.sendCustomMetadata(secondFragment.getCustomMetadataChunks());
        agentOne.sendCustomMetadataFileName(firstFragment.getCustomMetadataFileName().get());
        agentOne.sendCustomMetadata(firstFragment.getCustomMetadataChunks());
        agentTwo.sendCustomMetadataChecksum(getChecksum(secondFragment.getCustomMetadataChunks()));
        agentTwo.closeDataChannel();
        waitUntil(() -> secondFragment.getFragmentFolder().getCustomMetadataFileFolder().resolve(secondFragment.getCustomMetadataFileName().get())
                .toFile().exists());

        waitUntil(() -> getAction(actionId).getProgressPercentage().equals(Double.valueOf(0.33)));

        final ActionResponse action = getAction(actionId);
        assertEquals(ResultType.NOT_AVAILABLE, action.getResult());
        assertEquals(ActionStateType.RUNNING, action.getState());
        assertEquals(Double.valueOf(0.33), action.getProgressPercentage());

        agentOne.sendCustomMetadataChecksum(getChecksum(firstFragment.getCustomMetadataChunks()));
        waitUntil(() -> firstFragment.getFragmentFolder().getCustomMetadataFileFolder().resolve(firstFragment.getCustomMetadataFileName().get())
                .toFile().exists());
        agentOne.closeDataChannel();

        //Exec Finish -> Post
        agentOne.sendStageCompleteMessage(BACKUP, true);
        agentTwo.sendStageCompleteMessage(BACKUP, true);

        agentTwo.waitUntilStageIsReached(BACKUP, POST_ACTIONS);

        agentOne.sendStageCompleteMessage(BACKUP, true);
        agentTwo.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(actionId);

        assertEquals(ResultType.SUCCESS, restTemplate.getForObject((V1_DEFAULT_BACKUP_MANAGER.toString() + "action/") + actionId, ActionResponse.class).getResult());

        assertActionIsSuccessfullyCompleted(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.COMPLETE, backup.getStatus());
        assertEquals(3, backup.getSoftwareVersions().size());

        assertEquals(getMetadata("A"), firstFragment.getBackedUpMetadata());
        assertEquals("bc", firstFragment.getBackedUpFileContent());
        assertEquals(Optional.of("basd"), firstFragment.getBackedUpCustomMetadataContent());

        assertEquals(getMetadata("B"), secondFragment.getBackedUpMetadata());
        assertEquals("ddddddddddddddddddddd", secondFragment.getBackedUpFileContent());
        assertEquals(Optional.of("cfncfncvbn"), secondFragment.getBackedUpCustomMetadataContent());
    }

    @Test
    public void backup_nonDefaultBackupManager_onlyAgentInNonDefaultScopeIsBackedUp() throws Exception {
        final SystemTestAgent defaultAgent = createTestAgent("defaultAgent", API_V3_0);

        final SystemTestAgent scopedAgent = createTestAgent("notDefaultAgent", "systemTestScope", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_BASE_URL + "backup-manager/systemTestScope/action", request, CreateActionResponse.class)
                .getActionId();

        waitUntil(scopedAgent::isParticipatingInAction);
        waitUntilBackupExists("systemTestScope", backupId);

        assertFalse(defaultAgent.isParticipatingInAction());

        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("notDefaultAgent", backupId, "qqq", "aaa", Arrays.asList("bbb"),
                "systemTestScope");

        scopedAgent.sendStageCompleteMessage(BACKUP, true);
        scopedAgent.waitUntilStageIsReached(BACKUP, EXECUTION);
        fragment.sendThroughAgent(scopedAgent);
        scopedAgent.closeDataChannel();
        scopedAgent.sendStageCompleteMessage(BACKUP, true);
        scopedAgent.waitUntilStageIsReached(BACKUP, POST_ACTIONS);
        scopedAgent.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish("systemTestScope", actionId);
        assertActionIsSuccessfullyCompleted("systemTestScope", actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_BASE_URL + "backup-manager/systemTestScope/backup/" + backupId, BackupResponse.class);
        assertEquals(BackupStatus.COMPLETE, backup.getStatus());
        assertEquals(2, backup.getSoftwareVersions().size());

        assertEquals(getMetadata("qqq"), fragment.getBackedUpMetadata());
        assertEquals("bbb", fragment.getBackedUpFileContent());
        assertEquals(Optional.empty(), fragment.getBackedUpCustomMetadataContent());
    }

    @Test
    public void backup_agentSendsFragmentThenSendsFragmentWithSameId_secondFragmentOverridesTheFirst() throws Exception {
        final SystemTestAgent agent = createTestAgent("sameFragmentAgent", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        final SystemTestBackupFragment fragment = new SystemTestBackupFragmentWithCustomMetadata("sameFragmentAgent", backupId, "fragment_333",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"));

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        fragment.sendThroughAgent(agent);
        agent.closeDataChannel();

        new SystemTestBackupFragmentWithCustomMetadata("sameFragmentAgent", backupId, "fragment_333", "backupFile", Arrays.asList("qqq"),
                "customMetadataFile", Arrays.asList("www")).sendThroughAgent(agent);

        agent.closeDataChannel();
        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, POST_ACTIONS);
        agent.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(actionId);

        assertActionIsSuccessfullyCompleted(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.COMPLETE, backup.getStatus());
        assertEquals(2, backup.getSoftwareVersions().size());

        assertEquals(getMetadata("fragment_333"), fragment.getBackedUpMetadata());
        assertEquals("qqq", fragment.getBackedUpFileContent());
        assertEquals(Optional.of("www"), fragment.getBackedUpCustomMetadataContent());
    }

    //FIXME: Included in Jira for tests audit ADPPRG-26357. Cause of intermittent 20/05/11 14:59:26 INFO Job: Failing action <41877> - CREATE_BACKUP action did not execute because another action <16761> is already running
    @Test
    public void backup_twoRegisteredAgentsSendingDataAtTheSameTimeApiV2_performsBackupOnAgents() throws Exception {
        final SystemTestAgent agentOne = createTestAgent("otherFirstAgent", API_V2_0);
        final SystemTestAgent agentTwo = createTestAgent("otherSecondAgent", API_V2_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agentOne::isParticipatingInAction);
        waitUntil(agentTwo::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        final SystemTestBackupFragment firstFragment = new SystemTestBackupFragmentWithCustomMetadata("otherFirstAgent", backupId, "A", "backupFile",
                Arrays.asList("b", "c"), "customMetadataFile", Arrays.asList("basd"));
        final SystemTestBackupFragment secondFragment = new SystemTestBackupFragmentWithCustomMetadata("otherSecondAgent", backupId, "B",
                "backupFile", Arrays.asList("ddddddddddddddddddddd"), "S", Arrays.asList("cfncfncvbn"));

        agentOne.sendMetadata("otherFirstAgent", agentOne.getBackupName(), "A");
        agentTwo.sendMetadata("otherSecondAgent", agentTwo.getBackupName(), "B");
        agentOne.sendDataFileName(firstFragment.getFileName());
        agentTwo.sendDataFileName(secondFragment.getFileName());
        agentOne.sendData(firstFragment.getFileChunks());
        agentTwo.sendData(secondFragment.getFileChunks());
        agentTwo.sendDataChecksum(getChecksum(secondFragment.getFileChunks()));
        agentOne.sendDataChecksum(getChecksum(firstFragment.getFileChunks()));
        agentTwo.sendCustomMetadataFileName(secondFragment.getCustomMetadataFileName().get());
        agentTwo.sendCustomMetadata(secondFragment.getCustomMetadataChunks());
        agentOne.sendCustomMetadataFileName(firstFragment.getCustomMetadataFileName().get());
        agentOne.sendCustomMetadata(firstFragment.getCustomMetadataChunks());
        agentTwo.sendCustomMetadataChecksum(getChecksum(secondFragment.getCustomMetadataChunks()));
        agentTwo.closeDataChannel();
        waitUntil(() -> secondFragment.getFragmentFolder().getCustomMetadataFileFolder().resolve(secondFragment.getCustomMetadataFileName().get())
                .toFile().exists());


        final ActionResponse action = getAction(actionId);
        assertEquals(ResultType.NOT_AVAILABLE, action.getResult());
        assertEquals(ActionStateType.RUNNING, action.getState());

        agentOne.sendCustomMetadataChecksum(getChecksum(firstFragment.getCustomMetadataChunks()));
        waitUntil(() -> firstFragment.getFragmentFolder().getCustomMetadataFileFolder().resolve(firstFragment.getCustomMetadataFileName().get())
                .toFile().exists());
        agentOne.closeDataChannel();

        agentOne.sendStageCompleteMessage(BACKUP, true);
        agentTwo.sendStageCompleteMessage(BACKUP, true);
        agentTwo.sendStageCompleteMessage(BACKUP, true);
        agentOne.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(actionId);

        assertEquals(ResultType.SUCCESS, restTemplate.getForObject((V1_DEFAULT_BACKUP_MANAGER.toString() + "action/") + actionId, ActionResponse.class).getResult());

        assertActionIsSuccessfullyCompleted(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.COMPLETE, backup.getStatus());
        assertEquals(3, backup.getSoftwareVersions().size());

        assertEquals(getMetadata("A"), firstFragment.getBackedUpMetadata());
        assertEquals("bc", firstFragment.getBackedUpFileContent());
        assertEquals(Optional.of("basd"), firstFragment.getBackedUpCustomMetadataContent());

        assertEquals(getMetadata("B"), secondFragment.getBackedUpMetadata());
        assertEquals("ddddddddddddddddddddd", secondFragment.getBackedUpFileContent());
        assertEquals(Optional.of("cfncfncvbn"), secondFragment.getBackedUpCustomMetadataContent());
    }

    private String getChecksum(final List<String> chunks) {
        final ChecksumCalculator calculator = new ChecksumCalculator();
        chunks.forEach(chunk -> calculator.addBytes(chunk.getBytes()));
        return calculator.getChecksum();
    }

    private String getMetadata(final String fragmentId) {
        return "{\"fragmentId\":\"" + fragmentId + "\",\"version\":\"version\",\"sizeInBytes\":\"bytes\",\"customInformation\":{}}";
    }

}
