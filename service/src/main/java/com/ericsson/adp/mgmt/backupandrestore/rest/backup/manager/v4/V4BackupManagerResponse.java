/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.v4;

import java.util.ArrayList;
import java.util.Collection;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.BackupManagerResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext.TaskHypertextControl;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * JSON response representing one backupManager.
 *
 * Include.NON_NULL as non-vBRMs will have a null agent list and parent id
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class V4BackupManagerResponse extends BackupManagerResponse {

    private Collection<TaskHypertextControl> ongoingTasks = new ArrayList<>();
    private Collection<TaskHypertextControl> availableTasks = new ArrayList<>();

    /**
     * Default constructor, to be used by Jackson.
     */
    public V4BackupManagerResponse() {
    }

    /**
     * Creates BackupManagerResponse from a BackupManager.
     * @param backupManager the backup manager
     * @param ongoingTasks the list of operations running in a backup manager
     * @param availableTasks the list of operations available to run in a backup manager
     */
    public V4BackupManagerResponse(final BackupManager backupManager,
                                   final Collection<TaskHypertextControl> ongoingTasks,
                                   final Collection<TaskHypertextControl> availableTasks) {
        super(backupManager);
        this.ongoingTasks = ongoingTasks;
        this.availableTasks = availableTasks;
    }

    public Collection<TaskHypertextControl> getOngoingTasks() {
        return ongoingTasks;
    }

    public Collection<TaskHypertextControl> getAvailableTasks() {
        return availableTasks;
    }

    public void setOngoingTasks(final Collection<TaskHypertextControl> ongoingTasks) {
        this.ongoingTasks = ongoingTasks;
    }

    public void setAvailableTasks(final Collection<TaskHypertextControl> availableTasks) {
        this.availableTasks = availableTasks;
    }
}
