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

/**
 * Describes actions to take after a backup is completed.
 */
public class PostBackupActions extends BackupActions {
    /**
     * Constructor
     *
     * @param agent - the {@link Agent} that uses these actions
     * @param actionInformation - holds the {@link ActionInformation}
     */
    public PostBackupActions(final Agent agent, final ActionInformation actionInformation) {
        super(agent, actionInformation);
    }
}
