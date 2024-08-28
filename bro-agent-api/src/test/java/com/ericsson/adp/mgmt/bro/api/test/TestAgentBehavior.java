/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.bro.api.test;

import com.ericsson.adp.mgmt.bro.api.agent.AgentBehavior;
import com.ericsson.adp.mgmt.bro.api.agent.BackupExecutionActions;
import com.ericsson.adp.mgmt.bro.api.agent.CancelActions;
import com.ericsson.adp.mgmt.bro.api.agent.PostRestoreActions;
import com.ericsson.adp.mgmt.bro.api.agent.RestoreExecutionActions;
import com.ericsson.adp.mgmt.bro.api.agent.RestorePreparationActions;
import com.ericsson.adp.mgmt.bro.api.agent.BackupPreparationActions;
import com.ericsson.adp.mgmt.bro.api.agent.PostBackupActions;
import com.ericsson.adp.mgmt.bro.api.registration.RegistrationInformation;

public class TestAgentBehavior implements AgentBehavior {

    private boolean executedBackup;
    private boolean preparedBackup;
    private boolean performedPostBackup;
    private boolean executedRestore;
    private boolean preparedForRestore;
    private boolean performedPostRestore;
    private boolean cancelledAction;

    @Override
    public RegistrationInformation getRegistrationInformation() {
        return RegistrationInformationUtil.getTestRegistrationInformation();
    }

    @Override
    public void prepareForBackup(BackupPreparationActions backupPreparationActions) {
        preparedBackup = true;
        backupPreparationActions.sendStageComplete(true, "Test Prepare backup.");
    }

    @Override
    public void postBackup(PostBackupActions postBackupActions) {
        performedPostBackup = true;
        postBackupActions.sendStageComplete(true, "Test Post backup.");
    }

    @Override
    public void executeBackup(final BackupExecutionActions backupExecutionActions) {
        this.executedBackup = true;
        backupExecutionActions.backupComplete(true, "Test backup");
    }

    @Override
    public void prepareForRestore(final RestorePreparationActions restorePreparationActions) {
        this.preparedForRestore = true;
        restorePreparationActions.sendStageComplete(true, "stage success");
    }

    @Override
    public void postRestore(final PostRestoreActions postRestoreActions) {
        this.performedPostRestore = true;
        postRestoreActions.sendStageComplete(true, "stage success");
    }

    @Override
    public void executeRestore(final RestoreExecutionActions restoreExecutionActions) {
        this.executedRestore = true;
        restoreExecutionActions.restoreComplete(true, "stage success");
    }

    @Override
    public void cancelAction(final CancelActions cancelActions) {
        this.cancelledAction = true;
        cancelActions.sendStageComplete(true, "stage success");
    }

    public boolean executedBackup() {
        return this.executedBackup;
    }

    public boolean executedRestore() {
        return this.executedRestore;
    }

    public boolean preparedForRestore() {
        return this.preparedForRestore;
    }

    public boolean performedPostRestore() {
        return this.performedPostRestore;
    }

    public boolean cancelledAction() {
        return this.cancelledAction;
    }

    public boolean hasPreparedBackup() {
        return preparedBackup;
    }

    public boolean hasPerformedPostBackup() {
        return performedPostBackup;
    }

    public void reset() {
        this.executedBackup = false;
        this.executedRestore = false;
        this.preparedBackup = false;
        this.preparedForRestore = false;
        this.performedPostBackup = false;
        this.performedPostRestore = false;
        this.cancelledAction = false;
    }

}
