package com.ericsson.adp.mgmt.bro.api.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.bro.api.registration.RegistrationInformation;
import com.ericsson.adp.mgmt.bro.api.test.TestAgentBehavior;
import com.ericsson.adp.mgmt.control.AgentControl;

public class CancelActionsTest {

    private CancelActions cancelActions;
    private TestOrchestratorGrpcChannel channel;
    private Agent agent;

    @Before
    public void setUp() {
        this.channel = new TestOrchestratorGrpcChannel();
    }

    @Test
    public void sendStageComplete_testAgentBehavior_stageCompleteTrue() {
        this.agent = new Agent(new TestAgentBehavior(), this.channel);
        this.cancelActions = new CancelActions(this.agent, "BackupName", Action.BACKUP);

        this.cancelActions.sendStageComplete(true, "Success");
        assertTrue(this.channel.getMessage().getStageComplete().getSuccess());

        assertEquals("BackupName", this.cancelActions.getBackupName());
        assertEquals(Action.BACKUP, this.cancelActions.getAction());
    }

    @Test
    public void sendStageComplete_cancelAgentBehavior_stageCompleteTrue() {
        this.agent = new Agent(new CancelTestAgentBehavior(), this.channel);
        this.cancelActions = new CancelActions(this.agent, "Backup", Action.RESTORE);

        this.cancelActions.sendStageComplete(true, "Success");
        assertTrue(this.channel.getMessage().getStageComplete().getSuccess());

        assertEquals("Backup", this.cancelActions.getBackupName());
        assertEquals(Action.RESTORE, this.cancelActions.getAction());
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


    private class CancelTestAgentBehavior implements AgentBehavior {

        @Override
        public RegistrationInformation getRegistrationInformation() {
            return null;
        }

        @Override
        public void executeBackup(final BackupExecutionActions backupExecutionActions) {
            //Do nothing
        }

        @Override
        public void executeRestore(final RestoreExecutionActions restoreExecutionActions) {
            //Do nothing
        }

    }
}
