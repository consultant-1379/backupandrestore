/*
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 * ****************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.bro.api.agent;

import static com.ericsson.adp.mgmt.action.Action.BACKUP;

/**
 * Abstract class used to describe behavior of Backup Actions
 */
public abstract class BackupActions {
    protected final ActionInformation actionInformation;
    protected final Agent agent;
    /**
     * Constructor
     *
     * @param agent - the {@link Agent} that uses these actions
     * @param actionInformation - holds the {@link ActionInformation}
     */
    public BackupActions(final Agent agent, final ActionInformation actionInformation) {
        this.agent = agent;
        this.actionInformation = actionInformation;
    }

    /**
     * @return - Provides the name that was given with a backup request.
     */
    public String getBackupName() {
        return actionInformation.getBackupName();
    }

    /**
     * @return - Provides the backupType that was given with a backup request.
     */
    public String getBackupType() {
        return actionInformation.getBackupType();
    }

    /**
     * Notify that stage is complete.
     * @param success - true if stage completed successfully else false.
     * @param message - a message to log.
     */
    public void sendStageComplete(final boolean success, final String message) {
        this.agent.sendStageCompleteMessage(success, message, BACKUP);
    }
}
