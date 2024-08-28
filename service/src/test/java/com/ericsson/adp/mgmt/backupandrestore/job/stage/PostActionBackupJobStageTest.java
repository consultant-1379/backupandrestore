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
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.agent.state.RecognizedState;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import com.ericsson.adp.mgmt.control.StageComplete;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class PostActionBackupJobStageTest {

    private PostActionBackupJobStage postActionBackupJobStage;
    private NotificationService notificationService;
    private List<Agent> agents;
    private Action action;
    private MockedAgentFactory agentMock;
    private RecognizedState recognizedState;
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

        recognizedState = createMock(RecognizedState.class);
        notificationService = createMock(NotificationService.class);

        replay(action, job);

        agents = Arrays.asList(agentMock.mockedAgentBackupPostAction(1,API_V2_0, false), agentMock.mockedAgentBackupPostAction(2,API_V3_0, false));
        postActionBackupJobStage = new PostActionBackupJobStage(agents, job, notificationService);
    }

    @Test
    public void trigger_postActionBackupJobStage_triggersAgents() throws Exception {
        replay(notificationService);

        postActionBackupJobStage.trigger();

        verify(notificationService);
        verify(agents.get(0));
        verify(agents.get(1));
    }

    @Test
    public void getNextState_hasUnsuccessfulAgents_nextStageIsFailed() throws Exception {
        agents = Arrays.asList(agentMock.mockedAgentBackupPostAction(1,API_V2_0, true), agentMock.mockedAgentBackupPostAction(2,API_V3_0, true));
        postActionBackupJobStage = new PostActionBackupJobStage(agents, job, notificationService);
        notificationService.notifyAllActionFailed(action);
        expectLastCall();
        replay(notificationService);

        postActionBackupJobStage.trigger();
        postActionBackupJobStage.updateAgentProgress("2", getMessage(false));
        final JobStage<CreateBackupJob> createBackupJobJobStage = postActionBackupJobStage.changeStages();
        createBackupJobJobStage.trigger();
        assertEquals(createBackupJobJobStage.getClass(), FailedBackupJobStage.class);

        verify(notificationService);
    }

    @Test
    public void getNextState_allAgentsSuccessful_nextStageIsCompleted() throws Exception {
        postActionBackupJobStage.trigger();
        postActionBackupJobStage.updateAgentProgress("2", getMessage(true));
        assertEquals(postActionBackupJobStage.changeStages().getClass(), CompletedBackupJobStage.class);
    }

    @Test
    public void isJobFinished_postActionBackupJobStage_false() throws Exception {
        assertFalse(postActionBackupJobStage.isJobFinished());
    }

    @Test(expected=BackupServiceException.class)
    public void receiveNewFragment_postActionBackupJobStage_throwsException() {
        postActionBackupJobStage.receiveNewFragment("abc", "123");
    }

    private StageComplete getMessage(final boolean success) {
        return StageComplete.newBuilder().setSuccess(success).build();
    }
}
