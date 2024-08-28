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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.CANCEL_BACKUP_RESTORE;
import static com.ericsson.adp.mgmt.control.OrchestratorMessageType.POST_ACTIONS;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.action.CancelBackupRestore;
import com.ericsson.adp.mgmt.bro.api.test.TestAgentBehavior;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import com.ericsson.adp.mgmt.control.PostActions;
import org.junit.Before;
import org.junit.Test;

public class PostBackupStateTest {

    private TestAgentBehavior agentBehavior;
    private Agent agent;
    private TestOrchestratorGrpcChannel channel;
    private AgentState state;

    @Before
    public void setUp() {
        channel = new TestOrchestratorGrpcChannel();
        agentBehavior = new TestAgentBehavior();
        agent = new Agent(agentBehavior, channel);
        state = new PostBackupState(agent, new ActionInformation("backup","backupType"));
    }

    @Test
    public void processMessage_postActionMessage_stateChangeToWaitingForActions() {
        final AgentState stateChange = this.state.processMessage(getPostActionMessage());
        assertEquals(WaitingForActionState.class, stateChange.getClass());
    }

    @Test
    public void processMessage_CancelMessage_stateChangeToWaitingForActionState() {
        final AgentState stateChange = this.state.processMessage(OrchestratorControl.newBuilder()
            .setAction(Action.BACKUP)
            .setOrchestratorMessageType(CANCEL_BACKUP_RESTORE)
            .setCancel(CancelBackupRestore.newBuilder().build())
            .build());

        assertEquals(WaitingForActionState.class, stateChange.getClass());
    }

    @Test
    public void trigger_agent_stateChangeToWaitingForActions() {
        final AgentState stateChange = this.state.processMessage(getPostActionMessage());
        assertEquals(WaitingForActionState.class, stateChange.getClass());

        stateChange.trigger();
        assertNull(this.channel.getMessage());
    }

    @Test
    public void finishAction_returnsFinishedActionState() {
        final AgentState stateChange  = state.finishAction();
        assertEquals(FinishedActionState.class, stateChange.getClass());
    }
    private OrchestratorControl getPostActionMessage() {
        return OrchestratorControl.newBuilder()
            .setOrchestratorMessageType(POST_ACTIONS)
            .setAction(Action.BACKUP)
            .setPostActions(PostActions.newBuilder().build())
            .build();
    }
}