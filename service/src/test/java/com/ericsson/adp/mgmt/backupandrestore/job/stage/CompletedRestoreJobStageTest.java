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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.control.StageComplete;

public class CompletedRestoreJobStageTest {

    private CompletedRestoreJobStage completedRestoreJobStage;
    private NotificationService notificationService;
    private List<Agent> agents;
    private MockedAgentFactory agentMock;

    @Before
    public void setup() {
        final Action action = createMock(Action.class);
        final RestoreJob job = createMock(RestoreJob.class);
        agentMock = new MockedAgentFactory();
        expect(job.getAction()).andReturn(action);
        expect(action.getBackupName()).andReturn("backup");

        notificationService = createMock(NotificationService.class);

        replay(job, notificationService);

        agents = Arrays.asList(agentMock.mockedAgent(1), agentMock.mockedAgent(2));
        completedRestoreJobStage = new CompletedRestoreJobStage(agents, job, notificationService);
    }

    @Test
    public void trigger_completedRestoreJobStage_doesNothing() throws Exception {
        completedRestoreJobStage.trigger();
        verify(notificationService);

        verify(notificationService);
        verify(agents.get(0));
        verify(agents.get(1));
    }

    @Test
    public void isJobFinished_completedRestoreJobStage_true() throws Exception {
        assertTrue(completedRestoreJobStage.isJobFinished());
    }

    @Test
    public void updateAgentProgress_completedRestoreJobStage_staysInTheSameStage() throws Exception {
        completedRestoreJobStage.updateAgentProgress("1", StageComplete.getDefaultInstance());
        assertEquals(completedRestoreJobStage, completedRestoreJobStage.changeStages());
    }

    @Test
    public void handleAgentDisconnecting_completedRestoreJobStage_staysInTheSameStage() throws Exception {
        completedRestoreJobStage.handleAgentDisconnecting("1");
        assertEquals(completedRestoreJobStage, completedRestoreJobStage.changeStages());
    }

    @Test
    public void getProgressPercentage_completedRestoreJobStage_one() throws Exception {
        assertEquals(Double.valueOf(1.0d), Double.valueOf(completedRestoreJobStage.getProgressPercentage()));
    }


}
