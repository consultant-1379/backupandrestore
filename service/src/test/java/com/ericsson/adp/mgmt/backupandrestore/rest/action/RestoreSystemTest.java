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
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V2_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.SpringContext;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestAgent;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestBackupFragment;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestBackupFragmentWithCustomMetadata;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestWithAgents;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class RestoreSystemTest extends SystemTestWithAgents {

    private static final Logger log = LogManager.getLogger(RestoreSystemTest.class);

    @Test
    public void fakeTest() {
        Assert.assertTrue(true);
    }

    @Test
    public void restore_oneRegisteredAgentWithBackupFile_receivesBackupFileOnRestore() throws Exception {
        final SystemTestAgent agent = createTestAgent("firstAgentId", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("firstAgentId", backupId, "2", "backupFile", Arrays.asList("3"));

        backup(agent, request, backupId, fragment);

        // perform restore
        request.setAction(ActionType.RESTORE);
        final String restoreActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();

        waitUntil(agent::isParticipatingInAction);

        agent.downloadFragments();
        waitForActionToFinish(restoreActionId);

        assertActionIsSuccessfullyCompleted(restoreActionId);

        assertEquals(1, agent.getRestoredFragments().size());
        assertEquals(fragment.getExpectedRestoreMessages(), agent.getRestoredFragments().get("2"));

        // Verify that last action is present in bro.operation.info
        // bro.operation.transferred.bytes and bro.operation.end.time metrics
        SpringContext.getBean(MeterRegistry.class).ifPresent(meterRegistry -> {
            assertNotNull(meterRegistry
                    .find("bro.operation.info")
                    .tag("action", ActionType.RESTORE.name())
                    .tag("backup_type", "DEFAULT")
                    .tag("backup_name", backupId)
                    .gauge());
            assertNotNull(meterRegistry
                    .find("bro.operation.end.time")
                    .tag("action", ActionType.RESTORE.name())
                    .tag("backup_type", "DEFAULT")
                    .gauge());
            assertNotNull(meterRegistry
                    .find("bro.operation.transferred.bytes")
                    .tag("action", ActionType.RESTORE.name())
                    .tag("agent", "firstAgentId")
                    .tag("backup_type", "DEFAULT")
                    .gauge());
        });
    }

    @Test
    public void restore_v2Endpoint_receivesBackupFileOnRestore() throws Exception {
        final SystemTestAgent agent = createTestAgent("firstAgentId", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("firstAgentId", backupId, "2", "backupFile", Arrays.asList("3"));

        backup(agent, request, backupId, fragment);

        final YangActionResponse response = put(V2_BASE_URL + "ericsson-brm:brm::backup-manager::backup::restore", getYangPayload("DEFAULT", backupId), YangActionResponse.class);
        final String actionId = String.valueOf(response.getActionId());

        waitUntil(agent::isParticipatingInAction);

        agent.downloadFragments();
        waitForActionToFinish(actionId);

        assertActionIsSuccessfullyCompleted(actionId);

        assertEquals(1, agent.getRestoredFragments().size());
        assertEquals(fragment.getExpectedRestoreMessages(), agent.getRestoredFragments().get("2"));

        // Verify that last action is present in bro.operation.info
        // bro.operation.transferred.bytes and bro.operation.end.time metrics
        SpringContext.getBean(MeterRegistry.class).ifPresent(meterRegistry -> {
            assertNotNull(meterRegistry
                    .find("bro.operation.info")
                    .tag("action", ActionType.RESTORE.name())
                    .tag("backup_type", "DEFAULT")
                    .tag("backup_name", backupId)
                    .gauge());
            assertNotNull(meterRegistry
                    .find("bro.operation.end.time")
                    .tag("action", ActionType.RESTORE.name())
                    .tag("backup_type", "DEFAULT")
                    .gauge());
            assertNotNull(meterRegistry
                    .find("bro.operation.transferred.bytes")
                    .tag("action", ActionType.RESTORE.name())
                    .tag("agent", "firstAgentId")
                    .tag("backup_type", "DEFAULT")
                    .gauge());
        });
    }

    @Test
    public void restore_oneRegisteredAgentWithBackupAndCustomMetadataFile_receivesBackupFileAndCustomMetadataFileOnResto() throws Exception {
        final SystemTestAgent agent = createTestAgent("someAgentId", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final SystemTestBackupFragment fragment = new SystemTestBackupFragmentWithCustomMetadata("someAgentId", backupId, "fragment_333",
                "backupFile", Arrays.asList("ABC"), "customMetadataFile", Arrays.asList("123"));

        backup(agent, request, backupId, fragment);

        // perform restore
        request.setAction(ActionType.RESTORE);
        final String restoreActionId = restTemplate.postForEntity(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getBody()
                .getActionId();

        waitUntil(agent::isParticipatingInAction);

        agent.downloadFragments();
        waitForActionToFinish(restoreActionId);

        assertActionIsSuccessfullyCompleted(restoreActionId);

        assertEquals(1, agent.getRestoredFragments().size());
        assertEquals(fragment.getExpectedRestoreMessages(), agent.getRestoredFragments().get("fragment_333"));
    }

    @Test
    public void restore_oneAgentWithMultipleFragments_receivesAllFragmentsOnRestore() throws Exception {
        final SystemTestAgent agent = createTestAgent("whichAgentId", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String backupActionId = restTemplate.postForEntity(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getBody()
                .getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        final SystemTestBackupFragment fragment = new SystemTestBackupFragmentWithCustomMetadata("whichAgentId", backupId, "fragment_333",
                "backupFile", Arrays.asList("ABC"), "customMetadataFile", Arrays.asList("123"));
        final SystemTestBackupFragment secondFragment = new SystemTestBackupFragment("whichAgentId", backupId, "aaaaaa", "asd",
                Arrays.asList("zxcwwwwwwwwwwwwwwwwwwww"));
        secondFragment.addCustomInformation("qwe", "asd");

        agent.sendStageCompleteMessage(Action.BACKUP, true);
        agent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);
        fragment.sendThroughAgent(agent);
        agent.closeDataChannel();
        secondFragment.sendThroughAgent(agent);
        agent.closeDataChannel();

        agent.sendStageCompleteMessage(Action.BACKUP, true);
        agent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.POST_ACTIONS);
        agent.sendStageCompleteMessage(Action.BACKUP, true);
        waitForActionToFinish(backupActionId);

        agent.clearBackupName(); // to mark agent as no longer participating in action

        request.setAction(ActionType.RESTORE);
        final String restoreActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();

        waitUntil(agent::isParticipatingInAction);

        agent.downloadFragments();
        waitForActionToFinish(restoreActionId);

        assertActionIsSuccessfullyCompleted(restoreActionId);

        assertEquals(2, agent.getRestoredFragments().size());
        assertEquals(fragment.getExpectedRestoreMessages(), agent.getRestoredFragments().get("fragment_333"));
        assertEquals(secondFragment.getExpectedRestoreMessages(), agent.getRestoredFragments().get("aaaaaa"));

        final Fragment secondReceivedFragment = agent.getFragments().stream().filter(receivedFragment -> "aaaaaa".equals(receivedFragment.getFragmentId())).findFirst().get();

        assertEquals("asd", secondReceivedFragment.getCustomInformationMap().get("qwe"));
    }

    @Test
    public void restore_nonDefaultBackupManager_onlyAgentInNonDefaultScopeIsRestored() throws Exception {
        log.info("START: restore_nonDefaultBackupManager_onlyAgentInNonDefaultScopeIsRestored()");
        final SystemTestAgent defaultAgent = createTestAgent("anAgent",  API_V3_0);
        final SystemTestAgent scopedAgent = createTestAgent("blueAgent", "restoreScope", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String backupActionId = restTemplate
                .postForObject(V1_BASE_URL + "backup-manager/restoreScope/action", request, CreateActionResponse.class).getActionId();

        waitUntil(scopedAgent::isParticipatingInAction);
        assertFalse(defaultAgent.isParticipatingInAction());
        waitUntilBackupExists("restoreScope", backupId);

        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("blueAgent", backupId, "2", "backupFile", Arrays.asList("3"),
                "restoreScope");

        scopedAgent.sendStageCompleteMessage(Action.BACKUP, true);
        scopedAgent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);
        fragment.sendThroughAgent(scopedAgent);
        scopedAgent.closeDataChannel();
        scopedAgent.sendStageCompleteMessage(Action.BACKUP, true);
        scopedAgent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.POST_ACTIONS);
        scopedAgent.sendStageCompleteMessage(Action.BACKUP, true);

        waitForActionToFinish("restoreScope", backupActionId);

        scopedAgent.clearBackupName(); // to mark agent as not longer participating in action

        // perform restore
        request.setAction(ActionType.RESTORE);
        final String restoreActionId = restTemplate
                .postForObject(V1_BASE_URL + "backup-manager/restoreScope/action", request, CreateActionResponse.class).getActionId();

        waitUntil(scopedAgent::isParticipatingInAction);
        assertFalse(defaultAgent.isParticipatingInAction());
        scopedAgent.downloadFragments();
        waitForActionToFinish("restoreScope", restoreActionId);

        assertActionIsSuccessfullyCompleted("restoreScope", restoreActionId);

        assertEquals(1, scopedAgent.getRestoredFragments().size());
        assertEquals(fragment.getExpectedRestoreMessages(), scopedAgent.getRestoredFragments().get("2"));
        log.info("END: restore_nonDefaultBackupManager_onlyAgentInNonDefaultScopeIsRestored()");
    }

    @Test
    public void restore_twoRegisteredAgents_bothAgentsReceiveTheirFragments() throws Exception {
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

        firstAgent.sendStageCompleteMessage(Action.BACKUP, true);
        secondAgent.sendStageCompleteMessage(Action.BACKUP, true);
        secondAgent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.EXECUTION);
        firstFragment.sendThroughAgent(firstAgent);
        firstAgent.closeDataChannel();
        secondFragment.sendThroughAgent(secondAgent);
        secondAgent.closeDataChannel();

        firstAgent.sendStageCompleteMessage(Action.BACKUP, true);
        secondAgent.sendStageCompleteMessage(Action.BACKUP, true);
        secondAgent.waitUntilStageIsReached(Action.BACKUP, OrchestratorMessageType.POST_ACTIONS);
        firstAgent.sendStageCompleteMessage(Action.BACKUP, true);
        secondAgent.sendStageCompleteMessage(Action.BACKUP, true);

        waitForActionToFinish(backupActionId);

        //Mark agents as no longer participating in action
        firstAgent.clearBackupName();
        secondAgent.clearBackupName();

        // perform restore
        request.setAction(ActionType.RESTORE);
        final String restoreActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();

        waitUntil(firstAgent::isParticipatingInAction);
        waitUntil(secondAgent::isParticipatingInAction);

        new Thread(firstAgent::downloadFragments).start();
        new Thread(secondAgent::downloadFragments).start();
        waitForActionToFinish(restoreActionId);

        assertActionIsSuccessfullyCompleted(restoreActionId);

        assertEquals(1, firstAgent.getRestoredFragments().size());
        assertEquals(firstFragment.getExpectedRestoreMessages(), firstAgent.getRestoredFragments().get("2"));

        assertEquals(1, secondAgent.getRestoredFragments().size());
        assertEquals(secondFragment.getExpectedRestoreMessages(), secondAgent.getRestoredFragments().get("555555"));
    }

    @Test
    public void restore_twoAgentsAreBackedUpThenThirdAgentRegistersInSameScope_onlyFirstTwoAgentsRestored() {
        final SystemTestAgent firstAgent = createTestAgent("agentA", "ScopeA", API_V3_0);
        final SystemTestAgent secondAgent = createTestAgent("agentB", "ScopeA", API_V3_0);
        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final SystemTestBackupFragment firstFragment = new SystemTestBackupFragment("agentA", backupId, "2", "backupFile", Arrays.asList("3"));
        final SystemTestBackupFragment secondFragment = new SystemTestBackupFragment("agentB", backupId, "555555", "qwe",
                Arrays.asList("aaaaaaaaaaaaaaa"));
        final String backupActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();

        //Pre
        waitUntil(firstAgent::isParticipatingInAction);
        waitUntil(secondAgent::isParticipatingInAction);
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

        //Mark agents as no longer participating in action
        firstAgent.clearBackupName();
        secondAgent.clearBackupName();

        final SystemTestAgent thirdAgent = createTestAgent("agentC", "ScopeA", API_V3_0);

        // perform restore
        request.setAction(ActionType.RESTORE);
        final String restoreActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();

        waitUntil(firstAgent::isParticipatingInAction);
        waitUntil(secondAgent::isParticipatingInAction);
        assertFalse(thirdAgent.isParticipatingInAction());

        new Thread(firstAgent::downloadFragments).start();
        new Thread(secondAgent::downloadFragments).start();
        waitForActionToFinish(restoreActionId);

        assertActionIsSuccessfullyCompleted(restoreActionId);

        assertTrue(thirdAgent.getRestoredFragments().isEmpty());

        assertEquals(1, firstAgent.getRestoredFragments().size());
        assertEquals(firstFragment.getExpectedRestoreMessages(), firstAgent.getRestoredFragments().get("2"));

        assertEquals(1, secondAgent.getRestoredFragments().size());
        assertEquals(secondFragment.getExpectedRestoreMessages(), secondAgent.getRestoredFragments().get("555555"));
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
        agent.clearBackupName(); // Mark agent as no longer participating in action
    }
}
