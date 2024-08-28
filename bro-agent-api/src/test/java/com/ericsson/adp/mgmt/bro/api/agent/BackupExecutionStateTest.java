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

import static org.junit.Assert.assertTrue;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.action.CancelBackupRestore;
import com.ericsson.adp.mgmt.bro.api.test.TestAgentBehavior;
import com.ericsson.adp.mgmt.control.Execution;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import com.ericsson.adp.mgmt.control.PostActions;
import org.junit.Before;
import org.junit.Test;

public class BackupExecutionStateTest {
    private Agent agent;
    private TestOrchestratorGrpcChannel channel;
    private AgentState state;
    private ActionInformation actionInformation;
    private TestAgentBehavior agentBehavior;

    @Before
    public void setup() {
        channel = new TestOrchestratorGrpcChannel();
        agentBehavior = new TestAgentBehavior();
        agent = new Agent(agentBehavior, channel);
        actionInformation = new ActionInformation("backup", "DEFAULT");
        state = new BackupExecutionState(agent, actionInformation);
    }

    @Test
    public void processMessage_postActionMessage_stateChangesToPostBackupState() {
        final AgentState changedState = state.processMessage(getPostActionMessage());
        assertTrue(changedState instanceof PostBackupState);
    }

    @Test
    public void processMessage_cancelMessage_stateChangeToCancelActionState() {
        final AgentState changedState = state.processMessage(
            OrchestratorControl.newBuilder()
            .setAction(Action.BACKUP)
            .setOrchestratorMessageType(OrchestratorMessageType.CANCEL_BACKUP_RESTORE)
            .setCancel(CancelBackupRestore.newBuilder().build())
            .build());

        assertTrue(changedState instanceof CancelActionState);
        changedState.trigger();
        assertTrue(agentBehavior.cancelledAction());
    }

    @Test
    public void processMessage_ExecutionMessage_stateChangesToWaitingForActions() {
        state = new BackupExecutionState(agent, actionInformation);
        final AgentState changedState = state.processMessage(getExecutionMessage());
        assertTrue(changedState instanceof WaitingForActionState);
    }

    @Test
    public void trigger_agent_stageComplete() {
        state = new BackupExecutionState(agent, actionInformation);
        final AgentState changedState = state.processMessage(getPostActionMessage());
        assertTrue(changedState instanceof PostBackupState);
        changedState.trigger();
        assertTrue(channel.getMessage().getStageComplete().getSuccess());
        assertTrue(agentBehavior.hasPerformedPostBackup());
    }

    private OrchestratorControl getPostActionMessage() {
        return OrchestratorControl.newBuilder()
            .setOrchestratorMessageType(OrchestratorMessageType.POST_ACTIONS)
            .setAction(Action.BACKUP)
            .setPostActions(PostActions.newBuilder().build())
            .build();
    }

    public OrchestratorControl getExecutionMessage() {
        return OrchestratorControl.newBuilder()
            .setOrchestratorMessageType(OrchestratorMessageType.EXECUTION)
            .setAction(Action.BACKUP)
            .setExecution(Execution.newBuilder().build())
            .build();
    }
}
