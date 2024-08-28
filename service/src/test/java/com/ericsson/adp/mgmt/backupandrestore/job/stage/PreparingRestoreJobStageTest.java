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

import com.ericsson.adp.mgmt.backupandrestore.agent.state.PreparingRestoreState;
import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.control.StageComplete;

public class PreparingRestoreJobStageTest {

    private PreparingRestoreJobStage preparingRestoreJobStage;
    private NotificationService notificationService;
    private List<Agent> agents;
    private Action action;
    private MockedAgentFactory agentMock;
    private PreparingRestoreState preparingRestoreState;
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

        replay(action, job);
        preparingRestoreState = createMock(PreparingRestoreState.class);
        agents = Arrays.asList(agentMock.mockedAgent(1, job, preparingRestoreState, false), agentMock.mockedAgent(2, job, preparingRestoreState, false));
        preparingRestoreJobStage = new PreparingRestoreJobStage(agents, job, notificationService);
    }

    @Test
    public void trigger_agents_triggersAgentsAndSendsNotification() throws Exception {
        notificationService.notifyAllActionStarted(action);
        expectLastCall();
        replay(notificationService);

        preparingRestoreJobStage.trigger();

        verify(notificationService);
        verify(agents.get(0));
        verify(agents.get(1));
    }

    @Test
    public void getNextState_hasUnsuccessfulAgents_nextStageIsFailed() throws Exception {
        agents = Arrays.asList(agentMock.mockedAgent(1, job, preparingRestoreState, true), agentMock.mockedAgent(2, job, preparingRestoreState, true));
        preparingRestoreJobStage = new PreparingRestoreJobStage(agents, job, notificationService);
        notificationService.notifyAllActionFailed(action);
        expectLastCall();
        replay(notificationService);

        preparingRestoreJobStage.handleAgentDisconnecting("1");
        preparingRestoreJobStage.updateAgentProgress("2", getMessage(true));
        final JobStage<?> restoreJobStage = preparingRestoreJobStage.changeStages();
        restoreJobStage.trigger();
        assertEquals(FailedRestoreJobStage.class, restoreJobStage.getClass());

        verify(notificationService);
    }

    @Test
    public void getNextState_allAgentsSuccessful_nextStageIsExecution() throws Exception {
        preparingRestoreJobStage.updateAgentProgress("1", getMessage(true));
        preparingRestoreJobStage.updateAgentProgress("2", getMessage(true));
        assertEquals(ExecutingRestoreJobStage.class, preparingRestoreJobStage.changeStages().getClass());
    }

    @Test
    public void isJobFinished_preparingRestoreJobStage_false() throws Exception {
        assertFalse(preparingRestoreJobStage.isJobFinished());
    }

    private StageComplete getMessage(final boolean success) {
        return StageComplete.newBuilder().setSuccess(success).build();
    }

}
