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
package com.ericsson.adp.mgmt.brotestagent.agent.behavior;

import com.ericsson.adp.mgmt.bro.api.agent.RestoreExecutionActions;
import com.ericsson.adp.mgmt.brotestagent.BackupAndRestoreAgent;

/**
 * This class holds custom agent behavior for BRO testing purposes.
 * It is used to simulate agent's connection failure during restore.
 */
public class AgentDiesDuringRestoreTestAgentBehavior extends TestAgentBehavior {

    @Override
    public void executeRestore(final RestoreExecutionActions restoreExecutionActions) {
        BackupAndRestoreAgent.killAgent();
    }
}
