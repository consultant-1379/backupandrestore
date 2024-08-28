/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.bro.api.agent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.action.CancelBackupRestore;
import com.ericsson.adp.mgmt.bro.api.test.TestAgentBehavior;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.Execution;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import com.ericsson.adp.mgmt.control.PostActions;
import com.ericsson.adp.mgmt.control.Preparation;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

public class RestorePreparationStateTest {

    private Agent agent;
    private TestOrchestratorGrpcChannel channel;
    private AgentState state;
    private TestAgentBehavior agentBehavior;

    @Before
    public void setUp() {
        this.channel = new TestOrchestratorGrpcChannel();
        this.agentBehavior = new TestAgentBehavior();
        this.agent = new Agent(this.agentBehavior, this.channel);
        this.state = new RestorePreparationState(this.agent, getPreparationMessage());
    }

    @Test
    public void processMessage_ExecutionMessage_stateChangeToRestoreExecutionState() {
        final AgentState stateChange = this.state.processMessage(OrchestratorControl.newBuilder()
                .setAction(Action.RESTORE)
                .setOrchestratorMessageType(OrchestratorMessageType.EXECUTION)
                .setExecution(Execution.newBuilder().build())
                .build());

        assertTrue(stateChange instanceof RestoreExecutionState);
    }

    @Test
    public void processMessage_CancelMessage_stateChangeToCancelActionState() {
        final AgentState stateChange = this.state.processMessage(OrchestratorControl.newBuilder()
                .setAction(Action.RESTORE)
                .setOrchestratorMessageType(OrchestratorMessageType.CANCEL_BACKUP_RESTORE)
                .setCancel(CancelBackupRestore.newBuilder().build())
                .build());

        assertTrue(stateChange instanceof CancelActionState);

        stateChange.trigger();
        assertTrue(this.agentBehavior.cancelledAction());
    }

    @Test
    public void processMessage_postActionMessage_stateChangesToWaitingForActions() {
        final AgentState stateChange = this.state.processMessage(OrchestratorControl.newBuilder()
                .setAction(Action.RESTORE)
                .setOrchestratorMessageType(OrchestratorMessageType.POST_ACTIONS)
                .setPostActions(PostActions.newBuilder().build())
                .build());

        assertTrue(stateChange instanceof WaitingForActionState);
    }

    @Test
    public void trigger_agent_sendStageComplete() {
        final AgentState stateChange = this.state.processMessage(OrchestratorControl.newBuilder()
                .setAction(Action.RESTORE)
                .setOrchestratorMessageType(OrchestratorMessageType.EXECUTION)
                .setExecution(Execution.newBuilder().build())
                .build());

        assertTrue(stateChange instanceof RestoreExecutionState);

        stateChange.trigger();
        assertTrue(this.channel.getMessage().getStageComplete().getSuccess());
        assertTrue(this.agentBehavior.executedRestore());
    }

    @Test
    public void finishAction_returnsFinishedActionState() {
        final AgentState stateChange  = state.finishAction();
        assertFalse(this.channel.getMessage().getStageComplete().getSuccess());
        assertTrue(stateChange instanceof FinishedActionState);
    }

    @Test
    public void cancelAction_duringPreparationState_movesToCancelState() throws Exception {
        assertTrue(state.cancelAction() instanceof CancelActionState);
    }

    private class TestOrchestratorGrpcChannel extends OrchestratorGrpcChannel {

        private final List<AgentControl> messages = new LinkedList<>();

        protected TestOrchestratorGrpcChannel() {
            super(null);
        }

        @Override
        protected void sendControlMessage(final AgentControl message) {
            this.messages.add(message);
        }

        public AgentControl getMessage() {
            final AgentControl message = this.messages.remove(0);
            return message;
        }

    }

    private Preparation getPreparationMessage() {
        return Preparation.newBuilder()
                .setBackupName("Backup")
                .setSoftwareVersionInfo(getSoftwareInfo())
                .addFragment(getFragment())
                .build();
    }

    private SoftwareVersionInfo getSoftwareInfo() {
        return SoftwareVersionInfo.newBuilder()
                .setProductName("Name")
                .setProductNumber("Number")
                .setDescription("Description")
                .setRevision("Revision")
                .setProductionDate("Date")
                .setType("Type")
                .build();
    }

    private Fragment getFragment() {
        return Fragment.newBuilder()
                .setFragmentId("id")
                .setSizeInBytes("size")
                .setVersion("version")
                .build();
    }
}
