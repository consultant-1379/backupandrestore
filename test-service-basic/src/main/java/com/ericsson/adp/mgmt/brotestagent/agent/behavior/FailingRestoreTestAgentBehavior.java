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
package com.ericsson.adp.mgmt.brotestagent.agent.behavior;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.bro.api.agent.RestoreExecutionActions;

/**
 * This class holds custom agent behavior for BRO testing purposes.
 * It is used to simulate a failing restore action from the agent.
 */
public class FailingRestoreTestAgentBehavior extends TestAgentBehavior {

    private static final Logger log = LogManager.getLogger(FailingRestoreTestAgentBehavior.class);

    @Override
    public void executeRestore(final RestoreExecutionActions restoreExecutionActions) {

        String message;

        log.error("Restore of backup <{}> intentionally failed", restoreExecutionActions.getBackupName());
        message = "Service failed to restore backup " + restoreExecutionActions.getBackupName();

        restoreExecutionActions.sendStageComplete(false, message);
    }

}
