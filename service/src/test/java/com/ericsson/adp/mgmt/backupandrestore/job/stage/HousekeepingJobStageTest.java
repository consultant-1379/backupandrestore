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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.HousekeepingJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;

public class HousekeepingJobStageTest {

    @Test
    public void moveToFailedStage_HousekeepingStageWithSomeProgress_movesToFailedStage() throws Exception {
        final Action action = createMock(Action.class);
        final HousekeepingJob job = createMock(HousekeepingJob.class);
        expect(job.getAction()).andReturn(action);
        expect(job.getBackupManagerId()).andReturn("1");

        final NotificationService notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionFailed(action);
        expectLastCall();
        replay(job, notificationService);
        final TestHousekeepingJobStage jobStage = new TestHousekeepingJobStage(new ArrayList<>(), job, notificationService);
        assertTrue(jobStage.moveToFailedStage() instanceof FailedHousekeepingJobStage);

    }

    private class TestHousekeepingJobStage extends HousekeepingJobStage {

        public TestHousekeepingJobStage(final List<Agent> agents, final HousekeepingJob job, final NotificationService notificationService) {
            super(agents, job, notificationService);
        }

        @Override
        protected void handleTrigger() {
            //do nothing
        }

        @Override
        protected JobStage<HousekeepingJob> getNextStageSuccess() {
            return this;
        }

        @Override
        protected JobStage<HousekeepingJob> getNextStageFailure() {
            return this;
        }

        @Override
        protected int getStageOrder() {
            return 1;
        }

        @Override
        public JobStageName getStageName() {
            //Not needed for this negative test
            return null;
        }
    }

}
