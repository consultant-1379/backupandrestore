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
package com.ericsson.adp.mgmt.bro.api.agent;

import com.ericsson.adp.mgmt.bro.api.registration.RegistrationInformation;

/**
 * Represents specific agent behavior.
 */
public interface AgentBehavior {

    /**
     * Gets agent's registration information.
     *
     * @return registration information.
     */
    RegistrationInformation getRegistrationInformation();

    /**
     * Used to call an agents custom code for a backup from the orchestrator, Backup data should be created by this call.
     *
     * @param backupExecutionActions
     *            provides methods to allow an agent to transfer backups and indicate that a backup has completed.
     *
     */
    void executeBackup(BackupExecutionActions backupExecutionActions);

    /**
     * Default behavior implementation for {@link BackupPreparationActions}.
     * Can be used to do some preparation before a backup. No backup data should be created by this call.
     * @param backupPreparationActions - An instance or instance of a descendant of {@link BackupPreparationActions}.
     */
    default void prepareForBackup(final BackupPreparationActions backupPreparationActions) {
        backupPreparationActions.sendStageComplete(true, "Preparation for backup is successful");
    }

    /**
     * Default behavior implementation for {@link PostBackupActions}.
     * Can be used to do some actions after a backup. No backup data should be created by this call.
     * @param postBackupActions - An instance or instance of a descendant of {@link PostBackupActions}.
     */
    default void postBackup(final PostBackupActions postBackupActions) {
        postBackupActions.sendStageComplete(true, "Post backup actions completed");
    }

    /**
     * Used to call an agents custom code for a restore from the orchestrator
     *
     * @param restoreExecutionActions
     *            provides methods to download fragments to be restored by an agent and indicate that a restore has completed.
     *
     */
    void executeRestore(RestoreExecutionActions restoreExecutionActions);

    /**
     * Used to do some preparation before restore is called
     *
     * @param restorePreparationActions
     *            provides method to indicate that preparation for restore has completed.
     *
     */
    default void prepareForRestore(final RestorePreparationActions restorePreparationActions) {
        restorePreparationActions.sendStageComplete(true, "Preparation for restore is successful");
    }

    /**
     * Used to do perform some actions post restore
     *
     * @param postRestoreActions
     *            provides method to indicate that post restore actions has completed.
     *
     */
    default void postRestore(final PostRestoreActions postRestoreActions) {
        postRestoreActions.sendStageComplete(true, "Post restore actions completed");
    }

    /**
     * Used to do perform some actions on cancel
     *
     * @param cancelActions
     *            provides method to indicate that cancel actions has completed.
     *
     */
    default void cancelAction(final CancelActions cancelActions) {
        cancelActions.sendStageComplete(true, "Cancel actions completed");
    }
}
