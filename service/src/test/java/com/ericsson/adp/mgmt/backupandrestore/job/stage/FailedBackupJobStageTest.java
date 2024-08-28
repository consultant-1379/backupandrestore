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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.agent.state.CancelingActionState;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.progress.AgentProgress;
import com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import com.ericsson.adp.mgmt.control.StageComplete;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class FailedBackupJobStageTest {

    private FailedBackupJobStage failedBackupJobStage;
    private NotificationService notificationService;
    private List<Agent> agents;
    private MockedAgentFactory agentMock;

    @Before
    public void setup() {
        final Action action = createMock(Action.class);
        agentMock = new MockedAgentFactory();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        final CreateBackupJob job = createMock(CreateBackupJob.class);
        expect(job.getAction()).andReturn(action).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(job.getBackupManagerId()).andReturn("abc").anyTimes();

        notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionFailed(action);
        expectLastCall();

        replay(action, job, notificationService);

        CancelingActionState cancelingActionState = createMock(CancelingActionState.class);
        agents = Arrays.asList(agentMock.mockedAgent(1, true, cancelingActionState), agentMock.mockedAgent(2, false, cancelingActionState));
        final HashMap<String, AgentProgress> agentProgress = new HashMap<>();

        agentProgress.put("1", new AgentProgress());
        agentProgress.get("1").setProgress(Progress.FAILED);
        agentProgress.put("2", new AgentProgress());
        agentProgress.get("2").setProgress(Progress.DISCONNECTED);

        failedBackupJobStage = new FailedBackupJobStage(agents, job, notificationService, 0.376d, agentProgress);
    }

    @Test
    public void trigger_failedBackupJobStage_sendsNotificationAndCancelsActionsFromStillConnectedAgents() throws Exception {
        failedBackupJobStage.trigger();

        verify(notificationService);

        verify(agents.get(0));
        verify(agents.get(1));
    }

    @Test
    public void isJobFinished_stillWaitingOnStageCompleteForSomeAgents_false() throws Exception {
        failedBackupJobStage.trigger();
        failedBackupJobStage.changeStages();
        failedBackupJobStage.updateAgentProgress("1", getMessage(true));
        assertTrue(failedBackupJobStage.isJobFinished());
    }

    @Test
    public void updateAgentProgress_lastCancelStageCompleteComesIn_staysInTheSameStageAndIndicatesJobIsFinished() throws Exception {
        failedBackupJobStage.trigger();
        failedBackupJobStage.updateAgentProgress("1", getMessage(true));
        assertEquals(failedBackupJobStage.getClass(), FailedBackupJobStage.class);
        assertTrue(failedBackupJobStage.isJobFinished());
    }

    @Test
    public void handleAgentDisconnecting_failedBackupJobStage_staysInTheSameStageAndIndicatesJobIsFinished() throws Exception {
        failedBackupJobStage.handleAgentDisconnecting("1");
        assertEquals(failedBackupJobStage,failedBackupJobStage.changeStages());
        assertTrue(failedBackupJobStage.isJobFinished());
    }

    @Test
    public void moveToFailedStage_failedBackupJobStage_staysInTheSameStage() throws Exception {
        assertEquals(failedBackupJobStage, failedBackupJobStage.moveToFailedStage());
    }

    @Test
    public void handleUnexpectedDataChannel_failedBackupJobStage_staysInTheSameStage() throws Exception {
        failedBackupJobStage.handleUnexpectedDataChannel("1");
        assertEquals(failedBackupJobStage, failedBackupJobStage.changeStages());
    }

    @Test
    public void getProgressPercentage_failedBackupJobStage_returnsValueFromWhenJobFailed() throws Exception {
        assertEquals(Double.valueOf(0.376d), Double.valueOf(failedBackupJobStage.getProgressPercentage()));
    }

    @Test(expected=BackupServiceException.class)
    public void receiveNewFragment_completedBackupJobStage_throwsException() {
        failedBackupJobStage.receiveNewFragment("abc", "123");
    }

    @Test
    public void fragmentFailed_failedBackupJobStage_staysInTheSameStage() {
        failedBackupJobStage.fragmentFailed("abc", "1");
        assertEquals(failedBackupJobStage, failedBackupJobStage.changeStages());
    }

    @Test
    public void fragmentSucceeded_failedBackupJobStage_staysInTheSameStage() {
        failedBackupJobStage.fragmentSucceeded("abc", "1");
        assertEquals(failedBackupJobStage, failedBackupJobStage.changeStages());
    }

    @Test
    public void test_idsOfAgentsInProgress() {
        failedBackupJobStage.trigger();
        assertEquals(Arrays.asList("1"), failedBackupJobStage.idsOfAgentsInProgress());
        assertEquals(1, failedBackupJobStage.idsOfAgentsInProgress().size());
        failedBackupJobStage.updateAgentProgress("1", getMessage(true));
        assertEquals(0, failedBackupJobStage.idsOfAgentsInProgress().size());
    }
    private StageComplete getMessage(final boolean success) {
        return StageComplete.newBuilder().setSuccess(success).build();
    }
}
