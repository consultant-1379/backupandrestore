/*
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 * ****************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.bro.api.agent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.action.CancelBackupRestore;
import com.ericsson.adp.mgmt.bro.api.test.TestAgentBehavior;
import com.ericsson.adp.mgmt.control.Execution;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import com.ericsson.adp.mgmt.control.PostActions;
import com.ericsson.adp.mgmt.control.Preparation;
import org.junit.Before;
import org.junit.Test;

public class BackupPreparationStateTest {
    private Agent agent;
    private TestOrchestratorGrpcChannel channel;
    private AgentState state;
    private TestAgentBehavior agentBehavior;

    @Before
    public void setUp() {
        channel = new TestOrchestratorGrpcChannel();
        agentBehavior = new TestAgentBehavior();
        agent = new Agent(agentBehavior, channel);
        state = new BackupPreparationState(agent,new ActionInformation(getPreparationMessage()));
    }

    @Test
    public void processMessage_ExecutionMessage_stateChangeToExecutionState() {
        final AgentState stateChange = state.processMessage(OrchestratorControl.newBuilder().setAction(Action.BACKUP).setOrchestratorMessageType(
            OrchestratorMessageType.EXECUTION).setExecution(Execution.newBuilder().build()).build());
        assertTrue(stateChange instanceof BackupExecutionState);
    }

    @Test
    public void processMessage_CancelMessage_stateChangeToCancelActionState() {
        final AgentState stateChange = state.processMessage(OrchestratorControl.newBuilder().setAction(Action.BACKUP).setOrchestratorMessageType(
            OrchestratorMessageType.CANCEL_BACKUP_RESTORE)
            .setCancel(CancelBackupRestore.newBuilder().build()).build());
        assertTrue(stateChange instanceof CancelActionState);
        stateChange.trigger();
        assertTrue(agentBehavior.cancelledAction());
    }

    @Test
    public void processMessage_postActionMessage_stateChangeToWaitingForActions() {
        final AgentState stateChange = state.processMessage(OrchestratorControl.newBuilder()
            .setAction(Action.BACKUP)
            .setOrchestratorMessageType(OrchestratorMessageType.POST_ACTIONS)
            .setPostActions(PostActions.newBuilder().build()).build());
        assertTrue(stateChange instanceof WaitingForActionState);
    }

    @Test
    public void trigger_agent_sendStageComplete() {
        final AgentState stateChange = state.processMessage(OrchestratorControl.newBuilder()
        .setAction(Action.BACKUP)
        .setOrchestratorMessageType(OrchestratorMessageType.EXECUTION)
        .setExecution(Execution.newBuilder().build()).build());
        assertTrue(stateChange instanceof BackupExecutionState);
        stateChange.trigger();
        assertTrue(channel.getMessage().getStageComplete().getSuccess());
        assertTrue(agentBehavior.executedBackup());
    }

    @Test
    public void finishAction_returnsFinishedActionState() {
        final AgentState stateChange = state.finishAction();
        assertFalse(channel.getMessage().getStageComplete().getSuccess());
        assertTrue(stateChange instanceof FinishedActionState);
    }

    private Preparation getPreparationMessage() {
        return Preparation.newBuilder().
            setBackupName("Backup")
            .build();
    }
}
