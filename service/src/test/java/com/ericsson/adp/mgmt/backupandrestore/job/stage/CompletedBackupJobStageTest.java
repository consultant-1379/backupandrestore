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
import com.ericsson.adp.mgmt.backupandrestore.exception.BackupServiceException;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;

public class CompletedBackupJobStageTest {

    private CompletedBackupJobStage completedBackupJobStage;
    private NotificationService notificationService;
    private List<Agent> agents;
    private MockedAgentFactory agentMock;

    @Before
    public void setup() {
        agentMock = new MockedAgentFactory();
        final Action action = createMock(Action.class);
        final CreateBackupJob job = createMock(CreateBackupJob.class);
        expect(job.getAction()).andReturn(action);
        expect(action.getBackupName()).andReturn("backup");
        expect(action.getActionId()).andReturn("1111").anyTimes();

        notificationService = createMock(NotificationService.class);

        replay(job, notificationService);

        agents = Arrays.asList(agentMock.mockedAgent(1), agentMock.mockedAgent(2));
        completedBackupJobStage = new CompletedBackupJobStage(agents, job, notificationService);
    }

    @Test
    public void trigger_completedBackupJobStage_doesNothing() throws Exception {
        completedBackupJobStage.trigger();

        verify(notificationService);
    }

    @Test
    public void isJobFinished_completedBackupJobStage_true() throws Exception {
        assertTrue(completedBackupJobStage.isJobFinished());
    }

    @Test
    public void handleAgentDisconnecting_completedBackupJobStage_staysInTheSameStage() throws Exception {
        completedBackupJobStage.handleAgentDisconnecting("1");
        assertEquals(completedBackupJobStage, completedBackupJobStage.changeStages());
    }

    @Test
    public void handleUnexpectedDataChannel_completedBackupJobStage_staysInTheSameStage() throws Exception {
        completedBackupJobStage.handleUnexpectedDataChannel("1");
        assertEquals(completedBackupJobStage, completedBackupJobStage.changeStages());
    }

    @Test(expected=BackupServiceException.class)
    public void receiveNewFragment_completedBackupJobStage_throwsException() {
        completedBackupJobStage.receiveNewFragment("abc", "123");
    }

    @Test
    public void fragmentFailed_completedBackupJobStage_staysInTheSameStage() {
        completedBackupJobStage.fragmentFailed("abc", "1");
        assertEquals(completedBackupJobStage, completedBackupJobStage.changeStages());
    }

    public void fragmentSucceeded_completedBackupJobStage_staysInTheSameStage() {
        completedBackupJobStage.fragmentSucceeded("abc", "1");
        assertEquals(completedBackupJobStage, completedBackupJobStage.changeStages());
    }

    @Test
    public void getProgressPercentage_completedBackupJobStage_one() throws Exception {
        assertEquals(Double.valueOf(1.0d), Double.valueOf(completedBackupJobStage.getProgressPercentage()));
    }

}
