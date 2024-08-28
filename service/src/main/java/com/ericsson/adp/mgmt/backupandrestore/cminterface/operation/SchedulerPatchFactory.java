/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.cminterface.operation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;

/**
 * Creates patches to act on scheduler.
 */
@Service
public class SchedulerPatchFactory {
    private BackupManagerPatchFactory backupManagerPatchFactory;
    private BackupManagerRepository backupManagerRepository;

    /**
     * Creates a patch to add a scheduler to CM.
     * @param scheduler to be added
     * @return patch to add scheduler
     */
    public AddSchedulerPatch getPatchToAddScheduler(final Scheduler scheduler) {
        return new AddSchedulerPatch(scheduler, getPathToScheduler(scheduler.getBackupManagerId()));
    }

    /**
     * Creates a patch to update a scheduler in CM.
     * @param scheduler to be updated
     * @return patch to update scheduler
     */
    public UpdateSchedulerPatch getPatchToUpdateScheduler(final Scheduler scheduler) {
        return new UpdateSchedulerPatch(scheduler, getActions(scheduler),
                getPathToScheduler(scheduler.getBackupManagerId()));
    }

    private List<Action> getActions(final Scheduler scheduler) {
        return backupManagerRepository.getBackupManager(scheduler.getBackupManagerId()).getActions();
    }

    private String getPathToScheduler(final String backupManagerId) {
        return backupManagerPatchFactory.getPathToBackupManager(backupManagerId) + "/scheduler";
    }

    @Autowired
    public void setBackupManagerPatchFactory(final BackupManagerPatchFactory backupManagerPatchFactory) {
        this.backupManagerPatchFactory = backupManagerPatchFactory;
    }

    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

}
