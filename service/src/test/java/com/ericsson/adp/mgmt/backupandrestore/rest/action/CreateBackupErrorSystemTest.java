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
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.CREATE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_DEFAULT_BACKUP_MANAGER;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_DEFAULT_BACKUP_MANAGER_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.CANCEL_BACKUP_RESTORE;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.EXECUTION;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.POST_ACTIONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.backup.PersistedBackup;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.job.FragmentFolder;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.BackupResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.IntegrationTest;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestAgent;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestBackupFragment;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestBackupFragmentWithCustomMetadata;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestWithAgents;
import com.ericsson.adp.mgmt.backupandrestore.util.ChecksumCalculator;
import com.ericsson.adp.mgmt.data.Metadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

//FIXME: One test isn't cleaning up properly. Included in Jira for tests audit ADPPRG-26357.
//FIXME: Intermittent failures. Included in Jira for tests audit ADPPRG-26357.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateBackupErrorSystemTest extends SystemTestWithAgents {

    @Test
    public void backup_backupWithNoName_unprocessableEntity() throws Exception {
        final CreateActionRequest request = new CreateActionRequest();
        request.setAction(CREATE_BACKUP);
        request.setPayload(new BackupNamePayload());

        final ResponseEntity<CreateActionResponse> responseEntity = restTemplate.postForEntity(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request,
                CreateActionResponse.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());
    }

    @Test
    public void backup_triesToCreateBackupWithSameNameAsExistingBackup_failsActionAndDoesNotOverwritePreviousBackup() throws Exception {
        final SystemTestAgent agent = createTestAgent("1", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        final SystemTestBackupFragment fragment = new SystemTestBackupFragmentWithCustomMetadata("1", backupId, "fragment_333", "backupFile",
                Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"));

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        fragment.sendThroughAgent(agent);
        agent.closeDataChannel();
        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, POST_ACTIONS);
        agent.sendStageCompleteMessage(BACKUP,true);

        waitForActionToFinish(actionId);

        assertActionIsSuccessfullyCompleted(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.COMPLETE, backup.getStatus());
        assertEquals(2, backup.getSoftwareVersions().size());

        PersistedBackup persistedBackup = readBackupFile(backupId);
        assertEquals(BackupStatus.COMPLETE, persistedBackup.getStatus());

        assertEquals(getMetadata("fragment_333"), fragment.getBackedUpMetadata());
        assertEquals("ABCDEF", fragment.getBackedUpFileContent());
        assertEquals(Optional.of("123"), fragment.getBackedUpCustomMetadataContent());

        final String secondActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();
        waitForActionToFinish(secondActionId);

        final ActionResponse secondAction = getAction(BackupManager.DEFAULT_BACKUP_MANAGER_ID, secondActionId);
        assertEquals(ResultType.FAILURE, secondAction.getResult());
        assertEquals(ActionStateType.FINISHED, secondAction.getState());
        assertEquals(Double.valueOf(0.0), secondAction.getProgressPercentage());

        persistedBackup = readBackupFile(backupId);
        assertEquals(BackupStatus.COMPLETE, persistedBackup.getStatus());

        assertEquals(getMetadata("fragment_333"), fragment.getBackedUpMetadata());
        assertEquals("ABCDEF", fragment.getBackedUpFileContent());
        assertEquals(Optional.of("123"), fragment.getBackedUpCustomMetadataContent());
    }

    @Test
    public void backup_oneAgentParticipatingAndReportsFailure_failsActionAndCorruptsBackup() throws Exception {
        final SystemTestAgent agent = createTestAgent("q", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, POST_ACTIONS);
        agent.sendStageCompleteMessage(BACKUP, false);
        agent.waitUntilStageIsReached(BACKUP, CANCEL_BACKUP_RESTORE);
        agent.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(actionId);

        final ActionResponse secondAction = getAction(BackupManager.DEFAULT_BACKUP_MANAGER_ID, actionId);
        assertEquals(ResultType.FAILURE, secondAction.getResult());
        assertEquals(ActionStateType.FINISHED, secondAction.getState());
        assertEquals(Double.valueOf(0.67), secondAction.getProgressPercentage());

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());
        assertEquals(2, backup.getSoftwareVersions().size());
    }

    @Test
    public void backup_performsSuccessfulBackupAfterFailingOneBeforeWithV3Agent_failsFirstBackupAndCompletesSecond() throws Exception {
        final SystemTestAgent agent = createTestAgent("q", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);
        agent.sendStageCompleteMessage(BACKUP, false);
        agent.waitUntilStageIsReached(BACKUP, CANCEL_BACKUP_RESTORE);
        agent.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(actionId);

        final ActionResponse action = getAction(BackupManager.DEFAULT_BACKUP_MANAGER_ID, actionId);
        assertEquals(ResultType.FAILURE, action.getResult());
        assertEquals(ActionStateType.FINISHED, action.getState());
        assertEquals(Double.valueOf(0.0), action.getProgressPercentage());

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());
        assertEquals(2, backup.getSoftwareVersions().size());

        final CreateActionRequest newRequest = getRandomBackupMessage();
        final String newBackupId = ((BackupNamePayload) newRequest.getPayload()).getBackupName();
        final String newActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", newRequest, CreateActionResponse.class)
                .getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(newBackupId);

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        new SystemTestBackupFragment("q", newBackupId, "fragment_333", "backupFile", Arrays.asList("123")).sendThroughAgent(agent);

        agent.closeDataChannel();
        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, POST_ACTIONS);
        agent.sendStageCompleteMessage(BACKUP,true);

        waitForActionToFinish(newActionId);

        assertActionIsSuccessfullyCompleted(newActionId);
    }

    @Test
    public void backup_agentSendsOnErrorOnDataChannel_failsActionAndCorruptsBackup() {
        final SystemTestAgent agent = createTestAgent("1", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "/action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        new SystemTestBackupFragment("1", backupId, "fragment_333", "backupFile", Arrays.asList("123")).sendThroughAgent(agent);

        agent.sendOnErrorOnDataChannel();
        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, CANCEL_BACKUP_RESTORE);
        agent.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(actionId);

        assertActionFailed(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());
        assertEquals(2, backup.getSoftwareVersions().size());
    }

    @Test
    public void backup_agentControlChannelSendsOnError_failsActionAndCorruptsBackup() {
        final SystemTestAgent agent = createTestAgent("1", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        new SystemTestBackupFragment("1", backupId, "fragment_333", "backupFile", Arrays.asList("123")).sendThroughAgent(agent);

        agent.sendOnErrorOnControlChannel();

        waitForActionToFinish(actionId);

        assertActionFailed(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());
        assertEquals(2, backup.getSoftwareVersions().size());
    }

    @Test
    public void backup_emptyFileName_failsActionAndCorruptsBackup() throws Exception {
        final SystemTestAgent agent = createTestAgent("emptyFileNameAgent", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("emptyFileNameAgent", backupId, "fragment_333", "",
                Arrays.asList("ABC", "DEF"));

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        fragment.sendThroughAgent(agent);
        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, CANCEL_BACKUP_RESTORE);
        agent.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(actionId);

        assertActionFailed(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());
    }

    @Test
    public void backup_agentNotParticipatingInBackupSendsOnErrorOnControlChannel_backupFinishesSuccessfully() {
        final String scope = "abc";
        final SystemTestAgent firstAgent = createTestAgent("1", scope, API_V3_0);
        final SystemTestAgent secondAgent = createTestAgent("2", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(getScopedActionUrl(V1_BASE_URL, scope), request, CreateActionResponse.class).getActionId();

        waitUntil(firstAgent::isParticipatingInAction);
        waitUntilBackupExists(scope, backupId);

        secondAgent.sendOnErrorOnControlChannel();

        firstAgent.sendStageCompleteMessage(BACKUP, true);
        firstAgent.waitUntilStageIsReached(BACKUP, EXECUTION);
        new SystemTestBackupFragment("1", backupId, "fragment_333", "backupFile", Arrays.asList("123"), scope).sendThroughAgent(firstAgent);
        firstAgent.closeDataChannel();
        firstAgent.sendStageCompleteMessage(BACKUP, true);
        firstAgent.waitUntilStageIsReached(BACKUP, POST_ACTIONS);
        firstAgent.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(scope, actionId);

        assertActionIsSuccessfullyCompleted(scope, actionId);

        final BackupResponse backup = restTemplate.getForObject(getScopedBackupUrl(V1_BASE_URL, scope).toString() + "/" + backupId, BackupResponse.class);
        assertEquals(BackupStatus.COMPLETE, backup.getStatus());
        assertEquals(2, backup.getSoftwareVersions().size());
    }

    @Test
    public void backup_noAgentRegistered_failedAction() throws Exception {
        final CreateActionRequest request = getRandomBackupMessage();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitForActionToFinish(actionId);

        assertActionFailed(actionId);

        final ActionResponse action = getAction(actionId);

        assertEquals("Failing job for not having any registered agents", action.getAdditionalInfo());
    }

    @Test
    public void backup_agentInScopeOpensRestoreDataChannel_failsBackup() {
        final SystemTestAgent agent = createTestAgent("1", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String backupActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        agent.getRestoreDataIterator(Metadata.newBuilder().setAgentId("1").build());
        agent.waitUntilStageIsReached(BACKUP, CANCEL_BACKUP_RESTORE);
        agent.sendStageCompleteMessage(BACKUP, true);
        waitForActionToFinish(backupActionId);
        assertActionFailed(backupActionId);
    }

    @Test
    public void backup_agentOutOfScopeOpensRestoreDataChannel_closesDataChannelAndContinuesWithBackup() {
        final SystemTestAgent agent = createTestAgent("1", "scopeA", API_V3_0);
        final SystemTestAgent outOfScopeAgent = createTestAgent("2", "outOfScope", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String backupActionId = restTemplate.postForObject(getScopedActionUrl(V1_BASE_URL, "scopeA"), request, CreateActionResponse.class)
                .getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists("scopeA", backupId);

        outOfScopeAgent.getRestoreDataIterator(Metadata.newBuilder().setAgentId("2").build());

        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("1", backupId, "2", "backupFile", Arrays.asList("3"), "scopeA");

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        fragment.sendThroughAgent(agent);
        agent.closeDataChannel();
        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, POST_ACTIONS);
        agent.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish("scopeA", backupActionId);
        assertActionIsSuccessfullyCompleted("scopeA", backupActionId);
    }

    @Test
    public void backup_backupDataDoesNotMatchChecksumAndAgentSendsSuccessfulStageComplete_failsActionAndCorruptsBackup() {
        final SystemTestAgent agent = createTestAgent("1", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        agent.sendMetadata("1", backupId, "myFragmentId");
        agent.sendDataFileName("myBackupFile");
        agent.sendData(Arrays.asList("ABC", "DEF"));
        agent.sendDataChecksum("wrongChecksum");

        final FragmentFolder fragmentFolder = new FragmentFolder(IntegrationTest.BACKUP_DATA_LOCATION.resolve(BackupManager.DEFAULT_BACKUP_MANAGER_ID)
                .resolve(backupId).resolve("1").resolve("myFragmentId"));
        waitUntil(() -> fragmentFolder.getDataFileFolder().resolve("myBackupFile").toFile().exists());

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, CANCEL_BACKUP_RESTORE);
        agent.sendStageCompleteMessage(BACKUP, true);
        waitForActionToFinish(actionId);
        assertActionFailed(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());
        assertFalse(agent.backupDataChannelIsOpen());
        assertEquals(2, backup.getSoftwareVersions().size());
    }

    @Test
    public void backup_backupDataDoesNotMatchChecksum_closesDataChannel() {
        final SystemTestAgent agent = createTestAgent("1", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        agent.sendMetadata("1", backupId, "myFragmentId");
        agent.sendDataFileName("myBackupFile");
        agent.sendData(Arrays.asList("ABC", "DEF"));
        agent.sendDataChecksum("wrongChecksum");

        waitUntil(() -> !agent.backupDataChannelIsOpen());

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.INCOMPLETE, backup.getStatus());
        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, CANCEL_BACKUP_RESTORE);
        agent.sendStageCompleteMessage(BACKUP, true);
        waitForActionToFinish(actionId);
        assertActionFailed(actionId);
    }

    @Test
    public void backup_customMetadataDoesNotMatchChecksumAndAgentSendsSuccessfulStageComplete_failsActionAndCorruptsBackup() {
        final SystemTestAgent agent = createTestAgent("1", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        final SystemTestBackupFragment firstFragment = new SystemTestBackupFragmentWithCustomMetadata("otherFirstAgent", backupId, "A", "backupFile",
                Arrays.asList("b", "c"), "customMetadataFile", Arrays.asList("basd"));

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        agent.sendMetadata("1", backupId, "A");
        agent.sendDataFileName(firstFragment.getFileName());
        agent.sendData(firstFragment.getFileChunks());
        agent.sendDataChecksum(getChecksum(firstFragment.getFileChunks()));
        agent.sendCustomMetadataFileName(firstFragment.getCustomMetadataFileName().get());
        agent.sendCustomMetadata(firstFragment.getCustomMetadataChunks());
        agent.sendCustomMetadataChecksum("wrongChecksum");

        waitUntil(() -> !agent.backupDataChannelIsOpen());
        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, CANCEL_BACKUP_RESTORE);
        agent.sendStageCompleteMessage(BACKUP, true);
        waitForActionToFinish(actionId);
        assertActionFailed(actionId);

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());
        assertFalse(agent.backupDataChannelIsOpen());
        assertEquals(2, backup.getSoftwareVersions().size());
    }

    @Test
    public void backup_customMetadataDoesNotMatchChecksum_closesDataChannel() {
        final SystemTestAgent agent = createTestAgent("1", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        agent.sendMetadata("1", backupId, "myFragmentId");
        agent.sendDataFileName("myBackupFile");
        agent.sendData(Arrays.asList("ABC", "DEF"));
        agent.sendDataChecksum("wrongChecksum");
        agent.sendCustomMetadataFileName("x");
        agent.sendCustomMetadata(Arrays.asList("Y"));
        agent.sendCustomMetadataChecksum("nope");

        waitUntil(() -> !agent.backupDataChannelIsOpen());

        final BackupResponse backup = restTemplate.getForObject(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + backupId, BackupResponse.class);
        assertEquals(BackupStatus.INCOMPLETE, backup.getStatus());
        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, CANCEL_BACKUP_RESTORE);
        agent.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish(actionId);
        assertActionFailed(actionId);
    }

    @Test
    public void backup_submitsNewActionWhileAnotherActionIsAlreadyRunning_gotUnprocessableEntityResponse() {
        final SystemTestAgent agent = createTestAgent("oneRegisteredAgent", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        final SystemTestBackupFragment fragment = new SystemTestBackupFragmentWithCustomMetadata("oneRegisteredAgent", backupId, "fragment_333",
                "backupFile", Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"));
        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        fragment.sendThroughAgent(agent);

        final CreateActionRequest newRequest = getRandomBackupMessage();
        final ResponseEntity<CreateActionResponse> responseEntity = restTemplate.postForEntity(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", newRequest, CreateActionResponse.class);
        assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
        agent.closeDataChannel();
        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, POST_ACTIONS);
        agent.sendStageCompleteMessage(BACKUP, true);
        waitForActionToFinish(actionId);

        assertActionIsSuccessfullyCompleted(actionId);
    }

    @Test
    public void backup_agentOutOfScopeOpensBackupDataChannel_closesDataChannelAndContinuesWithBackup() {
        final SystemTestAgent agent = createTestAgent("1", "scopeA", API_V3_0);
        final SystemTestAgent outOfScopeAgent = createTestAgent("2", "outOfScope", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String backupActionId = restTemplate.postForObject(getScopedActionUrl(V1_BASE_URL, "scopeA"), request, CreateActionResponse.class)
                .getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists("scopeA", backupId);

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        outOfScopeAgent.sendMetadata("2", backupId, "myFragmentId");
        waitUntil(() -> !outOfScopeAgent.backupDataChannelIsOpen());

        final SystemTestBackupFragment fragment = new SystemTestBackupFragment("1", backupId, "2", "backupFile", Arrays.asList("3"), "scopeA");

        fragment.sendThroughAgent(agent);
        agent.closeDataChannel();
        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, POST_ACTIONS);
        agent.sendStageCompleteMessage(BACKUP, true);

        waitForActionToFinish("scopeA", backupActionId);
        assertActionIsSuccessfullyCompleted("scopeA", backupActionId);
    }

    @Test
    public void backup_vulnerableBackupName_unprocessableEntity() throws Exception {
        final CreateActionRequest request = new CreateActionRequest();
        request.setAction(CREATE_BACKUP);
        final BackupNamePayload backupNamePayload = new BackupNamePayload();
        backupNamePayload.setBackupName("</mybackup100w45pz4p><script>alert(1);</script><mybackup100w45pz4p>");
        request.setPayload(backupNamePayload);

        final ResponseEntity<CreateActionResponse> responseEntity = restTemplate.postForEntity(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request,
                CreateActionResponse.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());
    }

    @Test
    public void backup_invalidBackupName_unprocessableEntity() throws Exception {
        final CreateActionRequest request = new CreateActionRequest();
        request.setAction(CREATE_BACKUP);
        final BackupNamePayload backupNamePayload = new BackupNamePayload();
        backupNamePayload.setBackupName("..");
        request.setPayload(backupNamePayload);

        final ResponseEntity<CreateActionResponse> responseEntity = restTemplate.postForEntity(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request,
                CreateActionResponse.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());
    }

    @Test
    public void backup_invalidFragmentId_orchestratorRejectsFragmentButBackupRemainsOngoing() throws Exception {
        final SystemTestAgent agent = createTestAgent("1", API_V3_0);

        final CreateActionRequest request = getRandomBackupMessage();
        final String backupId = ((BackupNamePayload) request.getPayload()).getBackupName();
        final String actionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getActionId();

        waitUntil(agent::isParticipatingInAction);
        waitUntilBackupExists(backupId);

        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, EXECUTION);
        agent.sendMetadata("1", agent.getBackupName(), "../..");

        waitUntil(() -> agent.getDataChannelError() != null);

        assertEquals(Status.ABORTED.getCode(), ((StatusRuntimeException) agent.getDataChannelError()).getStatus().getCode());

        final SystemTestBackupFragment fragment = new SystemTestBackupFragmentWithCustomMetadata("1", backupId, "fragment_333", "backupFile",
                Arrays.asList("ABC", "DEF"), "customMetadataFile", Arrays.asList("123"));

        fragment.sendThroughAgent(agent);
        agent.closeDataChannel();
        agent.sendStageCompleteMessage(BACKUP, true);
        agent.waitUntilStageIsReached(BACKUP, POST_ACTIONS);
        agent.sendStageCompleteMessage(BACKUP, true);
        waitForActionToFinish(actionId);
        assertActionIsSuccessfullyCompleted(actionId);
    }

    private PersistedBackup readBackupFile(final String backupId) throws Exception {
        return new ObjectMapper().readValue(
                BACKUP_MANAGERS_LOCATION.resolve(BackupManager.DEFAULT_BACKUP_MANAGER_ID).resolve("backups").resolve(backupId + ".json").toFile(),
                PersistedBackup.class);
    }

    private String getMetadata(final String fragmentId) {
        return "{\"fragmentId\":\"" + fragmentId + "\",\"version\":\"version\",\"sizeInBytes\":\"bytes\",\"customInformation\":{}}";
    }

    private String getChecksum(final List<String> chunks) {
        final ChecksumCalculator calculator = new ChecksumCalculator();
        chunks.forEach(chunk -> calculator.addBytes(chunk.getBytes()));
        return calculator.getChecksum();
    }

}
