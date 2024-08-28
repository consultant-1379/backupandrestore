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
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress;
import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.control.StageComplete;

public class PreparingBackupJobStageTest {
    private PreparingBackupJobStage preparingBackupJobStage;
    private NotificationService notificationService;
    private List<Agent> agents;
    private Action action;
    private MockedAgentFactory agentMock;

    @Before
    public void setup() {
        action = createMock(Action.class);
        agentMock = new MockedAgentFactory();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        final CreateBackupJob job = createMock(CreateBackupJob.class);
        expect(job.getAction()).andReturn(action).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(job.getBackupManagerId()).andReturn("abc").anyTimes();

        notificationService = createMock(NotificationService.class);

        replay(action, job);

        agents = Arrays.asList(agentMock.mockedAgent(1, job, ApiVersion.API_V3_0), agentMock.mockedAgent(2, job, ApiVersion.API_V2_0));
        preparingBackupJobStage = new PreparingBackupJobStage(agents, job, notificationService);
    }

    @Test
    public void trigger_agents_triggersAgentsAndSendsNotification() throws Exception {
        notificationService.notifyAllActionStarted(action);
        expectLastCall();
        replay(notificationService);

        preparingBackupJobStage.trigger();

        verify(notificationService);
        verify(agents.get(0));
        verify(agents.get(1));
    }

    @Test
    public void getNextState_hasUnsuccessfulAgents_nextStageIsFailed() throws Exception {
        notificationService.notifyAllActionFailed(action);
        expectLastCall();
        replay(notificationService);

        preparingBackupJobStage.handleAgentDisconnecting("1");
        preparingBackupJobStage.updateAgentProgress("2", getMessage(true));
        final JobStage<?> backupJobStage = preparingBackupJobStage.changeStages();
        backupJobStage.trigger();
        assertEquals(FailedBackupJobStage.class, backupJobStage.getClass());

        verify(notificationService);
    }

    @Test
    public void getNextState_allAgentsSuccessful_nextStageIsExecution() throws Exception {
        preparingBackupJobStage.updateAgentProgress("1", getMessage(true));
        preparingBackupJobStage.updateAgentProgress("2", getMessage(true));
        final JobStage<?> backupJobStage = preparingBackupJobStage.changeStages();
        assertEquals(ExecutingBackupJobStage.class, backupJobStage.getClass());
    }

    @Test
    public void isJobFinished_preparingBackupJobStage_false() throws Exception {
        assertFalse(preparingBackupJobStage.isJobFinished());
    }

    @Test(expected=BackupServiceException.class)
    public void receiveNewFragment_preparingBackupJobStageApiV3_throwsException() {
            preparingBackupJobStage.receiveNewFragment("1", "123");
    }

    @Test
    public void receiveNewFragment_preparingBackupJobStageApiV2_fragmentHandled() {
        preparingBackupJobStage.receiveNewFragment("2", "456");
        assertEquals(Progress.WAITING_RESULT, preparingBackupJobStage.agentProgress.get("2").getProgress());
    }

    private StageComplete getMessage(final boolean success) {
        return StageComplete.newBuilder().setSuccess(success).build();
    }

}
