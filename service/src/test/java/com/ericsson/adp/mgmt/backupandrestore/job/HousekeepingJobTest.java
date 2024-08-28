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
package com.ericsson.adp.mgmt.backupandrestore.job;

import static com.ericsson.adp.mgmt.backupandrestore.action.ResultType.SUCCESS;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.easymock.EasyMock;
import org.easymock.IMockBuilder;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionService;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupLocationService;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.backup.SoftwareVersion;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.exception.TimedOutHousekeepingException;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStage;

public class HousekeepingJobTest {

    private static final String BACKUP_MANAGER_ID = "BACKUP_MANAGER_ID";

    private HousekeepingJob job;
    private BackupLocationService backupLocationService;
    private BackupManager backupManager;
    private BackupRepository backupRepository;
    private JobStage<HousekeepingJob> stage;
    private JobStage<HousekeepingJob> failedStage;
    private ReentrantReadWriteLock readwriteLock;
    private ActionService actionService;
    private Action action;
    private ReentrantLock accessControl;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        final IMockBuilder<JobStage> mockBuilder = EasyMock.createMockBuilder(JobStage.class);
        mockBuilder.addMockedMethod("trigger");
        failedStage = mockBuilder.createMock();

        job = new HousekeepingJob();
        backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn(BACKUP_MANAGER_ID).anyTimes();
        expect(backupManager.getHousekeeping()).andReturn(new Housekeeping(2, AUTO_DELETE_ENABLED, null, null)).anyTimes();
        backupRepository = createMock(BackupRepository.class);
        backupLocationService = createMock(BackupLocationService.class);
        readwriteLock = createMock(ReentrantReadWriteLock.class);
        actionService = createMock(ActionService.class);
        accessControl = createMock(ReentrantLock.class);

        stage = EasyMock.createMock(JobStage.class);
        expect (stage.getProgressPercentage()).andReturn(0.5).anyTimes();

        job.setBackupManager(backupManager);
        job.setJobStage(stage);
        job.setBackupRepository(backupRepository);
    }

    @Test
    public void failHousekeeping_NoBackupsToDelete_returnListOfBackupsEmpty () throws Exception {
        action = getAction();
        job.setAction(action);
        expect(backupRepository.getBackupsForAutoDeletion(EasyMock.anyString(), EasyMock.anyInt()))
        .andReturn(getBackups(0));
        replay(action, backupManager, backupLocationService, backupRepository, stage);
        final List<String> backupsOrdered = job.executeHousekeeping();
        assertTrue(backupsOrdered.isEmpty());
    }

    @Test
    public void Housekeeping_TwoBackupsToDelete_returnListOfBackups () throws Exception {
        action = getAction();
        job.setAction(action);
        expect(actionService.executeAndWait(anyObject(), anyObject())).andReturn(SUCCESS).anyTimes();
        expect(backupRepository.getBackupsForAutoDeletion(EasyMock.anyString(), EasyMock.anyInt()))
        .andReturn(getBackups(2));
        expect(readwriteLock.getReadLockCount()).andReturn(1).anyTimes();
        replay(action, backupManager, backupLocationService, backupRepository, stage, actionService, readwriteLock);
        job.setLock(readwriteLock);
        job.setActionService(actionService);
        final List<String> backupsOrdered = job.executeHousekeeping();
        assertFalse(backupsOrdered.isEmpty());
    }

    @Test
    public void Housekeeping_TwoBackupsToDelete_failedExecution () throws Exception {
        failedStage.trigger();
        expectLastCall();
        expect(stage.moveToFailedStage()).andReturn(failedStage);

        action = getAction();
        action.setResult(ResultType.FAILURE);
        expectLastCall();
        job.setAction(action);
        expect(actionService.executeAndWait(anyObject(), anyObject())).andReturn(ResultType.FAILURE).anyTimes();
        expect(backupRepository.getBackupsForAutoDeletion(EasyMock.anyString(), EasyMock.anyInt()))
        .andReturn(getBackups(2));
        expect(readwriteLock.getReadLockCount()).andReturn(1).anyTimes();
        replay(action, backupManager, backupLocationService, backupRepository, stage, failedStage, actionService, readwriteLock);
        job.setLock(readwriteLock);
        job.setActionService(actionService);
        final List<String> backupsOrdered = job.executeHousekeeping();
        assertTrue(backupsOrdered.get(0).equalsIgnoreCase("test - failure"));
        assertTrue(backupsOrdered.get(1).equalsIgnoreCase("test - failure"));
    }

    @Test
    public void completeJob_FinishedSuccessfully() throws Exception {
        EasyMock.expect(stage.isStageSuccessful()).andReturn(true).anyTimes();
        replay (stage, backupManager);
        job.completeJob();
    }

    @Test
    (expected = TimedOutHousekeepingException.class)
    public void Housekeeping_FourBackupsToDelete_returnListOfBackups () throws Exception {
        new ReentrantReadWriteLock();
        action = getAction();
        expect(actionService.executeAndWait(anyObject(), anyObject())).andReturn(SUCCESS).anyTimes();

        job.setAction(action);
        expect(backupRepository.getBackupsForAutoDeletion(EasyMock.anyString(), EasyMock.anyInt()))
        .andReturn(getBackups(4));
        expect(accessControl.tryLock(EasyMock.anyLong(), EasyMock.anyObject())).andReturn(false);
        accessControl.unlock();
        expectLastCall();
        replay(action, backupManager, backupLocationService, backupRepository,
                stage, actionService, accessControl);
        job.setAccessControl(accessControl);
        job.setTimeoutBackupDelete(1);
        job.setLock(readwriteLock);
        job.setActionService(actionService);
        job.setJobStage(stage);
        job.executeHousekeeping();
    }

    private Action getAction() {
        Action action;
        action = createMock(Action.class);
        action.setProgressPercentage(EasyMock.anyDouble());
        expectLastCall().anyTimes();
        action.persist();
        expectLastCall().anyTimes();
        action.setState(EasyMock.anyObject());
        expectLastCall().anyTimes();
        expect(action.getActionId()).andReturn("1").anyTimes();
        expect(action.isExecutedAsTask()).andReturn(false).anyTimes();
        expect(action.getMaximumManualBackupsStored()).andReturn(1).anyTimes();
        expect(action.getAutoDelete()).andReturn(AUTO_DELETE_ENABLED).anyTimes();
        return action;

    }

    private List<Backup> getBackups(final int totalBackups){
        final ArrayList<Backup> backupOrdered=new ArrayList<Backup>();
        for (int i=0; i < totalBackups; i++) {
            backupOrdered.add(mockBackup(BackupStatus.COMPLETE));
        }
        return backupOrdered;
    }

    private Backup mockBackup(final BackupStatus status) {
        final SoftwareVersion softwareVersion = new SoftwareVersion();
        softwareVersion.setAgentId("1");
        softwareVersion.setDate("");
        softwareVersion.setDescription("");
        softwareVersion.setProductName("");
        softwareVersion.setProductNumber("");
        softwareVersion.setProductRevision("");
        softwareVersion.setType("");

        final Backup backup = createMock(Backup.class);
        expect(backup.getBackupId()).andReturn("test").anyTimes();
        expect(backup.getName()).andReturn("backup").anyTimes();
        expect(backup.getStatus()).andReturn(status).anyTimes();
        expect(backup.getSoftwareVersions()).andReturn(Arrays.asList(softwareVersion)).anyTimes();
        replay(backup);
        return backup;
    }
}
