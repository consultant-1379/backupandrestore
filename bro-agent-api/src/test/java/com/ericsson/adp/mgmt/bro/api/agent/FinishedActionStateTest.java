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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static com.ericsson.adp.mgmt.action.Action.RESTORE;
import static com.ericsson.adp.mgmt.action.Action.BACKUP;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.action.CancelBackupRestore;
import com.ericsson.adp.mgmt.bro.api.test.TestAgentBehavior;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.OrchestratorMessageType;
import com.ericsson.adp.mgmt.control.PostActions;
import com.ericsson.adp.mgmt.control.Preparation;
import com.ericsson.adp.mgmt.metadata.Fragment;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

public class FinishedActionStateTest {
    private Agent agent;
    private TestOrchestratorGrpcChannel channel;
    private AgentState state;
    private TestAgentBehavior agentBehavior;

    @Before
    public void setUp() {
        channel = new TestOrchestratorGrpcChannel();
        agentBehavior = new TestAgentBehavior();
        agent = new Agent(agentBehavior, channel);
    }

    @Test
    public void processMessage_cancelRestoreMessage_stateChangeToWaitingForActions() {
        state = new FinishedActionState(agent, "Backup", RESTORE);
        final AgentState stateChange = state.processMessage(getCancelMessage(RESTORE));
        assertTrue(stateChange instanceof CancelActionState);
    }

    @Test
    public void processMessage_restorePostActionMessage_stateChangeToWaitingForActions() {
        state = new FinishedActionState(agent, "Backup", RESTORE);
        final AgentState stateChange = state.processMessage(getPostActionMessage(RESTORE));
        assertTrue(stateChange instanceof WaitingForActionState);

        stateChange.trigger();
        assertNull(channel.getMessage());
    }

    @Test
    public void processMessage_restorePreparationMessage_stateChangeToRestorePreparation() {
        state = new FinishedActionState(agent, "Backup", RESTORE);
        final AgentState stateChange = state.processMessage(getPreparationMessage(RESTORE));
        assertTrue(stateChange instanceof RestorePreparationState);
    }

    @Test
    public void trigger_restorePreparationMessage_sendStageComplete() {
        state = new FinishedActionState(agent, "Backup", RESTORE);
        final AgentState stateChange = state.processMessage(getPreparationMessage(RESTORE));
        assertTrue(stateChange instanceof RestorePreparationState);

        stateChange.trigger();
        assertTrue(channel.getMessage().getStageComplete().getSuccess());
    }

    @Test
    public void trigger_cancelRestoreState_stateChangeToWaitingForActions() {
        state = new FinishedActionState(agent, "Backup", RESTORE);
        final AgentState stateChange = state.processMessage(getCancelMessage(RESTORE));
        assertTrue(stateChange instanceof CancelActionState);

        stateChange.trigger();
        assertTrue(channel.getMessage().getStageComplete().getSuccess());
    }

    @Test
    public void trigger_cancelBackupState_stateChangeToWaitingForActions() {
        state = new FinishedActionState(agent, "Backup", BACKUP);
        state = new FinishedActionState(agent, "Backup", BACKUP);
        final AgentState stateChange = state.processMessage(getCancelMessage(RESTORE));
        assertTrue(stateChange instanceof CancelActionState);

        stateChange.trigger();
        assertTrue(channel.getMessage().getStageComplete().getSuccess());
    }

    @Test
    public void processMessage_cancelBackupMessage_stateChangeToWaitingForActions() {
        state = new FinishedActionState(agent, "Backup", BACKUP);
        final AgentState stateChange = state.processMessage(getCancelMessage(BACKUP));
        assertTrue(stateChange instanceof CancelActionState);
    }

    @Test
    public void processMessage_backupPostActionMessage_stateChangeToWaitingForActions() {
        state = new FinishedActionState(agent, "Backup", BACKUP);
        final AgentState stateChange = state.processMessage(getPostActionMessage(BACKUP));
        assertTrue(stateChange instanceof WaitingForActionState);

        stateChange.trigger();
        assertNull(channel.getMessage());
    }

    @Test
    public void processMessage_backupPreparationMessage_stateChangeToRestorePreparation() {
        state = new FinishedActionState(agent, "Backup", BACKUP);
        final AgentState stateChange = state.processMessage(getPreparationMessage(BACKUP));
        assertTrue(stateChange instanceof BackupPreparationState);
    }

    @Test
    public void trigger_backupPreparationMessage_sendStageComplete() {
        state = new FinishedActionState(agent, "Backup", BACKUP);
        final AgentState stateChange = state.processMessage(getPreparationMessage(BACKUP));
        assertTrue(stateChange instanceof BackupPreparationState);

        stateChange.trigger();
        assertTrue(channel.getMessage().getStageComplete().getSuccess());
    }

    @Test
    public void finishAction_backup_returnsFinishedActionState() {
        state = new FinishedActionState(agent, "Backup", BACKUP);
        final AgentState stateChange = state.finishAction();
        assertTrue(stateChange instanceof FinishedActionState);
    }

    @Test
    public void finishAction_restore_returnsFinishedActionState() {
        state = new FinishedActionState(agent, "Backup", RESTORE);
        final AgentState stateChange = state.finishAction();
        assertTrue(stateChange instanceof FinishedActionState);
    }

    private class TestOrchestratorGrpcChannel extends OrchestratorGrpcChannel {

        private final List<AgentControl> messages = new LinkedList<>();

        protected TestOrchestratorGrpcChannel() {
            super(null);
        }

        @Override
        protected void sendControlMessage(final AgentControl message) {
            messages.add(message);
        }

        public AgentControl getMessage() {
            if(messages.isEmpty()) {
                return null;
            }
            final AgentControl message = messages.remove(0);
            return message;
        }
    }

    private OrchestratorControl getCancelMessage(final Action actionType) {
        return OrchestratorControl.newBuilder()
                .setOrchestratorMessageType(OrchestratorMessageType.CANCEL_BACKUP_RESTORE)
                .setAction(actionType)
                .setCancel(CancelBackupRestore.newBuilder().build())
                .build();
    }

    private OrchestratorControl getPostActionMessage(final Action actionType) {
        return OrchestratorControl.newBuilder()
                .setOrchestratorMessageType(OrchestratorMessageType.POST_ACTIONS)
                .setAction(actionType)
                .setPostActions(PostActions.newBuilder().build())
                .build();
    }

    private OrchestratorControl getPreparationMessage(final Action actionType) {
        return OrchestratorControl.newBuilder()
                .setAction(actionType)
                .setOrchestratorMessageType(OrchestratorMessageType.PREPARATION)
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
