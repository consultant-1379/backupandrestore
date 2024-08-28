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
package com.ericsson.adp.mgmt.backupandrestore.job.stage;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import java.util.Arrays;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.agent.state.ExecutingRestoreState;
import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import org.junit.Before;
import org.junit.Test;
import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.control.StageComplete;

public class ExecutingRestoreJobStageTest {

    private ExecutingRestoreJobStage executingRestoreJobStage;
    private NotificationService notificationService;
    private List<Agent> agents;
    private Action action;
    private MockedAgentFactory agentMock;
    private ExecutingRestoreState executingRestoreState;
    private RestoreJob job;

    @Before
    public void setup() {
        action = createMock(Action.class);
        agentMock = new MockedAgentFactory();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        job = createMock(RestoreJob.class);
        expect(job.getAction()).andReturn(action).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(job.getBackupManagerId()).andReturn("abc").anyTimes();

        notificationService = createMock(NotificationService.class);
        executingRestoreState = createMock(ExecutingRestoreState.class);

        replay(action, job);

        agents = Arrays.asList(agentMock.mockedAgent(1, Agent::executeRestore, executingRestoreState, false), agentMock.mockedAgent(2, Agent::executeRestore, executingRestoreState, false));
        executingRestoreJobStage = new ExecutingRestoreJobStage(agents, job, notificationService);
    }

    @Test
    public void trigger_agents_triggersAgents() throws Exception {
        replay(notificationService);

        executingRestoreJobStage.trigger();

        verify(notificationService);
        verify(agents.get(0));
        verify(agents.get(1));
    }

    @Test
    public void getNextStage_hasUnsuccessfulAgents_nextStageIsFailed() throws Exception {
        agents = Arrays.asList(agentMock.mockedAgent(1, Agent::executeRestore, executingRestoreState, true), agentMock.mockedAgent(2, Agent::executeRestore, executingRestoreState, true));
        executingRestoreJobStage = new ExecutingRestoreJobStage(agents, job, notificationService);
        notificationService.notifyAllActionFailed(action);
        expectLastCall();
        replay(notificationService);

        executingRestoreJobStage.handleAgentDisconnecting("1");
        executingRestoreJobStage.updateAgentProgress("2", getMessage(true));
        JobStage<?> nextStage = executingRestoreJobStage.changeStages();
        nextStage.trigger();
        assertEquals(executingRestoreJobStage.changeStages().getClass(), FailedRestoreJobStage.class);

        verify(notificationService);
    }

    @Test
    public void getNextStage_allAgentsSuccessful_nextStageIsPostActions() throws Exception {
        replay(notificationService);

        executingRestoreJobStage.updateAgentProgress("1", getMessage(true));
        executingRestoreJobStage.updateAgentProgress("2", getMessage(true));
        assertEquals(executingRestoreJobStage.changeStages().getClass(), PostActionRestoreJobStage.class);

        verify(notificationService);
    }

    @Test
    public void isJobFinished_executingRestoreJobStage_false() throws Exception {
        assertFalse(executingRestoreJobStage.isJobFinished());
    }

    private StageComplete getMessage(final boolean success) {
        return StageComplete.newBuilder().setSuccess(success).build();
    }

}
