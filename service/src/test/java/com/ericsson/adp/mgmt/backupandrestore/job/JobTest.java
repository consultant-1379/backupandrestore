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

import static com.ericsson.adp.mgmt.backupandrestore.action.ResultType.SUCCESS;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.ericsson.adp.mgmt.backupandrestore.SpringContext;
import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionRepository;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionService;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.notification.Notification;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationFailedException;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;

import io.micrometer.core.instrument.MeterRegistry;


public class JobTest {

    private JobExecutor jobExecutor;
    private NotificationService notificationService;
    private ActionService actionService;
    private ActionRepository actionRepository;
    private MockedStatic<SpringContext> mockedSpringContext;
    private MeterRegistry meterRegistry;

    @Before
    public void setup() {
        mockedSpringContext = mockStatic(SpringContext.class);
        jobExecutor = createMock(JobExecutor.class);
        notificationService = createMock(NotificationService.class);
        actionService = createMock(ActionService.class);
        actionRepository = createMock(ActionRepository.class);
        when(SpringContext.getBean(MeterRegistry.class)).thenReturn(Optional.empty());


    }

    @After
    public void tearDown() {
        // Close the static mock after each test
        mockedSpringContext.close();
    }

    @Test
    public void run_createJobFromAction_executeJob() throws Exception {
        final Action action = mockSuccessfulAction();
        final TestJob job = getTestJob(action);

        jobExecutor.setCurrentJob(Optional.of(job));
        expectLastCall();
        jobExecutor.setCurrentJob(Optional.empty());
        expectLastCall();
        expect(actionService.executeHousekeeping(anyObject(BackupManager.class))).andReturn(SUCCESS).anyTimes();
        replay(jobExecutor, actionService);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getBackupManagerId()).andReturn("bcd").anyTimes();
        expect(action.getResult()).andReturn(ResultType.SUCCESS).anyTimes();

        notificationService.notifyAllActionCompleted(action);
        expectLastCall();
        mockPVCMetricsCacheClearance();

        replay(action);
        replay(notificationService);
        job.run();
        job.completeAction();

        assertTrue(job.isFinished());
        verify(action);
        verify(jobExecutor);
        verify(notificationService);
        verify(actionService);
    }

    @Test
    public void runJob_backupManagerWithBroAndCreateBackupAction_fails() {
        final Action action = mock_Action_with_params(false,false,false);
        final TestJob job = getTestJob(action);

        jobExecutor.setCurrentJob(Optional.of(job));
        expectLastCall();
        jobExecutor.setCurrentJob(Optional.empty());
        expectLastCall();
        expect(actionService.executeHousekeeping(anyObject(BackupManager.class))).andReturn(SUCCESS).anyTimes();
        expect(action.getActionId()).andReturn("11111").anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getBackupManagerId()).andReturn("bcd-bro").anyTimes();
        action.setResult(ResultType.FAILURE);
        expectLastCall();
        action.setAdditionalInfo("Only RESTORE is allowed for -bro backup manager");
        expectLastCall();
        replay(action,jobExecutor, actionService);
        job.run();
        verify(action);
    }
    @Test
    public void run_createJobFromAction_executeJob_failedNotification_actionComplete() throws Exception {
        final Notification notification = createMock(Notification.class);
        final Action action = mockSuccessfulAction();
        final TestJob job = getTestJob(action);

        jobExecutor.setCurrentJob(Optional.of(job));
        expectLastCall();
        jobExecutor.setCurrentJob(Optional.empty());
        expectLastCall();
        expect(actionService.executeHousekeeping(anyObject(BackupManager.class))).andReturn(SUCCESS).anyTimes();
        replay(jobExecutor, actionService);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getResult()).andReturn(ResultType.SUCCESS).anyTimes();
        expect(action.getBackupManagerId()).andReturn("DEFAULT").anyTimes();

        notificationService.notifyAllActionCompleted(action);
        expectLastCall().andThrow(new NotificationFailedException(notification, new Exception("d")));
        replay(action);
        replay(notificationService);
        mockPVCMetricsCacheClearance();
        job.run();
        job.completeAction();

        assertTrue(job.isFinished());
        verify(action);
        verify(jobExecutor);
        verify(notificationService);
        verify(actionService);
    }

    @Test
    public void run_DeleteBackupJob_DoNotSendCompleteNotification() throws Exception {
        final Action action = mockSuccessfulAction();
        final TestJob job = getTestJob(action);

        jobExecutor.setCurrentJob(Optional.of(job));
        expectLastCall();
        jobExecutor.setCurrentJob(Optional.empty());
        expectLastCall();
        replay(jobExecutor);
        expect(action.getName()).andReturn(ActionType.DELETE_BACKUP).anyTimes();
        expect(action.getBackupManagerId()).andReturn("abc").anyTimes();
        expect(action.getResult()).andReturn(ResultType.SUCCESS).anyTimes();

        replay(action, notificationService);
        mockPVCMetricsCacheClearance();
        job.run();
        job.completeAction();

        assertTrue(job.isFinished());
        verify(action);
        verify(jobExecutor);
        verify(notificationService);
    }

    @Test
    public void run_secondJobIsSubmittedAfterFirstJobFinishes_bothJobsShouldExecute() throws Exception {
        final Action firstAction = mockSuccessfulAction();
        final Action secondAction = mockSuccessfulAction();

        final TestJob firstJob = getTestJob(firstAction);
        final TestJob secondJob = getTestJob(secondAction);

        jobExecutor.setCurrentJob(Optional.of(firstJob));
        expectLastCall();
        jobExecutor.setCurrentJob(Optional.of(secondJob));
        expectLastCall();
        jobExecutor.setCurrentJob(Optional.empty());
        expectLastCall().anyTimes();
        expect(actionService.executeHousekeeping(anyObject(BackupManager.class))).andReturn(SUCCESS).anyTimes();
        replay(jobExecutor, actionService);
        expect(firstAction.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(firstAction.getResult()).andReturn(ResultType.SUCCESS).anyTimes();
        expect(firstAction.getBackupManagerId()).andReturn("DEFAULT").anyTimes();
        notificationService.notifyAllActionCompleted(firstAction);
        expectLastCall();
        expect(secondAction.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(secondAction.getResult()).andReturn(ResultType.SUCCESS).anyTimes();
        expect(secondAction.getBackupManagerId()).andReturn("DEFAULT").anyTimes();
        notificationService.notifyAllActionCompleted(firstAction);
        expectLastCall();
        replay(firstAction);
        replay(secondAction);

        firstJob.run();
        secondJob.run();
        firstJob.completeAction();
        secondJob.completeAction();

        assertTrue(firstJob.isFinished());
        assertTrue(secondJob.isFinished());
        verify(firstAction);
        verify(secondAction);
        verify(jobExecutor);
        verify(actionService);
    }

    @Test
    public void run_secondJobIsSubmittedAfterFirstJobThrowsException_secondJobShouldExecute() throws Exception {
        final Action action = mockFailedAction("Boo");
        expect(action.getResult()).andReturn(ResultType.FAILURE).anyTimes();
        expect(action.getBackupManagerId()).andReturn("DEFAULT").anyTimes();
        replay(action);

        final ExceptionTestJob job = getExceptionTestJob(action);
        jobExecutor.setCurrentJob(Optional.of(job));
        expectLastCall();
        jobExecutor.setCurrentJob(Optional.empty());
        expectLastCall();
        replay(jobExecutor);

        job.run();
        job.completeAction();

        assertTrue(job.failedJob());
        assertFalse(job.isFinished());
        verify(action);
        verify(jobExecutor);
    }

    @Test
    public void monitor_monitorCalled_monitorIsCalled() {
        final Action action = mockSuccessfulAction();
        final TestJob job = getTestJob(action);
        final JobMonitor monitor = new JobMonitor(job,1);
        Awaitility.await().dontCatchUncaughtExceptions().atLeast((long) 0.5, TimeUnit.SECONDS).until(job::isMonitorCalled);
        monitor.stop();
    }

    private Action mockAction() {
        final OffsetDateTime dateTime = OffsetDateTime.now();
        final Action action = createMock(Action.class);
        expect(action.isHousekeepingDelete()).andReturn(false).anyTimes();
        expect(action.isExport()).andReturn(false).anyTimes();
        expect(action.isRestore()).andReturn(false).anyTimes();
        action.setState(ActionStateType.FINISHED);
        expectLastCall();
        action.setCompletionTime(anyObject(OffsetDateTime.class));
        expectLastCall();
        expect(action.getCompletionTime()).andReturn(dateTime).anyTimes();
        expect(action.getStartTime()).andReturn(dateTime).anyTimes();
        action.setLastUpdateTime(dateTime);
        expectLastCall();
        action.persist();
        expectLastCall();
        return action;
    }

    private Action mock_Action_with_params(boolean isHousekeepingDelete, boolean isExport, boolean isRestore) {
        final Action action = createMock(Action.class);

        // Customize the behavior of the mock based on parameters
        expect(action.isHousekeepingDelete()).andReturn(isHousekeepingDelete).anyTimes();
        expect(action.isExport()).andReturn(isExport).anyTimes();
        expect(action.isRestore()).andReturn(isRestore).anyTimes();
        return action;
    }

    private Action mockSuccessfulAction() {
        final Action action = mockAction();
        action.setResult(ResultType.SUCCESS);
        expectLastCall().anyTimes();
        action.setState(anyObject(ActionStateType.class));
        expectLastCall().anyTimes();
        action.setProgressPercentage(1.0);
        expectLastCall();
        expect(action.getActionId()).andReturn("1").anyTimes();
        action.updateOperationsTotalMetric();
        expectLastCall().times(0,1);
        action.updateLastOperationInfoMetric();
        expectLastCall().times(0,1);
        return action;
    }

    private Action mockFailedAction(final String reason) {
        final Action action = mockAction();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(action.getActionId()).andReturn("123-456-789-test-action-id").anyTimes();
        action.setResult(ResultType.FAILURE);
        expectLastCall();
        action.setState(anyObject(ActionStateType.class));
        expectLastCall().anyTimes();
        action.setAdditionalInfo(reason);
        expectLastCall();
        action.updateOperationsTotalMetric();
        expectLastCall().times(0,1);
        action.updateLastOperationInfoMetric();
        expectLastCall().times(0,1);
        return action;
    }

    private void mockPVCMetricsCacheClearance() {
        final CacheManager cacheManager = createMock(CacheManager.class);
        final Cache cache = createMock(Cache.class);
        final List<String> cacheNames = new ArrayList<>();

        cacheNames.add("PVCUsage");
        expect(cacheManager.getCacheNames()).andReturn(cacheNames).anyTimes();
        expect(cacheManager.getCache("PVCUsage")).andReturn(cache);
        cache.clear();
        when(SpringContext.getBean(CacheManager.class)).thenReturn(Optional.of(cacheManager));

        expectLastCall();
        replay(cacheManager, cache);
    }

    private TestJob getTestJob(final Action action) {
        BackupManager backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn("abc").anyTimes();
        replay(backupManager);
        final TestJob job = new TestJob();
        job.setAction(action);
        job.setJobExecutor(jobExecutor);
        job.setNotificationService(notificationService);
        job.setActionService(actionService);
        job.setActionRepository(actionRepository);
        job.setBackupManager(backupManager);
        return job;
    }

    private ExceptionTestJob getExceptionTestJob(final Action action) {
        BackupManager backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn("abc").anyTimes();
        replay(backupManager);
        final ExceptionTestJob job = new ExceptionTestJob();
        job.setAction(action);
        job.setJobExecutor(jobExecutor);
        job.setActionService(actionService);
        job.setActionRepository(actionRepository);
        job.setBackupManager(backupManager);
        return job;
    }

    private class TestJob extends Job {

        private boolean finished;
        private boolean monitorCalled;

        @Override
        protected void triggerJob() {
            //Not needed
        }

        @Override
        protected boolean didFinish() {
            return true;
        }

        @Override
        protected void completeJob() {
            finished = true;
        }

        @Override
        protected void fail() {
            //Not needed
        }

        public boolean isFinished() {
            return finished;
        }

        public boolean isMonitorCalled() {
            return monitorCalled;
        }

        @Override
        protected void monitor() {
            super.monitor();
            monitorCalled = true;
        }
    }

    private class ExceptionTestJob extends TestJob {

        private boolean failedJob;

        @Override
        public void triggerJob() {
            throw new RuntimeException("Boo");
        }

        @Override
        protected void fail() {
            failedJob = true;
        }

        public boolean failedJob() {
            return failedJob;
        }

    }

}
