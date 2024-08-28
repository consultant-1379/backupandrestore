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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;

/**
 * Creates patches to act on periodic event.
 */
@Service
public class PeriodicEventPatchFactory {
    private BackupManagerPatchFactory backupManagerPatchFactory;
    private BackupManagerRepository backupManagerRepository;

    /**
     * Creates a patch to add a periodic event to CM.
     *
     * @param periodicEvent
     *            event to be added
     * @return patch to add periodic event
     */
    public AddPeriodicEventPatch getPatchToAddPeriodicEvent(final PeriodicEvent periodicEvent) {
        return new AddPeriodicEventPatch(periodicEvent, getPathToPeriodicEvent(periodicEvent.getBackupManagerId()));
    }

    /**
     * Creates a patch to update a periodic event in CM.
     * @param event to be updated.
     * @return patch.
     */
    public UpdatePeriodicEventPatch getPatchToUpdatePeriodicEvent(final PeriodicEvent event) {
        return new UpdatePeriodicEventPatch(event, getPathToEvent(event.getBackupManagerId(), event.getEventId()));
    }

    /**
     * Creates a patch to delete a periodic event in CM.
     * @param event to be deleted.
     * @return patch.
     */
    public DeletePeriodicEventPatch getPatchToDeletePeriodicEvent(final PeriodicEvent event) {
        return new DeletePeriodicEventPatch(getPathToEvent(event.getBackupManagerId(), event.getEventId()));
    }

    /**
     * Gets CM Path to a periodic event.
     * @param backupManagerId owner of event.
     * @param eventId event to look for.
     * @return path to periodic event.
     */
    public String getPathToEvent(final String backupManagerId, final String eventId) {
        return getPathToPeriodicEvent(backupManagerId) + String.valueOf(getPeriodicEventIndex(backupManagerId, eventId));
    }

    private String getPathToPeriodicEvent(final String backupManagerId) {
        return backupManagerPatchFactory.getPathToBackupManager(backupManagerId) + "/scheduler/periodic-event/";
    }

    private int getPeriodicEventIndex(final String backupManagerId, final String eventId) {
        return backupManagerRepository
                .getBackupManager(backupManagerId)
                .getScheduler()
                .getPeriodicEventIndex(eventId);
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
