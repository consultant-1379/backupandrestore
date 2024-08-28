package com.ericsson.adp.mgmt.backupandrestore.job;

import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.CREATE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.DELETE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.EXPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.HOUSEKEEPING_DELETE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.IMPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.RESTORE;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.awaitility.Awaitility;
import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.exception.AnotherActionRunningException;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;

@RunWith(Parameterized.class)
public class JobExecutorParallelActionsParameterisedTest extends JobExecuterParallelActionSetup{

    final ActionType action;
    final ActionType conflictingAction;
    final boolean sameBRM;

    public JobExecutorParallelActionsParameterisedTest(ActionType action,
                                                       ActionType conflictingActionForCreateBackup,
                                                       Boolean sameBRM) {
        this.action = action;
        this.conflictingAction = conflictingActionForCreateBackup;
        this.sameBRM = sameBRM;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { CREATE_BACKUP, CREATE_BACKUP, true },
                { CREATE_BACKUP, IMPORT, true },
                { CREATE_BACKUP, RESTORE, true },
                { CREATE_BACKUP, HOUSEKEEPING_DELETE_BACKUP, true },
                { CREATE_BACKUP, DELETE_BACKUP, true },
                { EXPORT, EXPORT, true },
                { EXPORT, IMPORT, true },
                { EXPORT, RESTORE, true },
                { EXPORT, HOUSEKEEPING_DELETE_BACKUP, true },
                { EXPORT, DELETE_BACKUP, true },
                { CREATE_BACKUP, CREATE_BACKUP, false },
                { CREATE_BACKUP, EXPORT, false },
                { CREATE_BACKUP, IMPORT, false },
                { CREATE_BACKUP, RESTORE, false },
                { CREATE_BACKUP, HOUSEKEEPING_DELETE_BACKUP, false },
                { CREATE_BACKUP, DELETE_BACKUP, false },
                { EXPORT, EXPORT, false },
                { EXPORT, CREATE_BACKUP, false },
                { EXPORT, IMPORT, false },
                { EXPORT, RESTORE, false },
                { EXPORT, HOUSEKEEPING_DELETE_BACKUP, false },
                { IMPORT, IMPORT, false },
                { IMPORT, CREATE_BACKUP, false },
                { IMPORT, EXPORT, false },
                { IMPORT, HOUSEKEEPING_DELETE_BACKUP, false },
                { IMPORT, IMPORT, true },
                { IMPORT, CREATE_BACKUP, true },
                { IMPORT, EXPORT, true },
                { IMPORT, HOUSEKEEPING_DELETE_BACKUP, true },
                { RESTORE, EXPORT, false },
                { RESTORE, CREATE_BACKUP, false },
                { RESTORE, RESTORE, false },
                { RESTORE, HOUSEKEEPING_DELETE_BACKUP, false },
                { RESTORE, EXPORT, true },
                { RESTORE, CREATE_BACKUP, true },
                { RESTORE, RESTORE, true },
                { RESTORE, HOUSEKEEPING_DELETE_BACKUP, true },
                { HOUSEKEEPING_DELETE_BACKUP, RESTORE, true },

        });
    }

    @Test
    public void test_conflicting_actions_isRejected() throws Exception {
        test_conflicting_actions(action, conflictingAction, sameBRM);
    }

    private void test_conflicting_actions(ActionType actionA, ActionType actionB, boolean sameBRM) {
        jobExecutor = new QueueingJobExecutor(jobFactory,
                actionRepository,
                actionFileService,
                cmMediatorService,
                true);

        final BackupManager backupManager = createBackupManagerMock("testBRM");
        BackupManager backupManager2 = createBackupManagerMock("testBRM2");

        if (sameBRM) {
            backupManager2 = backupManager;
        }

        // Create an action and expect it to finish
        final Action runningAction = createSuccessfulActionMock(backupManager, actionA, "12345");

        // Create an action and expect it to be rejected
        final Action rejectedAction = createFailingActionMock(backupManager2, actionB, "123456");

        final NotificationService notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionCompleted(runningAction);
        expectLastCall().once();

        // Lock shared across both test jobs. When they run, they both try to acquire the lock and hold it for 200ms.
        // If one of them fails to acquire the lock, it throws an AssertionError when TestJob::assertLockAcquired
        // is called. This ensures the jobs do not run at the same time. Waiting on the second job to finish and then
        // immediately asserting that the first is finished ensures the jobs are run in the correct order. These two
        // checks together test the JobExecutors promise of in-order, in-series execution of jobs.
        final ReentrantLock testLock = new ReentrantLock();

        final JobExecutorParallelActionsTest.TestJob runningJob = new JobExecutorParallelActionsTest.TestJob(testLock, backupManager, runningAction, notificationService, actionRepository);

        expect(jobFactory.createJob(backupManager, runningAction)).andReturn(runningJob).once();
        expect(rejectedAction.hasSameBackupName(runningAction)).andReturn(false).anyTimes();
        if (sameBRM) {
            expect(rejectedAction.hasSameBRMId(runningAction)).andReturn(true).anyTimes();
            EasyMock.replay(jobFactory, backupManager, runningAction, rejectedAction, actionFileService, cmMediatorService, notificationService);
        } else {
            expect(rejectedAction.hasSameBRMId(runningAction)).andReturn(false).anyTimes();
            EasyMock.replay(jobFactory, backupManager, backupManager2, runningAction, rejectedAction, actionFileService, cmMediatorService, notificationService);
        }

        jobExecutor.execute(backupManager, runningAction);

        boolean conflictingActionRejected = false;

        try {
            // Allow the previous action to run for a bit (100ms)
            Thread.sleep(100);
            jobExecutor.execute(backupManager, rejectedAction);
        } catch (InterruptedException e) {
        } catch (AnotherActionRunningException e) {
            System.out.println(e);
            conflictingActionRejected = true;
        }

        //  Verify the conflicting action is rejected.
        assertTrue(conflictingActionRejected);

        // Wait until the successfulJob is finished then assert successfulJob is finished as it should have been run first
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(runningJob::isFinished);
    }
}
