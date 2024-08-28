/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************/
package com.ericsson.adp.mgmt.backupandrestore.rest.hypertext;

import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.CREATE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.DELETE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.EXPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.IMPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.RESTORE;
import static com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext.AvailableTasksService.NO_AVAILABLE_TASK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.job.Job;
import com.ericsson.adp.mgmt.backupandrestore.job.JobExecutor;
import com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext.AvailableTasksService;
import com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext.AvailableTasksService.AvailableTask;

public class AvailableTasksServiceTest {

    private static final List<Backup> NO_BACKUP = List.of();
    private AvailableTasksService availableTasksService;
    private JobExecutor jobExecutor;
    private BackupManagerRepository backupManagerRepository;

    private static final String BRMID = "configuration-data";
    private static final String BACKUPNAME = "myBackup";

    private BackupManager backupManager;
    private Backup backup;

    @Before
    public void setUp() {
        jobExecutor = mock(JobExecutor.class);
        backupManagerRepository = mock(BackupManagerRepository.class);
        availableTasksService = new AvailableTasksService();
        availableTasksService.setJobExecutor(jobExecutor);
        availableTasksService.setBackupManagerRepository(backupManagerRepository);
        availableTasksService.setIsLimitedParallelActionsEnabled(true);

        backupManager = mock(BackupManager.class);
        when(backupManagerRepository.getBackupManager(BRMID)).thenReturn(backupManager);
        backup = mock(Backup.class);
        when(backupManager.getBackups(Ownership.OWNED)).thenReturn(List.of(backup));
    }

    @Test
    public void regularBRM_withNoExistingBackup_andNoRunningAction_canRunCreateBackupAndImport() {
        when(backupManager.getBackups(Ownership.OWNED)).thenReturn(NO_BACKUP);
        final Set<AvailableTask> expected = Set.of(new AvailableTask(CREATE_BACKUP), new AvailableTask(IMPORT));
        final Set<AvailableTask> actual = Set.copyOf(availableTasksService.getAvailableTasks(BRMID));
        assertTrue(expected.equals(actual));
    }

    @Test
    public void regularBRM_withExistingBackup_andNoRunningAction_canRunAllActions() {
        when(backup.getStatus()).thenReturn(BackupStatus.COMPLETE);
        when(backup.getBackupId()).thenReturn(BACKUPNAME);

        final Set<AvailableTask> actual = Set.copyOf(availableTasksService.getAvailableTasks(BRMID));
        final Set<AvailableTask> expected = Set.of(new AvailableTask(CREATE_BACKUP),
                                                   new AvailableTask(IMPORT),
                                                   new AvailableTask(RESTORE, BACKUPNAME),
                                                   new AvailableTask(DELETE_BACKUP, BACKUPNAME),
                                                   new AvailableTask(EXPORT, BACKUPNAME));
        assertTrue(expected.equals(actual));
    }

    @Test
    public void regularBRM_withExistingBackup_andTwoRunningActions_cannotRunAnyNewAction() {
        when(jobExecutor.getRunningJobs()).thenReturn(List.of(mock(Job.class), mock(Job.class)));
        when(backup.getStatus()).thenReturn(BackupStatus.COMPLETE);
        final Set<AvailableTask> actual = Set.copyOf(availableTasksService.getAvailableTasks(BRMID));
        assertTrue(NO_AVAILABLE_TASK.equals(actual));
    }

    @Test
    public void regularBRM_withExistingIncompleteBackup_andNoRunningAction_canRunCreateBackupAndImport() {
        when(backup.getStatus()).thenReturn(BackupStatus.INCOMPLETE);
        final Set<AvailableTask> expected = Set.of(new AvailableTask(CREATE_BACKUP),
                new AvailableTask(IMPORT));
        final Set<AvailableTask> actual = Set.copyOf(availableTasksService.getAvailableTasks(BRMID));
        assertTrue(expected.equals(actual));
    }

    @Test
    public void regularBRM_withLockedBackup_canRunCreateBackupAndImport() {
        when(backup.getStatus()).thenReturn(BackupStatus.COMPLETE);
        when(backup.getActionLock()).thenReturn(Optional.of(mock(Action.class)));
        final Set<AvailableTask> actual = Set.copyOf(availableTasksService.getAvailableTasks(BRMID));
        final Set<AvailableTask> expected = Set.of(new AvailableTask(CREATE_BACKUP),
                                                   new AvailableTask(IMPORT));
        assertTrue(expected.equals(actual));
    }

    @Test
    public void regularBRM_withExistingBackup_anActionIsRunning_ParallelActionIsDisabled_cannotRunAnyNewAction() {
        availableTasksService.setIsLimitedParallelActionsEnabled(false);
        when(backup.getStatus()).thenReturn(BackupStatus.COMPLETE);
        final Job job = mock(Job.class);
        when(job.getBackupManagerId()).thenReturn("DifferentBRM");
        final Action action = mock(Action.class);
        when(job.getAction()).thenReturn(action);
        when(action.getName()).thenReturn(CREATE_BACKUP);
        when(jobExecutor.getRunningJobs()).thenReturn(List.of(job));
        final Set<AvailableTask> actual = Set.copyOf(availableTasksService.getAvailableTasks(BRMID));
        assertTrue(NO_AVAILABLE_TASK.equals(actual));
    }

    @Test
    public void broBRM_withExistingBackup_andNoRunningAction_canRunConfigRestore() {
        final String broBRM = "configuration-data-bro";
        when(backupManagerRepository.getBackupManager(broBRM)).thenReturn(backupManager);
        when(backup.getStatus()).thenReturn(BackupStatus.COMPLETE);
        when(backup.getBackupId()).thenReturn(BACKUPNAME);
        final Set<AvailableTask> expected = Set.of(new AvailableTask(RESTORE, BACKUPNAME));
        final Set<AvailableTask> actual = Set.copyOf(availableTasksService.getAvailableTasks(broBRM));
        assertTrue(expected.equals(actual));
    }

    @Test
    public void vBRM_withNoOwnedBackup_andNoReadableBackup_andNoRunningAction_canRunCreateBackupAndImport() {
        final String vBRM = "configuration-data-test-agent";
        when(backupManagerRepository.getBackupManager(vBRM)).thenReturn(backupManager);
        when(backupManager.isVirtual()).thenReturn(true);
        when(backupManager.getBackups(Ownership.OWNED)).thenReturn(NO_BACKUP);
        when(backupManager.getBackups(Ownership.READABLE)).thenReturn(NO_BACKUP);

        final Set<AvailableTask> expected = Set.of(new AvailableTask(CREATE_BACKUP),
                                                    new AvailableTask(IMPORT));
        final Set<AvailableTask> actual = Set.copyOf(availableTasksService.getAvailableTasks(vBRM));
        assertTrue(expected.equals(actual));
    }

    @Test
    public void vBRM_withOwnedBackup_andNoRunningAction_canRunAllActions() {
        final String vBRM = "configuration-data-test-agent";
        when(backupManagerRepository.getBackupManager(vBRM)).thenReturn(backupManager);
        when(backupManager.isVirtual()).thenReturn(true);
        when(backup.getStatus()).thenReturn(BackupStatus.COMPLETE);
        when(backup.getBackupId()).thenReturn(BACKUPNAME);
        final Set<AvailableTask> expected = Set.of(new AvailableTask(CREATE_BACKUP),
                                                    new AvailableTask(IMPORT),
                                                    new AvailableTask(RESTORE, BACKUPNAME),
                                                    new AvailableTask(DELETE_BACKUP, BACKUPNAME),
                                                    new AvailableTask(EXPORT, BACKUPNAME));
        final Set<AvailableTask> actual = Set.copyOf(availableTasksService.getAvailableTasks(vBRM));
        assertTrue(expected.equals(actual));
    }

    @Test
    public void vBRM_withNoOwnedBackup_butWithReadableBackup_andNoRunningAction_canRunCreateBackupImportAndRestore() {
        final String vBRM = "configuration-data-test-agent";
        when(backupManagerRepository.getBackupManager(vBRM)).thenReturn(backupManager);
        when(backupManager.isVirtual()).thenReturn(true);
        when(backupManager.getBackups(Ownership.OWNED)).thenReturn(NO_BACKUP);
        when(backupManager.getBackups(Ownership.READABLE)).thenReturn(List.of(backup));
        when(backup.getStatus()).thenReturn(BackupStatus.COMPLETE);
        when(backup.getBackupId()).thenReturn(BACKUPNAME);

        final Set<AvailableTask> expected = Set.of(new AvailableTask(CREATE_BACKUP),
                                                    new AvailableTask(IMPORT),
                                                    new AvailableTask(RESTORE, BACKUPNAME));
        final Set<AvailableTask> actual = Set.copyOf(availableTasksService.getAvailableTasks(vBRM));
        assertEquals(expected, actual);
    }
}
