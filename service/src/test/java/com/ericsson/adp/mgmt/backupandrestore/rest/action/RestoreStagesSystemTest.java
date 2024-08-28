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

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_DEFAULT_BACKUP_MANAGER;
import static org.junit.Assert.assertEquals;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestAgent;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestBackupFragment;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestWithAgents;
import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import java.util.Arrays;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestoreStagesSystemTest extends SystemTestWithAgents {

    private SystemTestAgent firstAgent;
    private SystemTestAgent secondAgent;
    private String restoreActionId;

    @Before
    public void setup() {
        firstAgent = createTestAgent("agentA", ApiVersion.API_V3_0);
        secondAgent = createTestAgent("agentB", ApiVersion.API_V3_0);

        final CreateActionRequest request = performBackup(firstAgent, secondAgent);

        request.setAction(ActionType.RESTORE);
        restoreActionId = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class)
                .getActionId();

        waitUntil(firstAgent::isParticipatingInAction);
        waitUntil(secondAgent::isParticipatingInAction);

        //Prep for - Restore
        firstAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.PREPARATION);
        secondAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.PREPARATION);
    }

    @Test
    public void restore_oneAgentFailsInPreparationStage_failsRestoreWithoutRestoringDataOnAgents() throws Exception {
        firstAgent.sendStageCompleteMessage(Action.RESTORE, true);
        secondAgent.sendStageCompleteMessage(Action.RESTORE, false);

        firstAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);
        secondAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);

        firstAgent.sendStageCompleteMessage(Action.RESTORE, true);
        secondAgent.sendStageCompleteMessage(Action.RESTORE, true);

        waitForActionToFinish(restoreActionId);

        assertActionFailed(restoreActionId);

        final ActionResponse action = getAction(restoreActionId);
        assertEquals(Double.valueOf(0.17d), action.getProgressPercentage());
    }

    @Test
    public void restore_oneAgentDisconnectsInPreparationStage_onlySendsCancelMessageToOtherAgent() throws Exception {
        firstAgent.sendOnErrorOnControlChannel();
        secondAgent.sendStageCompleteMessage(Action.RESTORE, false);

        secondAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);

        secondAgent.sendStageCompleteMessage(Action.RESTORE, true);

        waitForActionToFinish(restoreActionId);

        assertActionFailed(restoreActionId);

        final ActionResponse action = getAction(restoreActionId);
        assertEquals(Double.valueOf(0.0d), action.getProgressPercentage());
    }

    @Test
    public void Restore_agentsDisconnectBeforeSendingCancellationStageComplete_jobFinishes() throws Exception {
        firstAgent.sendStageCompleteMessage(Action.RESTORE, false);
        secondAgent.sendStageCompleteMessage(Action.RESTORE, false);

        firstAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);
        secondAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);

        firstAgent.sendOnErrorOnControlChannel();
        secondAgent.sendOnErrorOnControlChannel();

        waitForActionToFinish(restoreActionId);

        assertActionFailed(restoreActionId);

        final ActionResponse action = getAction(restoreActionId);
        assertEquals(Double.valueOf(0.0d), action.getProgressPercentage());
    }

    @Test
    public void restore_allAgentsFailInPostActionStage_failsRestore() throws Exception {
        firstAgent.sendStageCompleteMessage(Action.RESTORE, true);
        secondAgent.sendStageCompleteMessage(Action.RESTORE, true);

        firstAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.EXECUTION);
        secondAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.EXECUTION);

        firstAgent.sendStageCompleteMessage(Action.RESTORE, true);
        secondAgent.sendStageCompleteMessage(Action.RESTORE, true);

        firstAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.POST_ACTIONS);
        secondAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.POST_ACTIONS);

        firstAgent.sendStageCompleteMessage(Action.RESTORE, false);
        secondAgent.sendStageCompleteMessage(Action.RESTORE, false);

        firstAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);
        secondAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);

        firstAgent.sendStageCompleteMessage(Action.RESTORE, true);
        secondAgent.sendStageCompleteMessage(Action.RESTORE, true);

        waitForActionToFinish(restoreActionId);

        assertActionFailed(restoreActionId);
        final ActionResponse action = getAction(restoreActionId);
        assertEquals(Double.valueOf(0.67d), action.getProgressPercentage());
    }

    @Test
    public void restore_oneAgentsFailInPostActionStage_failsRestore() throws Exception {
        firstAgent.sendStageCompleteMessage(Action.RESTORE, true);
        secondAgent.sendStageCompleteMessage(Action.RESTORE, true);

        firstAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.EXECUTION);
        secondAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.EXECUTION);

        firstAgent.sendStageCompleteMessage(Action.RESTORE, true);
        secondAgent.sendStageCompleteMessage(Action.RESTORE,  true);

        firstAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.POST_ACTIONS);
        secondAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.POST_ACTIONS);

        firstAgent.sendStageCompleteMessage(Action.RESTORE, true);
        secondAgent.sendStageCompleteMessage(Action.RESTORE, false);

        firstAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);
        final ActionResponse actionAtPointA = getAction(restoreActionId);
        assertEquals(Double.valueOf(0.83d), actionAtPointA.getProgressPercentage());

        secondAgent.waitUntilStageIsReached(Action.RESTORE, OrchestratorMessageType.CANCEL_BACKUP_RESTORE);

        firstAgent.sendStageCompleteMessage(Action.RESTORE, true);
        secondAgent.sendStageCompleteMessage(Action.RESTORE, true);

        waitForActionToFinish(restoreActionId);

        assertActionFailed(restoreActionId);

        final ActionResponse action = getAction(restoreActionId);
        assertEquals(Double.valueOf(0.83d), action.getProgressPercentage());
    }

    private CreateActionRequest performBackup(final SystemTestAgent firstAgent, final SystemTestAgent secondAgent) {
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
        firstAgent.waitUntilStageIsReached(Action.BACKUP,OrchestratorMessageType.PREPARATION);
        secondAgent.waitUntilStageIsReached(Action.BACKUP,OrchestratorMessageType.PREPARATION);
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
        return request;
    }
}
