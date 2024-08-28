/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.job.stage;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V2_0;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.agent.state.ExecutingBackupState;
import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.control.StageComplete;

public class ExecutingBackupJobStageTest {

    private ExecutingBackupJobStage executingBackupJobStage;
    private NotificationService notificationService;
    private List<Agent> agents;
    private Action action;
    private MockedAgentFactory agentMock;
    private ExecutingBackupState executingBackupState;
    private CreateBackupJob job;

    @Before
    public void setup() {
        action = createMock(Action.class);
        agentMock = new MockedAgentFactory();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        job = createMock(CreateBackupJob.class);
        expect(job.getAction()).andReturn(action).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(job.getBackupManagerId()).andReturn("abc").anyTimes();

        notificationService = createMock(NotificationService.class);
        executingBackupState = createMock(ExecutingBackupState.class);
        replay(action, job);

        agents = Arrays.asList(agentMock.mockedAgentExecutingBackup(1, API_V3_0, executingBackupState, false), agentMock.mockedAgentExecutingBackup(2, API_V2_0, executingBackupState, false));
        executingBackupJobStage = new ExecutingBackupJobStage(agents, job, notificationService);
    }

    @Test
    public void trigger_executingBackupJobStage_triggersAgents() throws Exception {
        replay(notificationService);

        executingBackupJobStage.trigger();

        verify(notificationService);
        verify(agents.get(0));
        verify(agents.get(1));
    }

    @Test
    public void getNextState_hasUnsuccessfulAgents_nextStageIsFailed() throws Exception {
        agents = Arrays.asList(agentMock.mockedAgentExecutingBackup(1, API_V3_0, executingBackupState, true), agentMock.mockedAgentExecutingBackup(2, API_V2_0, executingBackupState, true));
        executingBackupJobStage = new ExecutingBackupJobStage(agents, job, notificationService);
        notificationService.notifyAllActionFailed(action);
        expectLastCall();
        replay(notificationService);

        executingBackupJobStage.trigger();
        executingBackupJobStage.updateAgentProgress("1",getMessage(false));
        executingBackupJobStage.updateAgentProgress("2", getMessage(false));
        final JobStage<CreateBackupJob> jobStage = executingBackupJobStage.changeStages();
        jobStage.trigger();
        assertEquals(jobStage.getClass(), FailedBackupJobStage.class);

        verify(notificationService);
    }

    @Test
    public void getNextState_allAgentsSuccessful_movesToPostActionBackupJobStage() throws Exception {
        executingBackupJobStage.trigger();
        executingBackupJobStage.updateAgentProgress("1", getMessage(true));
        executingBackupJobStage.updateAgentProgress("2", getMessage(true));
        final JobStage<CreateBackupJob> jobStage = executingBackupJobStage.changeStages();
        jobStage.trigger();
        assertEquals(jobStage.getClass(), PostActionBackupJobStage.class);
    }

    @Test
    public void isJobFinished_executingRestoreJobStage_false() throws Exception {
        assertFalse(executingBackupJobStage.isJobFinished());
    }

    @Test
    public void receiveNewFragment_agentFinishedAndReceiveOneFragment_didNotMoveToNextStage() {
        executingBackupJobStage.trigger();
        executingBackupJobStage.receiveNewFragment("2", "fragmentA");

        executingBackupJobStage.updateAgentProgress("2", getMessage(true));
        final JobStage<CreateBackupJob> nextStage = executingBackupJobStage.changeStages();
        assertEquals(nextStage.getClass(), ExecutingBackupJobStage.class);
        assertFalse(executingBackupJobStage.isStageSuccessful());
    }

    @Test
    public void fragmentSucceeded_agentFinishedAndFragmentSucceeded_stageSuccessfulAndMovesToPostActionBackupJobStage() {
        executingBackupJobStage.trigger();
        executingBackupJobStage.receiveNewFragment("2", "fragmentA");
        executingBackupJobStage.fragmentSucceeded("2", "fragmentA");

        executingBackupJobStage.updateAgentProgress("1", getMessage(true));
        executingBackupJobStage.updateAgentProgress("2", getMessage(true));
        final JobStage<CreateBackupJob> nextStage = executingBackupJobStage.changeStages();

        assertEquals(nextStage.getClass(), PostActionBackupJobStage.class);
        assertTrue(executingBackupJobStage.isStageSuccessful());
    }

    @Test
    public void fragmentFailed_agentFinishedAndFragmentFailed_stageFailedAndMovesToFailedBackupJobStage() {
        executingBackupJobStage.trigger();
        executingBackupJobStage.receiveNewFragment("2", "fragmentA");
        executingBackupJobStage.fragmentFailed("2", "fragmentA");

        executingBackupJobStage.updateAgentProgress("1", getMessage(true));
        executingBackupJobStage.updateAgentProgress("2", getMessage(true));
        final JobStage<CreateBackupJob> nextStage = executingBackupJobStage.changeStages();
        assertEquals(nextStage.getClass(), FailedBackupJobStage.class);

        assertFalse(executingBackupJobStage.isStageSuccessful());
    }

    private StageComplete getMessage(final boolean success) {
        return StageComplete.newBuilder().setSuccess(success).build();
    }
}
