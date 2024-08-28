/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.HousekeepingJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;

public class PostActionHousekeepingJobStageTest {

    private PostActionHousekeepingJobStage postActionHousekeepingJobStage;
    private NotificationService notificationService;
    private List<Agent> agents;
    private Action action;
    private MockedAgentFactory agentMock;

    @Before
    public void setup() {
        agentMock = new MockedAgentFactory();
        action = createMock(Action.class);
        final HousekeepingJob job = createMock(HousekeepingJob.class);
        expect(job.getAction()).andReturn(action).anyTimes();
        // job.updateStage(EasyMock.anyObject());
        expectLastCall();
        expect(job.getBackupManagerId()).andReturn("1").anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        notificationService = createMock(NotificationService.class);
        job.updateBackupManagerHousekeeping();
        expectLastCall().anyTimes();
        replay(job);

        agents = Arrays.asList(agentMock.mockedAgent(1), agentMock.mockedAgent(2));
        postActionHousekeepingJobStage = new PostActionHousekeepingJobStage(agents, job, notificationService);
    }

    @Test
    public void executeTrigger_validateIsCompleted() throws Exception {
        expect(action.getName()).andReturn(ActionType.HOUSEKEEPING).anyTimes();
        replay(action);
        postActionHousekeepingJobStage.trigger();
        assertTrue(postActionHousekeepingJobStage.isJobFinished());
    }

    @Test
    public void moveToNextStage_triggerisExecuted_nextStageIsCompleted() throws Exception {
        expect(action.getName()).andReturn(ActionType.HOUSEKEEPING).anyTimes();
        replay(action, notificationService);
        postActionHousekeepingJobStage.trigger();
        assertTrue(postActionHousekeepingJobStage.moveToNextStage() instanceof CompletedHousekeepingJobStage);
        verify(notificationService);
    }

    @Test
    public void postActionHousekeeping_moveToNextStage_nextStage() throws Exception {
        expect(action.getName()).andReturn(ActionType.HOUSEKEEPING);
        replay(action);
        assertTrue(postActionHousekeepingJobStage.moveToNextStage() instanceof FailedHousekeepingJobStage );
    }

}
