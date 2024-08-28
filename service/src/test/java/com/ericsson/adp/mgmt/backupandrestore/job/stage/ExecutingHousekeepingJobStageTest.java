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

import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager.DEFAULT_BACKUP_MANAGER_ID;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_DISABLED;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.job.HousekeepingJob;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;

public class ExecutingHousekeepingJobStageTest {

    private ExecutingHousekeepingJobStage executingHousekeepingJobStage;
    private NotificationService notificationService;
    private Action action;
    private Backup backup;
    private Backup backup2;

    @Before
    public void setup() {
        final HousekeepingJob job = createMock(HousekeepingJob.class);
        job.updateBackupManagerHousekeeping();
        expectLastCall().anyTimes();
        action = createMock(Action.class);
        expect(action.getName()).andReturn(ActionType.HOUSEKEEPING).anyTimes();
        action.setResult(anyObject(ResultType.class));
        expectLastCall();
        action.setAdditionalInfo(anyString());
        expectLastCall();
        notificationService = createMock(NotificationService.class);
        executingHousekeepingJobStage = new ExecutingHousekeepingJobStage(new ArrayList<>(), job, notificationService);
        backup = createMock(Backup.class);
        expect(backup.getName()).andReturn("myBackup").anyTimes();
        expect(backup.getBackupId()).andReturn("myBackup1").anyTimes();
        backup2 = createMock(Backup.class);
        expect(backup2.getName()).andReturn("myBackup2").anyTimes();
        expect(backup2.getBackupId()).andReturn("myBackup2").anyTimes();
        replay(action, backup, backup2);
    }

    @Test
    public void trigger_AutoDeleteDisable_NoExecuted() throws Exception {
        final HousekeepingJob job = createJob(AUTO_DELETE_DISABLED, true);
        executingHousekeepingJobStage = new ExecutingHousekeepingJobStage(new ArrayList<>(), job, notificationService);
        executingHousekeepingJobStage.trigger();
        assertTrue(executingHousekeepingJobStage.isStageSuccessful());
        verify(job);
    }

    @Test
    public void trigger_MaximumNumber_NoReached() throws Exception {
        final HousekeepingJob job = createJob(AUTO_DELETE_ENABLED, false);
        executingHousekeepingJobStage = new ExecutingHousekeepingJobStage(new ArrayList<>(), job, notificationService);
        executingHousekeepingJobStage.trigger();
        assertTrue(executingHousekeepingJobStage.isStageSuccessful());
        verify(job);
    }

    @Test
    public void trigger_MaximumNumberAutoDeleteEnable_deleteBackups() throws Exception {
        final HousekeepingJob job = createJob(AUTO_DELETE_ENABLED, true);
        executingHousekeepingJobStage = new ExecutingHousekeepingJobStage(new ArrayList<>(), job, notificationService);
        executingHousekeepingJobStage.trigger();
        assertTrue(executingHousekeepingJobStage.isStageSuccessful());
        verify(job);
    }

    @Test
    public void isJobFinished_executingHousekeepingJobStage_false() throws Exception {
        assertFalse(executingHousekeepingJobStage.isJobFinished());
    }

    @Test
    public void moveToNextStage_triggerExecuted_PostActionHousekeepingJobStage() throws Exception {
        final HousekeepingJob job = createJob(AUTO_DELETE_ENABLED, false);
        executingHousekeepingJobStage = new ExecutingHousekeepingJobStage(new ArrayList<>(), job, notificationService);
        executingHousekeepingJobStage.trigger();
        assertTrue(executingHousekeepingJobStage.moveToNextStage() instanceof PostActionHousekeepingJobStage );
    }

    @Test
    public void moveToNextStage_triggerNotExecuted_failedHousekeepingJobStage() throws Exception {
        assertTrue(executingHousekeepingJobStage.moveToNextStage() instanceof FailedHousekeepingJobStage );
    }

    private HousekeepingJob createJob(final String autoDelete, final boolean MaxNumberBackups) throws InterruptedException {
        final HousekeepingJob job = createMock(HousekeepingJob.class);
        expect(job.getAction()).andReturn(action).anyTimes();
        expect(job.getAutoDelete()).andReturn(autoDelete).anyTimes();
        expect(job.isMaxNumberBackups()).andReturn(MaxNumberBackups).anyTimes();
        expect(job.executeHousekeeping()).andReturn(new ArrayList<String>(Arrays.asList(backup.getBackupId(), backup2.getBackupId()))).anyTimes();
        expect(job.getBackupManagerId()).andReturn(DEFAULT_BACKUP_MANAGER_ID).anyTimes();
        job.updateBackupManagerHousekeeping();
        expectLastCall().anyTimes();
        replay(job);

        return job;
    }

}
