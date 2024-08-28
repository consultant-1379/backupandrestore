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

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;

/**
 * Creates patches to act on housekeeping.
 */
@Service
public class HousekeepingPatchFactory {

    private BackupManagerPatchFactory backupManagerPatchFactory;

    /**
     * Creates a patch to add a housekeeping to CM.
     * @param housekeeping to be added
     * @return patch to add housekeeping
     */
    public AddHousekeepingPatch getPatchToAddHousekeeping(final Housekeeping housekeeping) {
        return new AddHousekeepingPatch(housekeeping, getPathToHousekeeping(housekeeping.getBackupManagerId()));
    }

    /**
     * Creates a patch to update a housekeeping in CM.
     * @param housekeeping to be updated
     * @return patch to update housekeeping
     */
    public UpdateHousekeepingPatch getPatchToUpdateHousekeeping(final Housekeeping housekeeping) {
        return new UpdateHousekeepingPatch(housekeeping, getPathToHousekeeping(housekeeping.getBackupManagerId()));
    }

    private String getPathToHousekeeping(final String backupManagerId) {
        return backupManagerPatchFactory.getPathToBackupManager(backupManagerId) + "/housekeeping/";
    }

    @Autowired
    public void setBackupManagerPatchFactory(final BackupManagerPatchFactory backupManagerPatchFactory) {
        this.backupManagerPatchFactory = backupManagerPatchFactory;
    }
}
