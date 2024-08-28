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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.agent.state.RecognizedState;
import com.ericsson.adp.mgmt.backupandrestore.agent.state.RestoreState;
import com.ericsson.adp.mgmt.backupandrestore.notification.Notification;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationFailedException;
import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.job.progress.AgentProgress;
import com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.control.StageComplete;

public class FailedRestoreJobStageTest {

    private FailedRestoreJobStage failedRestoreJobStage;
    private NotificationService notificationService;
    private List<Agent> agents;
    private Notification notification;
    private Action action;
    private RestoreJob job;
    private HashMap<String, AgentProgress> agentProgress;
    private MockedAgentFactory agentMock;
    private RestoreState restoreState;

    @Before
    public void setup() {
        action = createMock(Action.class);
        job = createMock(RestoreJob.class);
        agentMock = new MockedAgentFactory();
        restoreState = createMock(RestoreState.class);
        notificationService = createMock(NotificationService.class);

        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(job.getAction()).andReturn(action).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(job.getBackupManagerId()).andReturn("qwe").anyTimes();

    }

    @Test
    public void trigger_failedRestoreJobStage_sendsNotificationAndCancelsActionsFromStillConnectedAgents() throws Exception {
        setupWillThrowErrorWhenSendNotification(false, restoreState);

        failedRestoreJobStage.trigger();

        verify(notificationService);
        verify(agents.get(0));
        verify(agents.get(1));
    }

    @Test
    public void trigger_failedRestoreJobStage_failedToSendsNotificationAndCancelsActionsFromStillConnectedAgents() throws Exception {
        setupWillThrowErrorWhenSendNotification(true, restoreState);
        failedRestoreJobStage = new FailedRestoreJobStage(agents, job, notificationService, 0.376d, agentProgress);
        failedRestoreJobStage.trigger();
        verify(notificationService);
        verify(agents.get(0));
        verify(agents.get(1));
    }

    @Test
    public void isJobFinished_stillWaitingOnStageCompleteForSomeAgents_false() throws Exception {
        setupWillThrowErrorWhenSendNotification(false, restoreState);
        failedRestoreJobStage.trigger();
        failedRestoreJobStage.changeStages();
        failedRestoreJobStage.updateAgentProgress("1", getMessage(true));
        assertTrue(failedRestoreJobStage.isJobFinished());
    }

   @Test
    public void updateAgentProgress_lastCancelStageCompleteComesIN_staysInTheSameStageAndIndicatesJobIsFinished() throws Exception {
        setupWillThrowErrorWhenSendNotification(false, restoreState);
        failedRestoreJobStage.trigger();
        failedRestoreJobStage.updateAgentProgress("1", getMessage(true));
        assertEquals(failedRestoreJobStage.getClass(), FailedRestoreJobStage.class);
        assertTrue(failedRestoreJobStage.isJobFinished());
    }

    @Test
    public void handleAgentDisconnecting_lastCancellingAgentDisconnects_staysInTheSameStageAndIndicatesJobIsFinished() throws Exception {
        setupWillThrowErrorWhenSendNotification(false, restoreState);
        failedRestoreJobStage.trigger();
        failedRestoreJobStage.handleAgentDisconnecting("1");
        assertEquals(failedRestoreJobStage, failedRestoreJobStage.changeStages());
        assertTrue(failedRestoreJobStage.isJobFinished());
    }

    @Test
    public void moveToFailedStage_failedRestoreJobStage_staysInTheSameStage() throws Exception {
        setupWillThrowErrorWhenSendNotification(false, restoreState);
        assertEquals(failedRestoreJobStage, failedRestoreJobStage.moveToFailedStage());
    }

    @Test
    public void getProgressPercentage_failedRestoreJobStage_returnsValueFromWhenJobFailed() throws Exception {
        setupWillThrowErrorWhenSendNotification(false, restoreState);
        assertEquals(Double.valueOf(0.376d), Double.valueOf(failedRestoreJobStage.getProgressPercentage()));
    }

    @Test
    public void test_idsOfAgentsInProgress() {
        setupWillThrowErrorWhenSendNotification(false, restoreState);
        failedRestoreJobStage.trigger();
        assertEquals(Arrays.asList("1"), failedRestoreJobStage.idsOfAgentsInProgress());
        assertEquals(1, failedRestoreJobStage.idsOfAgentsInProgress().size());
        failedRestoreJobStage.updateAgentProgress("1", getMessage(true));
        assertEquals(0, failedRestoreJobStage.idsOfAgentsInProgress().size());
    }

    private void setupWillThrowErrorWhenSendNotification(final boolean throwsException, final RecognizedState agentStateWhenFail) {
        notificationService.notifyAllActionFailed(action);
        if (throwsException) {
            expectLastCall().andThrow(new NotificationFailedException(notification, new Exception()));
        } else {
            expectLastCall();
        }

        replay(action, job, notificationService);
        agents = Arrays.asList(agentMock.mockedAgent(1, true, agentStateWhenFail), agentMock.mockedAgent(2, false, agentStateWhenFail));
        agentProgress = new HashMap<>();

        agentProgress.put("1", new AgentProgress());
        agentProgress.get("1").setProgress(Progress.FAILED);
        agentProgress.put("2", new AgentProgress());
        agentProgress.get("2").setProgress(Progress.DISCONNECTED);

        failedRestoreJobStage = new FailedRestoreJobStage(agents, job, notificationService, 0.376d, agentProgress);
    }

    private StageComplete getMessage(final boolean success) {
        return StageComplete.newBuilder().setSuccess(success).build();
    }

}
