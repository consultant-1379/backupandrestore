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

import com.ericsson.adp.mgmt.backupandrestore.agent.state.PostActionRestoreState;
import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.control.StageComplete;

public class PostActionRestoreJobStageTest {

    private PostActionRestoreJobStage postActionRestoreJobStage;
    private NotificationService notificationService;
    private List<Agent> agents;
    private Action action;
    private MockedAgentFactory agentMock;
    private PostActionRestoreState postActionRestoreState;
    private RestoreJob job;

    @Before
    public void setup() {
        action = createMock(Action.class);
        agentMock = new MockedAgentFactory();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(action.getBackupName()).andReturn("qwe");
        job = createMock(RestoreJob.class);
        expect(job.getAction()).andReturn(action).anyTimes();
        expect(job.getBackupManagerId()).andReturn("abc").anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        postActionRestoreState = createMock(PostActionRestoreState.class);
        notificationService = createMock(NotificationService.class);

        replay(action, job);

        agents = Arrays.asList(agentMock.mockedAgent(1, Agent::executeRestorePostAction, postActionRestoreState, false), agentMock.mockedAgent(2, Agent::executeRestorePostAction, postActionRestoreState, false));
        postActionRestoreJobStage = new PostActionRestoreJobStage(agents, job, notificationService);
    }

    @Test
    public void trigger_agents_triggersAgents() throws Exception {
        replay(notificationService);

        postActionRestoreJobStage.trigger();

        verify(notificationService);
        verify(agents.get(0));
        verify(agents.get(1));
    }

    @Test
    public void getNextStage_hasUnsuccessfulAgents_nextStageIsFailed() throws Exception {
        agents = Arrays.asList(agentMock.mockedAgent(1, Agent::executeRestorePostAction, postActionRestoreState, true), agentMock.mockedAgent(2, Agent::executeRestorePostAction, postActionRestoreState, true));
        postActionRestoreJobStage = new PostActionRestoreJobStage(agents, job, notificationService);
        notificationService.notifyAllActionFailed(action);
        expectLastCall();
        replay(notificationService);

        postActionRestoreJobStage.handleAgentDisconnecting("1");
        postActionRestoreJobStage.updateAgentProgress("2", getMessage(true));
        final JobStage<RestoreJob> restoreJobJobStage = postActionRestoreJobStage.changeStages();
        restoreJobJobStage.trigger();
        Assert.assertEquals(restoreJobJobStage.getClass(), FailedRestoreJobStage.class);

        verify(notificationService);
    }

    @Test
    public void getNextStage_allAgentsSuccessful_nextStageIsCompleted() throws Exception {
        replay(notificationService);

        postActionRestoreJobStage.updateAgentProgress("1", getMessage(true));
        postActionRestoreJobStage.updateAgentProgress("2", getMessage(true));
        final JobStage<?> restoreJobJobStage = postActionRestoreJobStage.changeStages();
        restoreJobJobStage.trigger();
        assertEquals(restoreJobJobStage.getClass(), CompletedRestoreJobStage.class);
        verify(notificationService);
    }

    @Test
    public void isJobFinished_executingRestoreJobStage_false() throws Exception {
        assertFalse(postActionRestoreJobStage.isJobFinished());
    }

    private StageComplete getMessage(final boolean success) {
        return StageComplete.newBuilder().setSuccess(success).build();
    }

}
