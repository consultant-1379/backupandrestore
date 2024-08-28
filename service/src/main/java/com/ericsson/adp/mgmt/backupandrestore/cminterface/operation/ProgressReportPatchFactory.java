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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ericsson.adp.mgmt.backupandrestore.action.Action;

/**
 * Creates patches to act on progressReports.
 */
@Service
public class ProgressReportPatchFactory {

    private static final String SCHEDULER_ELEMENT = "/scheduler";
    private static final String PROGRESS_REPORT_BASE = "/progress-report";
    private static final String PROGRESS_REPORT_ELEMENT = PROGRESS_REPORT_BASE + "/";
    private BackupManagerPatchFactory backupManagerPatchFactory;
    private BackupPatchFactory backupPatchFactory;

    /**
     * Creates a patch to add a progressReport to CM.
     * @param action to be added.
     * @return patch.
     */
    public List<ProgressReportPatch> getPatchToAddProgressReport(final Action action) {
        final List<ProgressReportPatch> patches = new ArrayList<>();
        final AtomicInteger index = new AtomicInteger();
        getPathToAction(action).forEach(path -> {
            if (index.getAndIncrement() == 0) {
                patches.add(new AddProgressReportInitialPatch(action, path));
            } else {
                patches.add(new UpdateProgressReportPatch(action, path));
            }
        });

        return patches;
    }

    /**
     * Gets CM Path to a backup backup manager progress-report.
     * @param action Includes the backupmanager id.
     * @return path to backup.
     */
    public String getPathToProgressReportBackupManager(final Action action) {
        return backupManagerPatchFactory.getPathToBackupManager(action.getBackupManagerId())
                + PROGRESS_REPORT_BASE;
    }

    /**
     * Gets CM Path to a backup backup manager progress-report.
     * @param action Includes the backupmanager id and backup id.
     * @return path to backup.
     */
    public String getPathToProgressReportBackup(final Action action) {
        return backupPatchFactory.getPathToPostBackup(action.getBackupManagerId(),
                action.getActionId()) +
                PROGRESS_REPORT_BASE;
    }

    /**
     * Creates a patch to update a progressReport in CM.
     * @param action to be updated.
     * @return patch.
     */
    public List<UpdateProgressReportPatch> getPatchToUpdateProgressReport(final Action action) {
        final List<UpdateProgressReportPatch> patches = new ArrayList<>();
        for (final String path : getPathToAction(action)) {
            patches.add(new UpdateProgressReportPatch(action, path));
        }
        return patches;
    }

    private List<String> getPathToAction(final Action action) {
        final List<String> paths = new ArrayList<>();
        if (action.isRestoreOrExport()) {
            paths.add(backupPatchFactory.getPathToBackup(action.getBackupManagerId(), action.getBackupName()) + PROGRESS_REPORT_ELEMENT);
        } else {
            paths.add(backupManagerPatchFactory.getPathToBackupManager(action.getBackupManagerId()) + PROGRESS_REPORT_ELEMENT);
        }
        if (action.isScheduledEvent() && !action.isRestoreOrExport()) {
            paths.add(backupManagerPatchFactory.getPathToBackupManager(action.getBackupManagerId()) + SCHEDULER_ELEMENT + PROGRESS_REPORT_ELEMENT);
        }
        return paths;
    }

    @Autowired
    public void setBackupManagerPatchFactory(final BackupManagerPatchFactory backupManagerPatchFactory) {
        this.backupManagerPatchFactory = backupManagerPatchFactory;
    }

    @Autowired
    public void setBackupPatchFactory(final BackupPatchFactory backupPatchFactory) {
        this.backupPatchFactory = backupPatchFactory;
    }

}
