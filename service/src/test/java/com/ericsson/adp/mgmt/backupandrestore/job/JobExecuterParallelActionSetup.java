package com.ericsson.adp.mgmt.backupandrestore.job;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionFileService;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionRepository;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import org.easymock.EasyMock;

import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;

import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.CREATE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.EXPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.IMPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.RESTORE;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.HOUSEKEEPING;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JobExecuterParallelActionSetup {
    public JobExecutor jobExecutor;
    final JobFactory jobFactory = EasyMock.createMock(JobFactory.class);
    final ActionRepository actionRepository = EasyMock.createMock(ActionRepository.class);
    final ActionFileService actionFileService = EasyMock.createMock(ActionFileService.class);
    final CMMediatorService cmMediatorService = EasyMock.createMock(CMMediatorService.class);

    protected Action createSuccessfulActionMock(final BackupManager backupManager, final ActionType actionType, final String actionId) {
        return createActionMock(backupManager, actionType, actionId, true);
    }

    protected Action createFailingActionMock(final BackupManager backupManager, final ActionType actionType, final String actionId) {
        return createActionMock(backupManager, actionType, actionId, false);
    }

    protected Action createActionMock(final BackupManager backupManager, final ActionType actionType, final String actionId, final boolean isSuccessful) {
        final Action action = EasyMock.createMock(Action.class);
        expect(action.getActionId()).andReturn(actionId).anyTimes();
        expect(action.getName()).andReturn(actionType).anyTimes();
        expect(action.isExport()).andReturn(actionType.equals(EXPORT)).anyTimes();
        expect(action.isImport()).andReturn(actionType.equals(IMPORT)).anyTimes();
        expect(action.isRestore()).andReturn(actionType.equals(RESTORE)).anyTimes();
        expect(action.isCreateBackup()).andReturn(actionType.equals(CREATE_BACKUP)).anyTimes();
        expect(action.isPartOfHousekeeping()).andReturn(actionType.equals(HOUSEKEEPING)).anyTimes();
        // Expect the executor to check if this action is an IMPORT/EXPORT
        // to determine which queue to send it to.
        expect(action.isImportOrExport()).andReturn(actionType.equals(EXPORT) || actionType.equals(IMPORT)).anyTimes();
        if (isSuccessful) {
            // expect the action to be persisted and added to the backup manager in memory
            cmMediatorService.enqueueProgressReport(action);
            expectLastCall();
            actionFileService.writeToFile(action);
            expectLastCall();
            backupManager.addAction(action);
            expectLastCall().once();

            // expect the action to be completed
            action.setState(ActionStateType.FINISHED);
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
            expect(action.getStartTime()).andReturn(OffsetDateTime.now()).times(0,1);
            expect(action.getCompletionTime()).andReturn(OffsetDateTime.now().plusNanos(10)).times(1,2);
            expect(action.getResult()).andReturn(ResultType.SUCCESS).times(1,2);
        }
        return action;
    }

    protected NotificationService createNotificationServiceMock(final Action action, final boolean shouldNotify) {
        final NotificationService notificationService = createMock(NotificationService.class);
        if (shouldNotify) {
            notificationService.notifyAllActionCompleted(action);
            expectLastCall().once();
        }
        return notificationService;
    }

    protected BackupManager createBackupManagerMock(final String brmId) {
        final BackupManager backupManager = EasyMock.createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn(brmId).anyTimes();
        return backupManager;
    }

    protected class TestJob extends Job {

        private boolean finished;
        private Lock lock;
        boolean lockFailed;
        int additionalJobRunDelay = 400;
        private List<CreateActionRequest> postExecActions = new LinkedList<>();

        public TestJob(final BackupManager backupManager, final Action action,
                       final NotificationService notificationService) {
            this.backupManager = backupManager;
            this.action = action;
            this.notificationService = notificationService;
        }

        public TestJob(final BackupManager backupManager, final Action action,
                       final NotificationService notificationService,
                       final ActionRepository actionRepository) {
            this(backupManager, action, notificationService);
            this.actionRepository = actionRepository;
        }

        public TestJob(final Lock testLock, final BackupManager backupManager, final Action action,
                       final NotificationService notificationService, final ActionRepository actionRepository) {
            this(backupManager, action, notificationService, actionRepository);
            this.lock = testLock;
        }

        public TestJob(final int jobRunDelay, final BackupManager backupManager, final Action action,
                       final NotificationService notificationService, final ActionRepository actionRepository) {
            this(backupManager, action, notificationService, actionRepository);
            this.additionalJobRunDelay = jobRunDelay;
        }

        public TestJob(final int jobRunDelay, final Lock testLock, final BackupManager backupManager, final Action action,
                       final NotificationService notificationService, final ActionRepository actionRepository) {
            this(testLock, backupManager, action, notificationService, actionRepository);
            this.additionalJobRunDelay = jobRunDelay;
        }

        public TestJob(final BackupManager backupManager, final Action action,
                       final NotificationService notificationService, final ActionRepository actionRepository,
                       final List<CreateActionRequest> postExecActions) {
            this(backupManager, action, notificationService, actionRepository);
            this.postExecActions = postExecActions;
        }

        public void assertSequentialRun() {
            // Failure in this assertion indicates the
            // jobs did not run sequentially
            assertFalse(lockFailed);
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
            return postExecActions;
        }

        private void runAction() {
            // Sleep for a little bit to encourage lock contention if other jobs are running
            // and to allow the action to be persisted before the job finishes
            try {
                Thread.sleep(additionalJobRunDelay);
            } catch (final InterruptedException ignored) {
            }
        }

        @Override
        protected void triggerJob() {
            try {
                Thread.sleep(additionalJobRunDelay);  // Sleep for a little bit to encourage lock contention if other jobs are running
            } catch (final InterruptedException ignored) {
            }        }

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

    public void getRunningJobs_noJobIsRunning_returnsEmptyList() throws Exception {
        assertTrue(jobExecutor.getRunningJobs().isEmpty());
    }
}
