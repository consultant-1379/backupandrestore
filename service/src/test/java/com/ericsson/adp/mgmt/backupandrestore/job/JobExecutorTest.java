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
package com.ericsson.adp.mgmt.backupandrestore.job;

import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.CREATE_BACKUP;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.awaitility.Awaitility;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionFileService;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionRepository;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.exception.AnotherActionRunningException;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;

/**
 * These unit tests verifies that the JobExecutor
 * will accept and execute only one action at a time.
 */
public class JobExecutorTest {

    private JobExecutor jobExecutor;
    final JobFactory jobFactory = EasyMock.createMock(JobFactory.class);
    final ActionRepository actionRepository = EasyMock.createMock(ActionRepository.class);
    final ActionFileService actionFileService = EasyMock.createMock(ActionFileService.class);
    final CMMediatorService cmMediatorService = EasyMock.createMock(CMMediatorService.class);

    @Before
    public void setup() {
        /**
         * Only allow single action at a time through
         * setting isParallelActionsEnabled to false
         */
        jobExecutor = new QueueingJobExecutor(jobFactory,
                                              actionRepository,
                                              actionFileService,
                                              cmMediatorService,
                                              false);
    }

    @Test
    public void execute_backupManagerIdAndAction_createsJobAndRunsOnASeparateThread_AndAddsToBRMAndPersists() throws Exception {
        final BackupManager backupManager = EasyMock.createMock(BackupManager.class);
        final Action action = EasyMock.createMock(Action.class);
        final TestJob testJob = new TestJob();
        final NotificationService notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionCompleted(action);
        expectLastCall();
        testJob.setNotificationService(notificationService);
        testJob.setBackupManager(backupManager);
        testJob.setAction(action);
        testJob.setActionRepository(actionRepository);

        backupManager.addAction(action);
        expectLastCall().once();

        actionFileService.writeToFile(action);
        expectLastCall();
        expect(action.isPartOfHousekeeping()).andReturn(false);
        action.setState(anyObject());
        expectLastCall().once();
        action.setCompletionTime(anyObject());
        expectLastCall().once();
        action.setLastUpdateTime(anyObject());
        expectLastCall().once();
        action.updateOperationsTotalMetric();
        expectLastCall().times(0,1);
        action.updateLastOperationInfoMetric();
        expectLastCall().times(0,1);
        action.persist();
        expectLastCall().once();
        expect(action.getCompletionTime()).andReturn(OffsetDateTime.now()).once();
        expect(action.getResult()).andReturn(ResultType.SUCCESS).once();
        expect(action.getName()).andReturn(CREATE_BACKUP).times(1,2);
        expect(action.getActionId()).andReturn("12345").anyTimes();

        cmMediatorService.enqueueProgressReport(action);
        expectLastCall();
        expect(jobFactory.createJob(backupManager, action)).andReturn(testJob).once();
        EasyMock.replay(jobFactory, backupManager, action, actionFileService, cmMediatorService);

        jobExecutor.execute(backupManager, action);

        Awaitility.await().atMost(900, TimeUnit.MILLISECONDS).until(testJob::isFinished);

        assertTrue(testJob.isFinished());
        verify(jobFactory, backupManager, action);
    }

    @Test(expected = AnotherActionRunningException.class)
    public void execute_BackupManagerIdAndAction_twoJobsExecuted_SecondsFails() {
        // This test was written to test that the JobExecutor in use guarantees in order, serial Job execution.
        // Because the job executor in use at time of writing rejects a second Action if a first one is running,
        // this test throws the above exception. Should this test start failing with "Expected exception not thrown",
        // this is an indication the jobExecutor is no longer rejecting Actions while another is running. If this is
        // desired, remove the expected clause from the @Test annotation, but do not modify this test. If the this test
        // fails for any other reason, the JobExecutor has a critical bug in it, which may lead to data corruption due
        // to two jobs executing at once or jobs being run out of order of submission.

        // Setup the mocked backup manager and action
        final BackupManager backupManager = EasyMock.createMock(BackupManager.class);
        final Action action = EasyMock.createMock(Action.class);
        final NotificationService notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionCompleted(action);
        expectLastCall().anyTimes();
        expect(action.getName()).andReturn(CREATE_BACKUP).anyTimes();
        // Sometimes the execution thread gets far enough to set the current job, in which case the second call to
        // queueAction will try and get the current job's EVENT_ID while generating a failure message, so expect that.
        expect(action.getActionId()).andReturn("12345");
        backupManager.addAction(action);
        expectLastCall().once();

        // Lock shared across both test jobs. When they run, they both try to acquire the lock and hold it for 200ms.
        // If one of them fails to acquire the lock, it throws an AssertionError when TestJob::assertLockAcquired
        // is called. This ensures the jobs do not run at the same time. Waiting on the second job to finish and then
        // immediately asserting that the first is finished ensures the jobs are run in the correct order. These two
        // checks together test the JobExecutors promise of in-order, in-series execution of jobs.
        final ReentrantLock testLock = new ReentrantLock();

        // Set up the two test jobs
        final TestJob testJob1 = new TestJob(testLock);
        final TestJob testJob2 = new TestJob(testLock);
        testJob1.setBackupManager(backupManager);
        testJob1.setAction(action);
        testJob1.setNotificationService(notificationService);
        testJob1.setActionRepository(actionRepository);
        testJob2.setBackupManager(backupManager);
        testJob2.setAction(action);
        testJob2.setActionRepository(actionRepository);
        testJob2.setNotificationService(notificationService);

        // Create a JobFactory mock which return execute the first job, then the second, and throw on any subsequent calls
        expect(jobFactory.createJob(backupManager, action)).andReturn(testJob1).once();
        expect(jobFactory.createJob(backupManager, action)).andReturn(testJob2).once();

        actionFileService.writeToFile(action);
        expectLastCall();
        expect(action.isPartOfHousekeeping()).andReturn(false);

        final CMMediatorService cmMediatorService = EasyMock.createMock(CMMediatorService.class);
        cmMediatorService.enqueueProgressReport(action);
        expectLastCall();
        EasyMock.replay(jobFactory, action, actionFileService, cmMediatorService);

        jobExecutor.execute(backupManager, action);

        // This will throw the expected exception, because the other job is still running, and the JobExecutor at
        // time of writing is designed to reject an action if a Job is ongoing.
        jobExecutor.execute(backupManager, action);

        // Wait until testJob2 is finished then assert testJob1 is finished as it should have been run first
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(testJob2::isFinished);
        assertTrue(testJob1.isFinished());

        // Assert both jobs successfully acquired the shared test lock, testing that they ran in series. This guarantees
        // there is no simultaneous access to shared resources by the jobs.
        testJob1.assertLockAcquired();
        testJob2.assertLockAcquired();
    }

    @Test
    public void getRunningJobs_noJobIsRunning_returnsEmptyIterator() throws Exception {
        assertTrue(jobExecutor.getRunningJobs().isEmpty());
    }

    private class TestJob extends Job {

        private boolean finished;
        private Lock lock;
        boolean lockFailed;

        public TestJob() {
            // Empty default constructor
        }

        public TestJob(final Lock testLock) {
            // A lock shared across jobs, used to verify jobs are run in series not parallel (by checking for lock contention)
            lock = testLock;
        }

        public void assertLockAcquired() {
            assertFalse(lockFailed); // Indicates jobs were run at same time
        }

        @Override
        public List<CreateActionRequest> run() {
            boolean locked = false;
            if (lock != null) {
                locked = lock.tryLock();
                runAction();
                if (locked) {
                    lock.unlock();
                }
                lockFailed = !locked;
            } else {
                runAction();
            }
            finished = true;
            return new LinkedList<>();
        }

        private void runAction() {
            try {
                // Sleep for a little bit to encourage lock contention if other jobs are running
                // and to allow the accepted action to be persisted before the execution loop
                // finishes the Job execution.
                Thread.sleep(300);
            } catch (final InterruptedException ignored) {
            }
        }

        @Override
        protected void triggerJob() {
            //Not needed
        }

        public boolean isFinished() {
            return finished;
        }

        @Override
        protected boolean didFinish() {
            return true;
        }

        @Override
        protected void completeJob() {
            //Not needed
        }

        @Override
        protected void fail() {
            //Not needed
        }

    }

}
