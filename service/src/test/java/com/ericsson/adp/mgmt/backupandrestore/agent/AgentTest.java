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
package com.ericsson.adp.mgmt.backupandrestore.agent;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.adp.mgmt.backupandrestore.agent.state.AgentStateChange;
import com.ericsson.adp.mgmt.backupandrestore.agent.state.UnrecognizedState;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.control.AgentControl;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Agent.class, UnrecognizedState.class })
public class AgentTest {

    private UnrecognizedStateStub stateStub;
    private Agent agent;
    private AgentRepository agentRepository;
    private AgentInputStream agentInputStream;

    @Before
    public void setup() throws Exception {
        stateStub = new UnrecognizedStateStub();

        PowerMock.expectNew(UnrecognizedState.class, new Object[] { null }).andReturn(stateStub);
        PowerMock.replay(UnrecognizedState.class);

        agentRepository = createMock(AgentRepository.class);
        agentInputStream = createMock(AgentInputStream.class);

        agent = new Agent(agentInputStream, agentRepository, null);
    }

    @Test
    public void processMessage_message_stateIsChangedAndPostActionIsExecuted() throws Exception {
        agent.processMessage(mockMessage());

        assertTrue(stateStub.wasPostActionCalled());
    }

    @Test
    public void prepareForBackup_jobPerformingBackup_triggersBackupThroughStateAndUpdatesIt() throws Exception {
        final CreateBackupJob job = createMock(CreateBackupJob.class);

        agent.prepareForBackup(job);

        assertTrue(stateStub.executedBackup());
        assertTrue(stateStub.wasPostActionCalled());
        assertEquals(job, stateStub.getBackupJob());
    }

    @Test
    public void executeRestore_agent_triggersRestoreExecutionThroughStateAndCallsPostAction() throws Exception {
        agent.executeRestore();

        assertTrue(stateStub.executedRestore());
        assertTrue(stateStub.wasPostActionCalled());
    }

    @Test
    public void close_inputStream_closesStream() throws Exception {
        agentInputStream.close();
        expectLastCall();

        agentRepository.removeAgent(agent);
        expectLastCall();
        replay(agentInputStream, agentRepository);

        agent.closeConnection();

        verify(agentInputStream, agentRepository);
        assertTrue(stateStub.handledConnectionClosing());
    }

    @Test
    public void closeConnection_inputStreamAndError_closesStream() throws Exception {
        final StatusRuntimeException exception = Status.ABORTED.asRuntimeException();
        agentInputStream.close(exception);
        expectLastCall();

        agentRepository.removeAgent(agent);
        expectLastCall();
        replay(agentInputStream, agentRepository);

        agent.closeConnection(exception);

        verify(agentInputStream, agentRepository);
        assertTrue(stateStub.handledConnectionClosing());
    }

    @Test
    public void cancelAction_agent_triggersCancelInStateAndUpdatesIt() throws Exception {
        agent.cancelAction();

        assertTrue(stateStub.cancelledAction());
    }

    private AgentControl mockMessage() {
        return AgentControl.getDefaultInstance();
    }

    private class UnrecognizedStateStub extends UnrecognizedState {

        private boolean wasPostActionCalled;
        private boolean executedBackup;
        private boolean executedRestore;
        private boolean handledConnectionClosing;
        private boolean cancelledAction;
        private CreateBackupJob backupJob;

        public UnrecognizedStateStub() {
            super(null);
        }

        @Override
        public AgentStateChange processMessage(final AgentControl message) {
            return new AgentStateChange(this, () -> wasPostActionCalled = true);
        }

        @Override
        public AgentStateChange prepareForBackup(final AgentInputStream inputStream, final CreateBackupJob job) {
            executedBackup = true;
            backupJob = job;
            return new AgentStateChange(this, () -> wasPostActionCalled = true);
        }

        @Override
        public AgentStateChange executeRestore(final AgentInputStream inputStream) {
            executedRestore = true;
            return new AgentStateChange(this, () -> wasPostActionCalled = true);
        }

        @Override
        public void handleClosedConnection() {
            handledConnectionClosing = true;
        }

        @Override
        public AgentStateChange cancelAction(final AgentInputStream inputStream) {
            cancelledAction = true;
            return new AgentStateChange(this);
        }

        public boolean wasPostActionCalled() {
            return wasPostActionCalled;
        }

        public boolean executedBackup() {
            return executedBackup;
        }

        public boolean executedRestore() {
            return executedRestore;
        }

        public boolean handledConnectionClosing() {
            return handledConnectionClosing;
        }

        public boolean cancelledAction() {
            return cancelledAction;
        }

        public CreateBackupJob getBackupJob() {
            return backupJob;
        }

    }

}
