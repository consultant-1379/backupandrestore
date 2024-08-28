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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.action.Action.RESTORE;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.EXECUTION;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.PREPARATION;


import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.bro.api.test.TestAgentBehavior;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.Execution;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.Preparation;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

public class WaitingForActionStateTest {

    private AgentState state;
    private Agent agent;
    private TestOrchestratorGrpcChannel channel;
    private TestAgentBehavior agentBehavior;

    @Before
    public void setUp() {
        channel =  new TestOrchestratorGrpcChannel();
        agentBehavior = new TestAgentBehavior();
        agent = new Agent(agentBehavior, channel);
    }

    @Test
    public void processMessage_restorePreparationMessage_stateChangeToRestorePreparation() {
        state = new WaitingForActionState(agent);

        final AgentState stateChange = state.processMessage(getPreparationMessage(RESTORE));
        assertEquals(RestorePreparationState.class, stateChange.getClass());
    }

    @Test
    public void processMessage_backupPreparationMessage_stateChangeToBackupExecution() {
        state = new WaitingForActionState(agent);

        final AgentState stateChange = state.processMessage(getPreparationMessage(BACKUP));
        assertEquals(BackupPreparationState.class, stateChange.getClass());
    }

    @Test
    public void processMessage_executionMessage_noStateChange() {
        state = new WaitingForActionState(agent);

        final AgentState stateChange = state.processMessage(OrchestratorControl.newBuilder()
                .setAction(RESTORE)
                .setOrchestratorMessageType(EXECUTION)
                .setExecution(Execution.newBuilder().build())
                .build());
        assertEquals(WaitingForActionState.class, stateChange.getClass());
    }

    @Test
    public void trigger_agent_stageComplete() {
        state = new WaitingForActionState(agent);

        final AgentState stateChange = state.processMessage(getPreparationMessage(BACKUP));

        stateChange.trigger();
        assertTrue(this.channel.getMessage().getStageComplete().getSuccess());
        assertTrue(agentBehavior.hasPreparedBackup());
    }

    @Test
    public void trigger_agentWithBehavior_stageCompleteFalse() {
        agent = new Agent(new RestoreTestAgentBehavior(), channel);
        state = new WaitingForActionState(agent);

        final AgentState stateChange = state.processMessage(getPreparationMessage(RESTORE));

        stateChange.trigger();
        assertFalse(this.channel.getMessage().getStageComplete().getSuccess());
    }

    @Test
    public void finishAction_returnsWaitingForActionState() {
        state = new WaitingForActionState(agent);
        final AgentState stateChange = state.finishAction();
        assertEquals(WaitingForActionState.class, stateChange.getClass());
    }

    private class RestoreTestAgentBehavior extends TestAgentBehavior {
        @Override
        public void prepareForRestore(final RestorePreparationActions restorePreparationActions) {
            restorePreparationActions.sendStageComplete(false, "failure");
        }
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
            final AgentControl message = messages.remove(0);
            return message;
        }
    }

    private OrchestratorControl getPreparationMessage(final Action action) {
        return OrchestratorControl
                .newBuilder()
                .setOrchestratorMessageType(PREPARATION)
                .setAction(action)
                .setPreparation(Preparation.newBuilder()
                        .setBackupName("Backup")
                        .setSoftwareVersionInfo(getSoftwareInfo())
                        .addFragment(getFragment())
                        .build())
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

