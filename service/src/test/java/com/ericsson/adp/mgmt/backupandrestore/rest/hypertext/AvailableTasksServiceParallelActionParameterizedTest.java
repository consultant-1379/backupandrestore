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
import static com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus.COMPLETE;
import static com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus.INCOMPLETE;
import static com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext.AvailableTasksService.NO_AVAILABLE_TASK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.job.Job;
import com.ericsson.adp.mgmt.backupandrestore.job.JobExecutor;
import com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext.AvailableTasksService;
import com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext.AvailableTasksService.AvailableTask;

@RunWith(Parameterized.class)
public class AvailableTasksServiceParallelActionParameterizedTest {

    private final String BRMID = "configuration-data";
    private static final String BACKUPNAME = "myBackup";

    private AvailableTasksService availableTasksService;
    private JobExecutor jobExecutor;
    private BackupManagerRepository backupManagerRepository;

    private BackupManager backupManager;
    private ActionType runningActionType;
    private boolean isRunningInSameBRM;
    private Optional<Backup> existingBackup;
    private BackupStatus backupStatus;
    private Action backupLock;
    private Set<AvailableTask> expectedAvailableTasks;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Backup backup = mock(Backup.class);
        when(backup.getBackupId()).thenReturn(BACKUPNAME);
        Action lock = mock(Action.class);
        return Arrays.asList(new Object[][] {
            { CREATE_BACKUP, true, Optional.empty(), null, null, NO_AVAILABLE_TASK },
            { CREATE_BACKUP, true, Optional.of(backup), COMPLETE, lock, NO_AVAILABLE_TASK },
            { CREATE_BACKUP, true, Optional.of(backup), COMPLETE, null, Set.of(new AvailableTask(EXPORT, BACKUPNAME)) },
            { CREATE_BACKUP, false, Optional.empty(), null, null, NO_AVAILABLE_TASK },
            { CREATE_BACKUP, false, Optional.of(backup), INCOMPLETE, null, NO_AVAILABLE_TASK },
            { CREATE_BACKUP, false, Optional.of(backup), COMPLETE, lock, NO_AVAILABLE_TASK },
            { CREATE_BACKUP, false, Optional.of(backup), COMPLETE, null, NO_AVAILABLE_TASK },
            { EXPORT, true, Optional.of(backup), COMPLETE, null, Set.of(new AvailableTask(CREATE_BACKUP)) },
            { EXPORT, false, Optional.of(backup), COMPLETE, null, NO_AVAILABLE_TASK },
            { IMPORT, true, Optional.empty(), null, null, NO_AVAILABLE_TASK },
            { IMPORT, true, Optional.of(backup), INCOMPLETE, null, NO_AVAILABLE_TASK },
            { IMPORT, true, Optional.of(backup), COMPLETE, lock, NO_AVAILABLE_TASK },
            { IMPORT, true, Optional.of(backup), COMPLETE, null, Set.of(new AvailableTask(RESTORE, BACKUPNAME)) },
            { IMPORT, false, Optional.empty(), null, null, NO_AVAILABLE_TASK },
            { IMPORT, false, Optional.of(backup), INCOMPLETE, null, NO_AVAILABLE_TASK },
            { IMPORT, false, Optional.of(backup), COMPLETE, lock, NO_AVAILABLE_TASK },
            { IMPORT, false, Optional.of(backup), COMPLETE, null, Set.of(new AvailableTask(RESTORE, BACKUPNAME)) },
            { RESTORE, true, Optional.of(backup), COMPLETE, null, Set.of(new AvailableTask(IMPORT)) },
            { RESTORE, false, Optional.of(backup), COMPLETE, null, Set.of(new AvailableTask(IMPORT)) },
            { DELETE_BACKUP, true, Optional.of(backup), COMPLETE, null, NO_AVAILABLE_TASK },
            { DELETE_BACKUP, false, Optional.of(backup), COMPLETE, null, NO_AVAILABLE_TASK },
        });
    }

    public AvailableTasksServiceParallelActionParameterizedTest(final ActionType runningActionType,
                                                                final boolean isRunningInSameBRM,
                                                                final Optional<Backup> existingBackup,
                                                                final BackupStatus backupStatus,
                                                                final Action backupLock,
                                                                final Set<AvailableTask> expectedAvailableTasks) {
        this.existingBackup = existingBackup;
        this.backupStatus = backupStatus;
        this.backupLock = backupLock;
        this.runningActionType = runningActionType;
        this.isRunningInSameBRM = isRunningInSameBRM;
        this.expectedAvailableTasks = expectedAvailableTasks;
    }

    @Before
    public void setUp() {
        jobExecutor = mock(JobExecutor.class);
        backupManagerRepository = mock(BackupManagerRepository.class);
        availableTasksService = new AvailableTasksService();
        availableTasksService.setJobExecutor(jobExecutor);
        availableTasksService.setBackupManagerRepository(backupManagerRepository);
        availableTasksService.setIsLimitedParallelActionsEnabled(true);
    }

    @Test
    public void regularBRM_andActionRunningOnSameBRM() {

        backupManager = mock(BackupManager.class);
        when(backupManagerRepository.getBackupManager(BRMID)).thenReturn(backupManager);
        existingBackup.ifPresent(backup -> {
            when(backupManager.getBackups(Ownership.OWNED)).thenReturn(List.of(backup));
            when(backup.getStatus()).thenReturn(backupStatus);
            when(backup.getActionLock()).thenReturn(Optional.ofNullable(backupLock));
        });
        final Job job = mock(Job.class);
        final String runningBrmId = isRunningInSameBRM ? BRMID : "DifferentBRM";
        when(job.getBackupManagerId()).thenReturn(runningBrmId);
        final Action action = mock(Action.class);
        when(job.getAction()).thenReturn(action);
        when(action.getName()).thenReturn(runningActionType);
        when(action.getBackupManagerId()).thenReturn(runningBrmId);
        when(jobExecutor.getRunningJobs()).thenReturn(List.of(job));
        final Set<AvailableTask> actual = Set.copyOf(availableTasksService.getAvailableTasks(BRMID));
        assertEquals(expectedAvailableTasks, actual);
    }
}
