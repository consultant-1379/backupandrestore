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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;

public class RestoreJobStageTest {

    @Test
    public void moveToFailedStage_restoreStageWithSomeProgressPercentage_movesToFailedStageWithSameProgressPercentage() throws Exception {
        final Action action = createMock(Action.class);
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        final RestoreJob job = createMock(RestoreJob.class);
        expect(job.getAction()).andReturn(action).anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(job.getBackupManagerId()).andReturn("qwe").anyTimes();

        final NotificationService notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionFailed(action);
        expectLastCall();

        replay(action, job, notificationService);

        JobStage<RestoreJob> jobStage = new TestRestoreJobStage(new ArrayList<>(), job, notificationService);

        jobStage.trigger();
        jobStage = jobStage.changeStages();
        jobStage.trigger();

        assertEquals(jobStage.moveToFailedStage().getClass(), FailedRestoreJobStage.class);

        verify(notificationService);
    }

    private class TestRestoreJobStage extends RestoreJobStage {

        public TestRestoreJobStage(final List<Agent> agents, final RestoreJob job, final NotificationService notificationService) {
            super(agents, job, notificationService);
        }

        @Override
        protected void handleTrigger() {

        }

        @Override
        protected JobStage<RestoreJob> getNextStageSuccess() {
            return this;
        }

        @Override
        public boolean isJobFinished() {
            return true;
        }

        @Override
        public boolean isStageSuccessful() {
            return false;
        }

        @Override
        protected int getStageOrder() {
            return 1;
        }

        @Override
        public JobStageName getStageName() {
            return JobStageName.EXECUTION;
        }

    }

}
