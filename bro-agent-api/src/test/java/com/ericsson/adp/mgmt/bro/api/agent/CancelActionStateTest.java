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

public class CancelActionStateTest {

    private Agent agent;
    private TestOrchestratorGrpcChannel channel;
    private AgentState state;
    private TestAgentBehavior agentBehavior;

    @Before
    public void setUp() {
        this.channel = new TestOrchestratorGrpcChannel();
        this.agentBehavior = new TestAgentBehavior();
        this.agent = new Agent(this.agentBehavior, this.channel);
        this.state = new CancelActionState(this.agent, "Backup", Action.RESTORE);
    }

    @Test
    public void processMessage_cancelMessage_stateChangeToWaitingForActions() {
        final AgentState stateChange = this.state.processMessage(getCancelMessage());
        assertTrue(stateChange instanceof WaitingForActionState);
    }

    @Test
    public void trigger_agent_stateChangeToWaitingForActions() {
        final AgentState stateChange = this.state.processMessage(getCancelMessage());
        assertTrue(stateChange instanceof WaitingForActionState);

        stateChange.trigger();
        assertNull(this.channel.getMessage());
    }

    @Test
    public void finishAction_returnsWaitingForActionState() {
        final AgentState stateChange  = state.finishAction();
        assertTrue(stateChange instanceof WaitingForActionState);
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
            if(this.messages.isEmpty()) {
                return null;
            }
            final AgentControl message = this.messages.remove(0);
            return message;
        }
    }

    private OrchestratorControl getCancelMessage() {
        return OrchestratorControl.newBuilder()
                .setOrchestratorMessageType(OrchestratorMessageType.CANCEL_BACKUP_RESTORE)
                .setAction(Action.RESTORE)
                .setCancel(CancelBackupRestore.newBuilder().build())
                .build();
    }
}
