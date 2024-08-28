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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.ericsson.adp.mgmt.brotestagent.test.RestoreExecutionActionsStub;

public class FailingRestoreTestAgentBehaviorTest {

    @Test
    public void executeRestore_noMatterWhat_failsRestore() throws Exception {
        final String backupName = "myTestBackup";
        final String expectedMessage = "Service failed to restore backup " + backupName;
        final FailingRestoreTestAgentBehavior agentBehavior = new FailingRestoreTestAgentBehavior();

        final RestoreExecutionActionsStub restoreExecutionActions = new RestoreExecutionActionsStub(backupName, "DEFAULT");

        agentBehavior.executeRestore(restoreExecutionActions);

        assertFalse(restoreExecutionActions.isSuccessful());
        assertEquals(expectedMessage, restoreExecutionActions.getMessage());
    }

}
