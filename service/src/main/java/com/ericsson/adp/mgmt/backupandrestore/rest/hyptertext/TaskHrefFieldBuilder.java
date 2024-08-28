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
package com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext;

import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.DELETE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.EXPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.RESTORE;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;

/**
 * A class responsible for building href representation
 * of an available and on-going task of a backup manager
 *
 */
public class TaskHrefFieldBuilder {

    private static final Set<ActionType> ACTIONS_NEEDING_EXISTING_BACKUP = Set.of(RESTORE, EXPORT, DELETE_BACKUP);

    /**
     * Build the href field for an available task of a backup manager
     * @param brmURI the base uri for a specific backup manager
     * @param type the type of action
     * @param backupName the optional backup name
     * @return the href String representation of the available task
     */
    public Path buildAvailableTaskHrefField(final String brmURI, final ActionType type, final Optional<String> backupName) {
        final boolean taskNeedsExistingBackup = ACTIONS_NEEDING_EXISTING_BACKUP.contains(type);
        if (taskNeedsExistingBackup && !backupName.isPresent()) {
            throw new IllegalArgumentException("No backup name is specified for the action: " + type + " when building the task href.");
        }

        Path href = Path.of(brmURI);
        if (taskNeedsExistingBackup) {
            href = Path.of(brmURI, "backups", backupName.get());
        }

        switch (type) {
            case CREATE_BACKUP:
                return href.resolve("backups");
            case EXPORT:
            case RESTORE:
            case IMPORT:
                return href.resolve(type.name().toLowerCase() + "s");
            default:
                return href;
        }
    }

    /**
     * Build the href field for an ongoing task of a backup manager
     * @param brmURI the base uri for a specific backup manager
     * @param action the running action of a backup manager
     * @return the href String representation of the on-going task
     */
    public Path buildOngoingTaskHrefField(final String brmURI, final Action action) {
        final ActionType type = action.getName();
        final Optional<String> backupName = !action.isPartOfHousekeeping() ? Optional.of(action.getBackupName()) : Optional.empty();
        String bckName = "";
        if (backupName.isPresent()) {
            bckName = backupName.get();
        }

        final Path href = buildAvailableTaskHrefField(brmURI, type, backupName);
        switch (type) {
            case CREATE_BACKUP:
                return href.resolve(bckName);
            case EXPORT:
            case RESTORE:
            case IMPORT:
                return href.resolve(action.getActionId());
            default:
                return href;
        }
    }

}
