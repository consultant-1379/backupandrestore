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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.adp.mgmt.bro.api.agent.BackupExecutionActions;
import com.ericsson.adp.mgmt.brotestagent.BackupAndRestoreAgent;


@RunWith(PowerMockRunner.class)
@PrepareForTest(BackupAndRestoreAgent.class)
@PowerMockIgnore("javax.management.*")
public class AgentDiesDuringBackupTestAgentBehaviorTest {

    @Before
    public void setup() {
        PowerMockito.mockStatic(BackupAndRestoreAgent.class);
    }

    @Test
    public void executeBackup_killAgent() {
        final AgentDiesDuringBackupTestAgentBehavior agentBehavior = new AgentDiesDuringBackupTestAgentBehavior();
        final BackupExecutionActionsStub backupExecutionActions = new BackupExecutionActionsStub();
        agentBehavior.executeBackup(backupExecutionActions);

        PowerMockito.verifyStatic(BackupAndRestoreAgent.class, VerificationModeFactory.times(1));
        BackupAndRestoreAgent.killAgent();
    }

    private class BackupExecutionActionsStub extends BackupExecutionActions {

        public BackupExecutionActionsStub() {
            super(null, null);
        }

    }

}
