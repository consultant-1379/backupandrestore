/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
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
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.EXPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.RESTORE;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.IMPORT;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.verify;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.locks.ReentrantLock;

import org.awaitility.Awaitility;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.VirtualInformation;

import com.ericsson.adp.mgmt.backupandrestore.exception.AnotherActionRunningException;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;

/**
* This test is written to test that the JobExecutor in use will not allow conflicting actions
* when parallel actions is enabled.
* At the time of writing, the executor will only allow:
* 1. Single action at a time
* 2. Or parallel EXPORT and CREATE_BACKUP action, if they are from the same BRM but for different backups
*/
public class JobExecutorParallelActionsTest extends JobExecuterParallelActionSetup{

    @Before
    public void setup() {
        /**
         * Allow parallel CREATE_BACKUP and EXPORT actions by
         * setting isParallelActionsEnabled to true
         */
        jobExecutor = new QueueingJobExecutor(jobFactory,
                                              actionRepository,
                                              actionFileService,
                                              cmMediatorService,
                                              true);
    }

    @Test
    public void execute_singleCreateBackupAction_createsJobAndRunsOnASeparateThread_AndAddsToBRMAndPersists() throws Exception {
        final BackupManager backupManager = createBackupManagerMock("testBRM");

        // Create an action and expect it to finish
        final Action action = createSuccessfulActionMock(backupManager, CREATE_BACKUP, "12345");

        // Since this is a CREATE_BACKUP action, expect that a notification will be sent
        final NotificationService notificationService = createNotificationServiceMock(action, true);

        final TestJob testJob = new TestJob(backupManager, action, notificationService);

        expect(jobFactory.createJob(backupManager, action)).andReturn(testJob).once();
        EasyMock.replay(jobFactory, backupManager, action, actionFileService, cmMediatorService, notificationService);

        jobExecutor.execute(backupManager, action);

        Awaitility.await().atMost(1000, TimeUnit.MILLISECONDS).until(testJob::isFinished);
        assertTrue(testJob.isFinished());
        verify(jobFactory, backupManager, action, notificationService);
    }

    @Test
    public void execute_singleExportAction_createsJobAndRunsOnASeparateThread_AndAddsToBRMAndPersists() throws Exception {
        final BackupManager backupManager = createBackupManagerMock("testBRM");

        // Create an action and expect it to finish
        final Action action = createSuccessfulActionMock(backupManager, EXPORT, "12345");

        // Since this is an EXPORT action, expect that a notification will NOT be sent
        final NotificationService notificationService = createNotificationServiceMock(action, false);

        final TestJob testJob = new TestJob(backupManager, action, notificationService);

        expect(jobFactory.createJob(backupManager, action)).andReturn(testJob).once();
        EasyMock.replay(jobFactory, backupManager, action, actionFileService, cmMediatorService, notificationService);

        jobExecutor.execute(backupManager, action);

        Awaitility.await().atMost(1000, TimeUnit.MILLISECONDS).until(testJob::isFinished);
        assertTrue(testJob.isFinished());
        verify(jobFactory, backupManager, action, notificationService);
    }

    @Test
    public void execute_exportAndcreateBackupOnSimilarBRMandSimilarBackups_firstActionExecuted_SecondsFails() {
        // This test attempts to run an EXPORT action first then immediately followed by
        // a CREATE_BACKUP action which are for the same BRM and same backup.
        // As this is not allowed, an AnotherActionRunningException should be thrown,
        // when the CREATE_BACKUP is executed.
        final BackupManager backupManager = createBackupManagerMock("testBRM");

        // Create an EXPORT action which is expected to finish
        final Action successfulAction = createSuccessfulActionMock(backupManager, EXPORT, "12345");

        // Create a CREATE_BACKUP action which is expected to fail
        final Action rejectedAction = createFailingActionMock(backupManager, CREATE_BACKUP, "67890");

        // Expect the two actions are for similar BRM
        expect(rejectedAction.hasSameBRMId(successfulAction)).andReturn(true).once();

        // Expect the two actions are for similar backups
        expect(rejectedAction.hasSameBackupName(successfulAction)).andReturn(true).once();

        // Since the action which will complete is an EXPORT action,
        // no notification is expected to be sent
        final NotificationService notificationService = createMock(NotificationService.class);

        // Lock shared across both test jobs. When they run, they both try to acquire the lock and hold it for 200ms.
        // If one of them fails to acquire the lock, it throws an AssertionError when TestJob::assertLockAcquired
        // is called. This ensures the jobs do not run at the same time. Waiting on the second job to finish and then
        // immediately asserting that the first is finished ensures the jobs are run in the correct order. These two
        // checks together test the JobExecutors promise of in-order, in-series execution of jobs.
        final ReentrantLock testLock = new ReentrantLock();

        // Set up the two test jobs
        final TestJob successfulJob = new TestJob(testLock, backupManager, successfulAction, notificationService, actionRepository);

        final TestJob rejectedJob = new TestJob(testLock, backupManager, rejectedAction, notificationService, actionRepository);

        // Create a JobFactory mock which creates ONLY the job for successful action
        expect(jobFactory.createJob(backupManager, successfulAction)).andReturn(successfulJob).once();

        EasyMock.replay(backupManager, jobFactory, rejectedAction, successfulAction, actionFileService, cmMediatorService, notificationService);

        jobExecutor.execute(backupManager, successfulAction);

        // This flag is used to verify that the executing a second action throws an exception. It will only be set to TRUE when the
        // AnotherActionRunningException exception is thrown.
        boolean isSecondActionRejected = false;
        // This will throw the expected exception, because the other job is still running.
        try {
            jobExecutor.execute(backupManager, rejectedAction);
        } catch (AnotherActionRunningException e) {
            isSecondActionRejected = true;
        }

        //  Verify the second action is rejected.
        assertTrue(isSecondActionRejected);

        // Wait until successfulJob is finished then assert successfulJob is finished as it should have been run first

        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(successfulJob::isFinished);
        assertTrue(successfulJob.isFinished());

        // Assert both jobs run sequentially successfully. This guarantees
        // there is no simultaneous access to shared resources (i.e the ReentrantLock) by the jobs.
        successfulJob.assertSequentialRun();
        rejectedJob.assertSequentialRun();
    }

    @Test
    public void execute_createBackupAndExportOnSimilarBRMandDifferentBackups_bothActionsExecuted() {
        // This test attempts to run a CREATE_BACKUP action first then immediately followed by
        // an EXPORT action which are for the same BRM and different backups.
        // Both actions are expected to succeed.
        final BackupManager backupManager = createBackupManagerMock("testBRM");

        // Create a CREATE_BACKUP action which is expected to finish
        final Action createBackup = createSuccessfulActionMock(backupManager, CREATE_BACKUP, "12345");

        // Create an EXPORT action which is expected to finish
        final Action export = createSuccessfulActionMock(backupManager, EXPORT, "67890");

        // Expect the two actions are for similar BRM
        expect(export.hasSameBRMId(createBackup)).andReturn(true).once();

        // Expect the two actions are for different backups
        expect(export.hasSameBackupName(createBackup)).andReturn(false).once();

        // Expect a notification is sent from the completion
        // of the CREATE_BACKUP action
        final NotificationService notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionCompleted(createBackup);
        expectLastCall().once();

        // Set up the two test jobs
        // The test will execute the CREATE_BACKUP for a bit longer (1s),
        // to ensure that it is still running when the EXPORT is called
        final TestJob createBackupJob = new TestJob(1000, backupManager, createBackup, notificationService, actionRepository);

        final TestJob exportJob = new TestJob(backupManager, export, notificationService, actionRepository);

        // Create a JobFactory mock which creates jobs for both actions
        expect(jobFactory.createJob(backupManager, createBackup)).andReturn(createBackupJob).once();
        expect(jobFactory.createJob(backupManager, export)).andReturn(exportJob).once();

        EasyMock.replay(backupManager, jobFactory, export, createBackup, actionFileService, cmMediatorService, notificationService);

        jobExecutor.execute(backupManager, createBackup);
        jobExecutor.execute(backupManager, export);

        // Wait until both jobs are finished then assert that both have finished
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> createBackupJob.isFinished() && exportJob.isFinished());
        assertTrue(createBackupJob.isFinished());
        assertTrue(exportJob.isFinished());
    }

    @Test
    public void execute_exportandCreateBackupOnSimilarBRMandDifferentBackups_bothActionsExecuted() {
        // This test attempts to run a EXPORT action first then immediately followed by
        // a CREATE_BACKUP action which are for the same BRM and different backups.
        // Both actions are expected to succeed.
        final BackupManager backupManager = createBackupManagerMock("testBRM");

        // Create an EXPORT action which is expected to finish
        final Action export = createSuccessfulActionMock(backupManager, EXPORT, "67890");

        // Create a CREATE_BACKUP action which is expected to finish
        final Action createBackup = createSuccessfulActionMock(backupManager, CREATE_BACKUP, "12345");

        // Expect the two actions are for similar BRM
        expect(createBackup.hasSameBRMId(export)).andReturn(true).once();

        // Expect the two actions are for different backups
        expect(createBackup.hasSameBackupName(export)).andReturn(false).once();

        // Expect a notification is sent from the completion
        // of the CREATE_BACKUP action
        final NotificationService notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionCompleted(createBackup);
        expectLastCall().once();

        // Set up the two test jobs
        // The test will execute the EXPORT for a bit longer (1s),
        // to ensure that it is still running when the CREATE_BACKUP is called
        final TestJob exportJob = new TestJob(1000, backupManager, export, notificationService, actionRepository);
        final TestJob createBackupJob = new TestJob(1000, backupManager, createBackup, notificationService, actionRepository);

        // Create a JobFactory mock which creates jobs for both actions
        expect(jobFactory.createJob(backupManager, createBackup)).andReturn(createBackupJob).once();
        expect(jobFactory.createJob(backupManager, export)).andReturn(exportJob).once();

        EasyMock.replay(backupManager, jobFactory, export, createBackup, actionFileService, cmMediatorService, notificationService);

        jobExecutor.execute(backupManager, export);
        jobExecutor.execute(backupManager, createBackup);

        // Wait until both jobs are finished then assert that both have finished
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> createBackupJob.isFinished() && exportJob.isFinished());
        assertTrue(exportJob.isFinished());
        assertTrue(createBackupJob.isFinished());
    }

    @Test
    public void execute_ImportandRestoreBackupOnSameBRMandDifferentBackups_bothActionsExecuted() {
        // This test attempts to run a IMPORT action first then immediately followed by
        // a RESTORE action which are for the same BRM and different backups.
        // Both actions are expected to succeed.
        final BackupManager backupManager = createBackupManagerMock("testBRM");

        // Create an IMPORT action which is expected to finish
        final Action actionImport = createSuccessfulActionMock(backupManager, IMPORT, "67890");

        // Create a RESTORE action which is expected to finish
        final Action restore = createSuccessfulActionMock(backupManager, RESTORE, "12345");

        // Expect the two actions are for similar BRM
        expect(restore.hasSameBRMId(actionImport)).andReturn(true).once();

        // Expect the two actions are for different backups
        expect(restore.hasSameBackupName(actionImport)).andReturn(false).once();

        // Expect a notification is sent from the completion
        // of the RESTORE action
        final NotificationService notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionCompleted(restore);
        expectLastCall().once();

        // Set up the two test jobs
        // The test will execute the IMPORT for a bit longer (1s),
        // to ensure that it is still running when the RESTORE is called
        final TestJob exportJob = new TestJob(1000, backupManager, actionImport, notificationService, actionRepository);
        final TestJob createBackupJob = new TestJob(1000, backupManager, restore, notificationService, actionRepository);

        // Create a JobFactory mock which creates jobs for both actions
        expect(jobFactory.createJob(backupManager, restore)).andReturn(createBackupJob).once();
        expect(jobFactory.createJob(backupManager, actionImport)).andReturn(exportJob).once();

        EasyMock.replay(backupManager, jobFactory, actionImport, restore, actionFileService, cmMediatorService, notificationService);

        jobExecutor.execute(backupManager, actionImport);
        jobExecutor.execute(backupManager, restore);

        // Wait until both jobs are finished then assert that both have finished
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> createBackupJob.isFinished() && exportJob.isFinished());
        assertTrue(exportJob.isFinished());
        assertTrue(createBackupJob.isFinished());
    }

    @Test
    public void execute_ImportandRestoreBackupOnDifferentBRM_bothActionsExecuted() {
        // This test attempts to run a IMPORT action first then immediately followed by
        // a RESTORE action which are for the different BRM and different backups.
        // Both actions are expected to succeed.
        final BackupManager backupManager = createBackupManagerMock("testBRM");
        final BackupManager backupManager2 = createBackupManagerMock("testBRM2");

        // Create an IMPORT action which is expected to finish
        final Action actionImport = createSuccessfulActionMock(backupManager, IMPORT, "67890");

        // Create a RESTORE action which is expected to finish
        final Action restore = createSuccessfulActionMock(backupManager2, RESTORE, "12345");

        // Expect the two actions are for different BRM
        expect(restore.hasSameBRMId(actionImport)).andReturn(false).times(2);
        // Expect the two actions are for different backups
        expect(restore.hasSameBackupName(actionImport)).andReturn(false).times(2);
        // Expect the two actions are not configuration BRM for each other
        expect(restore.isConfigBRMOf(actionImport)).andReturn(false).once();
        expect(actionImport.isConfigBRMOf(restore)).andReturn(false).once();

        // Expect a notification is sent from the completion
        // of the RESTORE action
        final NotificationService notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionCompleted(restore);
        expectLastCall().once();

        // Set up the two test jobs
        // The test will execute the IMPORT for a bit longer (1s),
        // to ensure that it is still running when the RESTORE is called
        final TestJob importJob = new TestJob(1000, backupManager, actionImport, notificationService, actionRepository);
        final TestJob restoreBackupJob = new TestJob(1000, backupManager2, restore, notificationService, actionRepository);

        // Create a JobFactory mock which creates jobs for both actions
        expect(jobFactory.createJob(backupManager, actionImport)).andReturn(importJob).once();
        expect(jobFactory.createJob(backupManager2, restore)).andReturn(restoreBackupJob).once();

        EasyMock.replay(backupManager, backupManager2, jobFactory, actionImport, restore, actionFileService, cmMediatorService, notificationService);

        jobExecutor.execute(backupManager, actionImport);
        jobExecutor.execute(backupManager2, restore);

        // Wait until both jobs are finished then assert that both have finished
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> restoreBackupJob.isFinished() && importJob.isFinished());
        assertTrue(importJob.isFinished());
        assertTrue(restoreBackupJob.isFinished());
    }

    @Test
    public void execute_ImportandRestoreSameNameBackupOnDifferentBRM_bothActionsExecuted() {
        // This test attempts to run a IMPORT action first then immediately followed by
        // a RESTORE action which are for the different BRM and different backups.
        // Both actions are expected to succeed.
        final BackupManager backupManager = createBackupManagerMock("testBRM");
        final BackupManager backupManager2 = createBackupManagerMock("testBRM2");

        // Create an IMPORT action which is expected to finish
        final Action actionImport = createSuccessfulActionMock(backupManager, IMPORT, "67890");

        // Create a RESTORE action which is expected to finish
        final Action restore = createSuccessfulActionMock(backupManager2, RESTORE, "12345");

        // Expect the two actions are for different BRM
        expect(restore.hasSameBRMId(actionImport)).andReturn(false).times(2);
        // Expect the two actions are for different backups but same name
        expect(restore.hasSameBackupName(actionImport)).andReturn(true).times(2);
        // Expect the two actions are not configuration BRM for each other
        expect(restore.hasKinBRM(actionImport)).andReturn(false).once();
        expect(restore.isConfigBRMOf(actionImport)).andReturn(false).once();
        expect(actionImport.isConfigBRMOf(restore)).andReturn(false).once();

        // Expect a notification is sent from the completion
        // of the RESTORE action
        final NotificationService notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionCompleted(restore);
        expectLastCall().once();

        // Set up the two test jobs
        // The test will execute the IMPORT for a bit longer (1s),
        // to ensure that it is still running when the RESTORE is called
        final TestJob importJob = new TestJob(1000, backupManager, actionImport, notificationService, actionRepository);
        final TestJob restoreBackupJob = new TestJob(1000, backupManager2, restore, notificationService, actionRepository);

        // Create a JobFactory mock which creates jobs for both actions
        expect(jobFactory.createJob(backupManager, actionImport)).andReturn(importJob).once();
        expect(jobFactory.createJob(backupManager2, restore)).andReturn(restoreBackupJob).once();

        EasyMock.replay(backupManager, backupManager2, jobFactory, actionImport, restore, actionFileService, cmMediatorService, notificationService);

        jobExecutor.execute(backupManager, actionImport);
        jobExecutor.execute(backupManager2, restore);

        // Wait until both jobs are finished then assert that both have finished
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> restoreBackupJob.isFinished() && importJob.isFinished());
        assertTrue(importJob.isFinished());
        assertTrue(restoreBackupJob.isFinished());
    }

    @Test
    public void execute_ImportAndRestoreSameNameBackupOnKinBRMs_firstActionExecuted_SecondsFails() {
        // This test attempts to run a IMPORT action first then immediately followed by
        // a RESTORE action which are for the child BRM and same backups.
        final BackupManager backupManager = createBackupManagerMock("testBRM");
        final BackupManager childBackupManager = createBackupManagerMock("testBRM2");

        final VirtualInformation virtualInformation = new VirtualInformation("testBRM", new ArrayList<>());
        expect(childBackupManager.getVirtualInformation()).andReturn(virtualInformation).anyTimes();

        // Create an IMPORT action which is expected to finish
        final Action actionImport = createSuccessfulActionMock(backupManager, IMPORT, "67890");

        // Create a RESTORE action which is expected to finish
        final Action restore = createFailingActionMock(childBackupManager, RESTORE, "12345");

        // Expect the two actions are for different BRM
        expect(restore.hasSameBRMId(actionImport)).andReturn(false).times(2);

        // Expect the two actions are for same backups
        expect(restore.hasSameBackupName(actionImport)).andReturn(true).times(2);

        expect(restore.hasKinBRM(actionImport)).andReturn(true).once();
        expect(actionImport.hasKinBRM(restore)).andReturn(true).once();

        final NotificationService notificationService = createMock(NotificationService.class);

        // Set up the two test jobs
        // The test will execute the IMPORT for a bit longer (1s),
        // to ensure that it is still running when the RESTORE is called
        final TestJob importJob = new TestJob(1000, backupManager, actionImport, notificationService, actionRepository);
        final TestJob restoreBackupJob = new TestJob(1000, childBackupManager, restore, notificationService, actionRepository);

        // Create a JobFactory mock which creates jobs for both actions
        expect(jobFactory.createJob(backupManager, actionImport)).andReturn(importJob).once();
        expect(jobFactory.createJob(childBackupManager, restore)).andReturn(restoreBackupJob).once();

        EasyMock.replay(backupManager, childBackupManager, jobFactory, actionImport, restore, actionFileService, cmMediatorService, notificationService);

        jobExecutor.execute(backupManager, actionImport);

        boolean conflictingActionRejected = false;
        try {
            jobExecutor.execute(childBackupManager, restore);
        } catch (AnotherActionRunningException e) {
            conflictingActionRejected = true;
        }

        // Wait until success job is finished then assert failure job
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(importJob::isFinished);
        assertTrue(importJob.isFinished());
        assertTrue(conflictingActionRejected);
    }

    @Test
    public void execute_ImportBackupOnBRMAndRestoreBackupOnConfigBRM_firstActionExecuted_SecondsFails() {
        // This test attempts to run a IMPORT action first then immediately followed by
        // a RESTORE action which are for the config BRM and same backups.
        final BackupManager backupManager = createBackupManagerMock("testBRM");
        final BackupManager configBackupManager = createBackupManagerMock("testBRM-bro");

        // Create an IMPORT action which is expected to finish
        final Action actionImport = createSuccessfulActionMock(backupManager, IMPORT, "67890");

        // Create a RESTORE action which is expected to finish
        final Action restore = createFailingActionMock(configBackupManager, RESTORE, "12345");

        // Expect the two actions are for different BRM
        expect(restore.hasSameBRMId(actionImport)).andReturn(false).times(2);

        // Expect the two actions are for same backups
        expect(restore.hasSameBackupName(actionImport)).andReturn(true).times(2);

        expect(restore.hasKinBRM(actionImport)).andReturn(false).once();
        expect(restore.isConfigBRMOf(actionImport)).andReturn(true).once();
        expect(actionImport.isConfigBRMOf(restore)).andReturn(false).once();

        final NotificationService notificationService = createMock(NotificationService.class);

        // Set up the two test jobs
        // The test will execute the IMPORT for a bit longer (1s),
        // to ensure that it is still running when the RESTORE is called
        final TestJob importJob = new TestJob(1000, backupManager, actionImport, notificationService, actionRepository);
        final TestJob restoreBackupJob = new TestJob(1000, configBackupManager, restore, notificationService, actionRepository);

        // Create a JobFactory mock which creates jobs for both actions
        expect(jobFactory.createJob(backupManager, actionImport)).andReturn(importJob).once();
        expect(jobFactory.createJob(configBackupManager, restore)).andReturn(restoreBackupJob).once();

        EasyMock.replay(backupManager, configBackupManager, jobFactory, actionImport, restore, actionFileService, cmMediatorService, notificationService);

        jobExecutor.execute(backupManager, actionImport);

        boolean conflictingActionRejected = false;
        try {
            jobExecutor.execute(configBackupManager, restore);
        } catch (AnotherActionRunningException e) {
            conflictingActionRejected = true;
        }

        // Wait until success job is finished then assert failure job
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(importJob::isFinished);
        assertTrue(importJob.isFinished());
        assertTrue(conflictingActionRejected);
    }

    @Test
    public void execute_RestoreBackupOnConfigBRMAndThenImportBackupOnBRM_firstActionExecuted_SecondsFails() {
        // This test attempts to run a RESTORE action on "-bro" BRM first then immediately followed by
        // a IMPORT action which are for the BRM and same backups.
        final BackupManager configBackupManager = createBackupManagerMock("testBRM-bro");
        final BackupManager backupManager = createBackupManagerMock("testBRM");


        // Create an IMPORT action which is expected to fail
        final Action actionImport = createFailingActionMock(backupManager, IMPORT, "12345");

        // Create a RESTORE action which is expected to finish
        final Action restore = createSuccessfulActionMock(configBackupManager, RESTORE, "67890");

        // Expect the two actions are for different BRM
        expect(actionImport.hasSameBRMId(restore)).andReturn(false).times(2);

        // Expect the two actions are for same backups
        expect(actionImport.hasSameBackupName(restore)).andReturn(true).times(2);

        expect(actionImport.hasKinBRM(restore)).andReturn(false).once();
        expect(restore.isConfigBRMOf(actionImport)).andReturn(true).once();
        expect(actionImport.isConfigBRMOf(restore)).andReturn(false).once();

        final NotificationService notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionCompleted(restore);
        expectLastCall().once();

        // Set up the two test jobs
        // The test will execute the RESTORE for a bit longer (1s),
        // to ensure that it is still running when the IMPORT is called
        final TestJob restoreBackupJob = new TestJob(1000, configBackupManager, restore, notificationService, actionRepository);
        final TestJob importJob = new TestJob(1000, backupManager, actionImport, notificationService, actionRepository);

        // Create a JobFactory mock which creates jobs for both actions
        expect(jobFactory.createJob(configBackupManager, restore)).andReturn(restoreBackupJob).once();
        expect(jobFactory.createJob(backupManager, actionImport)).andReturn(importJob).once();

        EasyMock.replay(backupManager, configBackupManager, jobFactory, actionImport, restore, actionFileService, cmMediatorService, notificationService);

        jobExecutor.execute(configBackupManager, restore);

        boolean conflictingActionRejected = false;
        try {
            jobExecutor.execute(backupManager, actionImport);
        } catch (AnotherActionRunningException e) {
            conflictingActionRejected = true;
        }

        // Wait until success job is finished then assert failure job
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(restoreBackupJob::isFinished);
        assertTrue(restoreBackupJob.isFinished());
        assertTrue(conflictingActionRejected);
    }

    @Test
    public void execute_thirdActionWhenBothExportandCreateBackupAreRunning_existingActionsSuccedd_thirdActionFails() {
        // This test attempts to run a third action when there are already parallel actions running.
        // An EXPORT action immediately followed by
        // a CREATE_BACKUP action for the same BRM and different backups are first executed,
        // and then another CREATE_BACKUP action is executed.
        // The first CREATE_BACKUP and EXPORT actions are expected to succeed.
        // The second CREATE_BACKUP should be rejected,
        // as it cannot run in parallel with another CREATE_BACKUP.
        final BackupManager backupManager = createBackupManagerMock("testBRM");

        // Create an EXPORT action which is expected to finish
        final Action export = createSuccessfulActionMock(backupManager, EXPORT, "67890");

        // Create a CREATE_BACKUP action which is expected to finish
        final Action createBackup = createSuccessfulActionMock(backupManager, CREATE_BACKUP, "12345");

        // Create a third action which is expected to fail.
        final Action rejectedAction = createFailingActionMock(backupManager, CREATE_BACKUP, "12345");

        // Expect the first two actions are for similar BRM
        expect(createBackup.hasSameBRMId(export)).andReturn(true).once();

        // Expect the first two actions are for different backups
        expect(createBackup.hasSameBackupName(export)).andReturn(false).once();

        // Expect a notification is sent from the completion
        // of the first CREATE_BACKUP action
        final NotificationService notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionCompleted(createBackup);
        expectLastCall().once();

        // Set up the two test jobs
        // The test will execute the EXPORT and CREATE_BACKUP job for a bit longer (1s),
        // to ensure that it is still running when the CREATE_BACKUP is called
        final TestJob exportJob = new TestJob(1000, backupManager, export, notificationService, actionRepository);
        final TestJob createBackupJob = new TestJob(1000, backupManager, createBackup, notificationService, actionRepository);

        // Create a JobFactory mock which creates jobs for both actions
        expect(jobFactory.createJob(backupManager, createBackup)).andReturn(createBackupJob).once();
        expect(jobFactory.createJob(backupManager, export)).andReturn(exportJob).once();

        EasyMock.replay(backupManager, jobFactory, export, createBackup, rejectedAction, actionFileService, cmMediatorService, notificationService);

        jobExecutor.execute(backupManager, export);
        jobExecutor.execute(backupManager, createBackup);
        // This flag is used to verify that the executing a a third action
        // throws an exception. It will only be set to TRUE when the
        // AnotherActionRunningException exception is thrown.
        boolean isThirActionRejected = false;
        // This will throw the expected exception, because the other job is still running, and the JobExecutor at
        // time of writing is designed to reject an action if a Job is ongoing.
        try {
            jobExecutor.execute(backupManager, rejectedAction);
        } catch (AnotherActionRunningException e) {
            isThirActionRejected = true;
        }
        //  Verify the third action is rejected.
        assertTrue(isThirActionRejected);

        // Wait until both jobs are finished then assert that both have finished
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> createBackupJob.isFinished() && exportJob.isFinished());
        assertTrue(exportJob.isFinished());
        assertTrue(createBackupJob.isFinished());
    }

    @Test
    public void execute_autoCreateBackupWithAutoExport_whenManualExportIsRunning_allJobsSucceed() {
        // This test attempts to run an auto CREATE_BACKUP action which has a post execution auto EXPORT action,
        // while a long running manual EXPORT is executing.
        // When the CREATE_BACKUP is complete, and the manual EXPORT is still running,
        // The auto EXPORT will be put on hold, and will execute once the manual EXPORT is running.
        // All the jobs are expected to succeed.

        final BackupManager backupManager = createBackupManagerMock("testBRM");

        //Create a MANUAL EXPORT action which is expected to finish
        final Action manualExport = createSuccessfulActionMock(backupManager, EXPORT, "manualExport");

        // Create an AUTO CREATE_BACKUP action which is expected to finish
        final Action autoCreateBackup = createSuccessfulActionMock(backupManager, CREATE_BACKUP, "autoCreateBackup");

        // Create an AUTO EXPORT action which is expected to finish
        final Action autoExport = createSuccessfulActionMock(backupManager, EXPORT, "autoExport");
        final CreateActionRequest autoExportRequest = EasyMock.createMock(CreateActionRequest.class);
        expect(actionRepository.createAction(backupManager, autoExportRequest)).andReturn(autoExport);

        // Expect the autoCreateBackup and manualExport actions are for similar BRM
        expect(autoCreateBackup.hasSameBRMId(manualExport)).andReturn(true).once();

        // Expect the autoCreateBackup and manualExport actions are for different backups
        expect(autoCreateBackup.hasSameBackupName(manualExport)).andReturn(false).once();

        // Expect a notification is sent from the completion
        // of the first CREATE_BACKUP action
        final NotificationService notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionCompleted(autoCreateBackup);
        expectLastCall().once();

        // Wrap autoExportRequest in a list of post execution actions
        List<CreateActionRequest> postExecActions = List.of(autoExportRequest);

        // Create the AutoCreateBackup job which will return the autoExport as postExecAction
        final TestJob autoCreateBackupJob = new TestJob(backupManager, autoCreateBackup, notificationService, actionRepository, postExecActions);

        // Create a ReentrantLock which will be used to assert that the manual Export and autoExport
        // run sequentially instead of in parallel
        final ReentrantLock lock = new ReentrantLock();

        // Create the manual Export job which will run longer than autoCreateBackup job (2s)
        // to ensure it is still running when the AutoCreateBacup job enqueues the autoExport action
        final TestJob manualExportJob = new TestJob(2000, lock, backupManager, manualExport, notificationService, actionRepository);

        // Create the auto Export job
        final TestJob autoExportJob = new TestJob(lock, backupManager, autoExport, notificationService, actionRepository);

        // Create a JobFactory mock which creates jobs for all the 3 actions
        expect(jobFactory.createJob(backupManager, autoCreateBackup)).andReturn(autoCreateBackupJob).once();
        expect(jobFactory.createJob(backupManager, manualExport)).andReturn(manualExportJob).once();
        expect(jobFactory.createJob(backupManager, autoExport)).andReturn(autoExportJob).once();

        EasyMock.replay(backupManager, jobFactory, autoCreateBackup, manualExport, autoExport, autoExportRequest, actionFileService, cmMediatorService, actionRepository, notificationService);

        // Then execute the manual Export
        jobExecutor.execute(backupManager, manualExport);

        try {
            // Allow the manual export to run for a bit (100ms)
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }

        // Execute autoCreate backup
        jobExecutor.execute(backupManager, autoCreateBackup);

        // Wait until all the 3 jobs are finished then assert that they have finished
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> autoCreateBackupJob.isFinished() && manualExportJob.isFinished() && autoExportJob.isFinished());
        assertTrue(autoCreateBackupJob.isFinished());
        assertTrue(manualExportJob.isFinished());
        assertTrue(autoExportJob.isFinished());

        // Assert that the manual and autoExport job ran sequentially
        manualExportJob.assertSequentialRun();
        autoExportJob.assertSequentialRun();
    }

    @Test
    public void test_manualExportIsRejected_whenAutoExportRunning() {
        final BackupManager backupManager = createBackupManagerMock("testBRM");

        // Create an AUTO CREATE_BACKUP action which is expected to finish
        final Action autoCreateBackup = createSuccessfulActionMock(backupManager, CREATE_BACKUP, "autoCreateBackup");

        // Create an AUTO EXPORT action which is expected to finish
        final Action autoExport = createSuccessfulActionMock(backupManager, EXPORT, "autoExport");
        final CreateActionRequest autoExportRequest = EasyMock.createMock(CreateActionRequest.class);
        expect(actionRepository.createAction(backupManager, autoExportRequest)).andReturn(autoExport);

        //Create a MANUAL EXPORT action which is expected to be rejected
        final Action manualExport = createFailingActionMock(backupManager, EXPORT, "manualExport");

        // Expect the autoCreateBackup and manualExport actions are for similar BRM
        expect(manualExport.hasSameBRMId(autoCreateBackup)).andReturn(true).once();

        // Expect the autoCreateBackup and manualExport actions are for different backups
        expect(manualExport.hasSameBackupName(autoCreateBackup)).andReturn(false).once();

        // Expect a notification is sent from the completion
        // of the first CREATE_BACKUP action
        final NotificationService notificationService = createMock(NotificationService.class);
        notificationService.notifyAllActionCompleted(autoCreateBackup);
        expectLastCall().once();

        // Wrap autoExportRequest in a list of post execution actions
        List<CreateActionRequest> postExecActions = List.of(autoExportRequest);

        // Create the AutoCreateBackup job which will return the autoExport as postExecAction
        final TestJob autoCreateBackupJob = new TestJob(backupManager, autoCreateBackup, notificationService, actionRepository, postExecActions);

        // Create a ReentrantLock which will be used to assert that the manual Export and autoExport
        // run sequentially instead of in parallel
        final ReentrantLock lock = new ReentrantLock();

        // Create the auto Export job which will run longer than manualCreateBackup job (2s)
        // to ensure it is still running when the AutoCreateBacup job enqueues the autoExport action
        final TestJob autoExportJob = new TestJob(2000, lock, backupManager, autoExport, notificationService, actionRepository);

        // Create the manual job
        final TestJob manualExportJob = new TestJob(lock, backupManager, manualExport, notificationService, actionRepository);

        // Create a JobFactory mock which creates jobs for all the 2 auto backup and export actions
        expect(jobFactory.createJob(backupManager, autoCreateBackup)).andReturn(autoCreateBackupJob).once();
        expect(jobFactory.createJob(backupManager, autoExport)).andReturn(autoExportJob).once();

        EasyMock.replay(backupManager, jobFactory, autoCreateBackup, manualExport, autoExport, autoExportRequest, actionFileService, cmMediatorService, actionRepository, notificationService);

        // Then execute the auto createBackup
        jobExecutor.execute(backupManager, autoCreateBackup);
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> autoCreateBackupJob.isFinished());

        boolean conflictingActionRejected = false;

        try {
            // Allow the auto export to run for a bit (100ms)
            Thread.sleep(100);
            // Execute manual export
            jobExecutor.execute(backupManager, manualExport);
        } catch (InterruptedException e) {
        } catch (AnotherActionRunningException e) {
            System.out.println(e);
            conflictingActionRejected = true;
        }

        //  Verify the conflicting action is rejected.
        assertTrue(conflictingActionRejected);

        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> autoExportJob.isFinished());
    }

}
