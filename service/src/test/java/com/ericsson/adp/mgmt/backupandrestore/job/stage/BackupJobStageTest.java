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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;

public class BackupJobStageTest {

    @Test
    public void moveToFailedStage_backupStageWithSomeProgressPercentage_movesToFailedStageWithSameProgressPercentage() throws Exception {
        final Action action = createMock(Action.class);
        final CreateBackupJob job = createMock(CreateBackupJob.class);
        expect(job.getAction()).andReturn(action);

        final NotificationService notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionFailed(action);
        expectLastCall();

        replay(job, notificationService);

        final TestBackupJobStage jobStage = new TestBackupJobStage(new ArrayList<>(), job, notificationService);

        assertTrue(jobStage.moveToFailedStage() instanceof FailedBackupJobStage);

        verify(notificationService);
    }

    private class TestBackupJobStage extends BackupJobStage {

        public TestBackupJobStage(final List<Agent> agents, final CreateBackupJob job, final NotificationService notificationService) {
            super(agents, job, notificationService);
        }

        @Override
        protected void handleTrigger() {
            //Not needed
        }

        @Override
        protected JobStage<CreateBackupJob> getNextStageSuccess() {
            return this;
        }

        @Override
        protected JobStage<CreateBackupJob> getNextStageFailure() {
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
