/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.job;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;

/**
 * Simple immutable class to store all the information about a queued job.
 * */
class JobQueueItem {
    private final Action action;
    private final BackupManager manager;

    /**
     * Creates an instance of a JobQueueItem
     * @param manager the backup manager
     * @param action the action
     */
    JobQueueItem(final BackupManager manager, final Action action) {
        this.manager = manager;
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public BackupManager getManager() {
        return manager;
    }
}