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

import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.IMPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.RESTORE;
import static com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext.AvailableTasksService.NO_AVAILABLE_TASK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
public class AvailableTasksServiceBroBRMParallelActionParameterizedTest {

    private AvailableTasksService availableTasksService;
    private JobExecutor jobExecutor;
    private BackupManagerRepository backupManagerRepository;

    private static final String BRMID = "configuration-data";
    private static final String BACKUPNAME = "myBackup";

    private BackupManager backupManager;
    private Backup backup;

    private String queryBrm;
    private ActionType runningAction;
    private String runningBrm;
    private Set<AvailableTask> expected;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "configuration-data-bro", IMPORT, "different-brm", Set.of(new AvailableTask(RESTORE, BACKUPNAME)) },
            { "configuration-data-bro", IMPORT, "configuration-data", NO_AVAILABLE_TASK },
            { "configuration-data", RESTORE, "configuration-data-bro", NO_AVAILABLE_TASK },
            { "configuration-data", RESTORE, "DEFAULT-bro", Set.of(new AvailableTask(IMPORT)) },
        });
    }

    public AvailableTasksServiceBroBRMParallelActionParameterizedTest(final String queryBrm,
                                                                      final ActionType runningAction,
                                                                      final String runningBrm,
                                                                      final Set<AvailableTask> expected) {
        this.queryBrm = queryBrm;
        this.runningAction = runningAction;
        this.runningBrm = runningBrm;
        this.expected = expected;
    }

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
    public void broBRM_withExistingBackup_andImportActionRunningOnDifferentBRM() {
        when(backupManagerRepository.getBackupManager(queryBrm)).thenReturn(backupManager);
        when(backup.getStatus()).thenReturn(BackupStatus.COMPLETE);
        when(backup.getBackupId()).thenReturn(BACKUPNAME);

        final Job job = mock(Job.class);
        when(job.getBackupManagerId()).thenReturn(runningBrm);
        final Action action = mock(Action.class);
        when(job.getAction()).thenReturn(action);
        when(action.getName()).thenReturn(runningAction);
        when(action.getBackupManagerId()).thenReturn(runningBrm);
        when(jobExecutor.getRunningJobs()).thenReturn(List.of(job));
        final Set<AvailableTask> actual = Set.copyOf(availableTasksService.getAvailableTasks(queryBrm));
        assertEquals(expected, actual);
    }
}
