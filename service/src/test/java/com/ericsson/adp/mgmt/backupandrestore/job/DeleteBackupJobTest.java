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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.anyBoolean;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.*;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMClient;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;

public class DeleteBackupJobTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private DeleteBackupJob job;
    private Path rootBackupFolder;
    private Backup backup;
    private BackupManager backupManager;
    private BackupRepository backupRepository;
    private ActionRepository actionRepository;
    private CMMediatorService cmMediatorService;
    private CMMClient cmmClient;

    @Before
    public void setup() throws Exception {
        job = new DeleteBackupJob();
        cmMediatorService = EasyMock.createMock(CMMediatorService.class);

        final BackupNamePayload payload = new BackupNamePayload();
        payload.setBackupName("myBackup");
        final Action action = createMock(Action.class);
        cmmClient = new CMMClient();
        cmmClient.setFlagEnabled(true);
        cmmClient.setMaxAttempts("5");
        cmmClient.setMaxDelay("3000");
        expect(action.getPayload()).andReturn(payload);

        backup = createMock(Backup.class);
        backup.setStatus(BackupStatus.CORRUPTED);
        expectLastCall().anyTimes();
        backup.persist();
        expectLastCall().anyTimes();
        expect(backup.getBackupId()).andReturn("myBackup").anyTimes();
        backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackup("myBackup", Ownership.OWNED)).andReturn(backup);
        expect(backupManager.getBackupManagerId()).andReturn("bm").anyTimes();
        backupManager.backupManagerLevelProgressReportResetCreated();
        expectLastCall().anyTimes();

        cmMediatorService.prepareCMMediator(anyBoolean());
        expectLastCall().anyTimes();

        backupRepository = createMock(BackupRepository.class);
        actionRepository = createMock(ActionRepository.class);
        backupRepository.deleteBackup(backup, backupManager);
        expectLastCall();

        rootBackupFolder = folder.getRoot().toPath().resolve("123");
        Files.createDirectories(rootBackupFolder);
        Files.write(rootBackupFolder.resolve("a.txt"), "qwe".getBytes());

        final BackupLocationService backupLocationService = createMock(BackupLocationService.class);
        expect(backupLocationService.getBackupFolder("bm", "myBackup")).andReturn(new BackupFolder(rootBackupFolder));

        expect(cmMediatorService.getCMMClient()).andReturn(cmmClient);
        expect(cmMediatorService.isConfigurationinCMM()).andReturn(false);
        replay(action, backupManager, backup, backupRepository, backupLocationService, cmMediatorService);

        job.setAction(action);
        job.setBackupManager(backupManager);
        job.setBackupRepository(backupRepository);
        job.setBackupLocationService(backupLocationService);
        job.setActionRepository(actionRepository);
        job.setCmMediatorService(cmMediatorService);
    }

    @Test
    public void triggerJob_jobToDeleteBackup_deletesBackup() throws Exception {
        job.triggerJob();

        verify(backupManager);
        verify(backupRepository);
        assertFalse(rootBackupFolder.toFile().exists());
    }

    @Test
    public void triggerJob_backupWithNoData_deletesBackup() throws Exception {
        rootBackupFolder = folder.getRoot().toPath().resolve("456");

        assertFalse(rootBackupFolder.toFile().exists());

        final BackupLocationService backupLocationService = createMock(BackupLocationService.class);
        expect(backupLocationService.getBackupFolder(backupManager.getBackupManagerId(), "myBackup")).andReturn(new BackupFolder(rootBackupFolder));
        replay(backupLocationService);

        job.setBackupLocationService(backupLocationService);

        job.triggerJob();

        verify(backupManager);
        verify(backupRepository);
        assertFalse(rootBackupFolder.toFile().exists());
    }

    @Test
    public void didFinish_job_finishesWhenBackupManagerNoLongerHasBackup() throws Exception {
        job.triggerJob();

        backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackups(Ownership.OWNED)).andReturn(Arrays.asList(backup));
        replay(backupManager);
        job.setBackupManager(backupManager);

        assertFalse(job.didFinish());

        backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackups(Ownership.OWNED)).andReturn(Arrays.asList());
        replay(backupManager);
        job.setBackupManager(backupManager);

        assertTrue(job.didFinish());
    }

    @Test
    public void completeJob_job_doesNothing() throws Exception {
        job.triggerJob();

        job.completeJob();

        verify(backup);
    }

    @Test
    public void completeJob_job_resetCM(){
        cmmClient.setFlagEnabled(true);
        cmmClient.setInitialized(true);
        job.triggerJob();

        job.completeJob();

        verify(backup);
        cmmClient.setFlagEnabled(false);
        cmmClient.setInitialized(false);
    }

    @Test
    public void fail_backupToDelete_setItToCorrupted() throws Exception {
        job.triggerJob();

        job.fail();

        verify(backup);
    }

    @Test
    public void fail_backupWasNotFound_doesNothing() throws Exception {
        job.fail();

        verify(backup);
    }
}
