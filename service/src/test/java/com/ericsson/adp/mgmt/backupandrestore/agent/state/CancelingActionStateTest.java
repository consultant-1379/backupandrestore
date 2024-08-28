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
package com.ericsson.adp.mgmt.backupandrestore.agent.state;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.action.Action.RESTORE;
import static com.ericsson.adp.mgmt.control.AgentMessageType.STAGE_COMPLETE;
import static com.ericsson.adp.mgmt.control.AgentMessageType.PREPARE_DEPENDENCY;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.Register;
import com.ericsson.adp.mgmt.control.StageComplete;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

public class CancelingActionStateTest {

    private CancelingActionState state;
    private RestoreJob restoreJob;
    private CreateBackupJob backupJob;

    @Before
    public void setup() {
        restoreJob = createMock(RestoreJob.class);
        backupJob = createMock(CreateBackupJob.class);
    }

    @Test
    public void processMessage_restoreStageCompleteMessage_movesToNextRecognizedStateAndUpdatesRestoreJob() throws Exception {
        state = new CancelingActionState(getRegistrationMessage("123"), restoreJob);
        final AgentControl stageCompleteMessage = getStageCompleteMessage(RESTORE);
        restoreJob.updateProgress("123", stageCompleteMessage.getStageComplete());
        expectLastCall();
        replay(restoreJob);

        final AgentStateChange stateChange = state.processMessage(stageCompleteMessage);
        stateChange.postAction(null, null);

        assertTrue(stateChange.getNextState() instanceof RecognizedState);
        verify(restoreJob);
    }

    @Test
    public void processMessage_backupStageCompleteMessage_movesToNextRecognizedStateAndUpdatesBackupJob() throws Exception {
        state = new CancelingActionState(getRegistrationMessage("123"), backupJob);
        final AgentControl stageCompleteMessage = getStageCompleteMessage(BACKUP);
        backupJob.updateProgress("123", stageCompleteMessage.getStageComplete());
        expectLastCall();
        replay(backupJob);

        final AgentStateChange stateChange = state.processMessage(stageCompleteMessage);
        stateChange.postAction(null, null);

        assertTrue(stateChange.getNextState() instanceof RecognizedState);
        verify(backupJob);
    }

    @Test
    public void processMessage_anyMessageOtherThanStageComplete_ignoresAndContinuesOnSameState() throws Exception {
        state = new CancelingActionState(getRegistrationMessage("123"), restoreJob);
        replay(restoreJob);

        final AgentStateChange stateChange = state
                .processMessage(AgentControl.newBuilder().setAction(BACKUP).setAgentMessageType(PREPARE_DEPENDENCY).build());

        assertEquals(state, stateChange.getNextState());
        verify(restoreJob);
    }

    @Test
    public void handleClosedConnection_stateWithRestoreJob_updatesRestoreJob() throws Exception {
        state = new CancelingActionState(getRegistrationMessage("123"), restoreJob);
        restoreJob.handleAgentDisconnecting("123");
        expectLastCall();
        replay(restoreJob);

        state.handleClosedConnection();

        verify(restoreJob);
    }

    @Test
    public void handleClosedConnection_stateWithBackupJob_updatesBackupJob() throws Exception {
        state = new CancelingActionState(getRegistrationMessage("123"), backupJob);
        backupJob.handleAgentDisconnecting("123");
        expectLastCall();
        replay(backupJob);

        state.handleClosedConnection();

        verify(backupJob);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void prepareForBackup_executingBackupState_throwsException() throws Exception {
        state = new CancelingActionState(getRegistrationMessage("123"), restoreJob);
        state.prepareForBackup(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void prepareForRestore_executingBackupState_throwsException() throws Exception {
        state = new CancelingActionState(getRegistrationMessage("123"), restoreJob);
        state.prepareForRestore(null, null);
    }

    private AgentControl getStageCompleteMessage(final Action action) {
        final StageComplete stageComplete = StageComplete.newBuilder().setSuccess(false).setMessage("boo").build();

        return AgentControl.newBuilder().setAction(action).setAgentMessageType(STAGE_COMPLETE)
                .setStageComplete(stageComplete).build();
    }

    private Register getRegistrationMessage(final String agentId) {
        final SoftwareVersionInfo softwareVersionInfo = SoftwareVersionInfo.newBuilder().setProductName("ProductName")
                .setProductNumber("ProductNumber").setRevision("Revision").setProductionDate("ProductionDate").setDescription("Description")
                .setType("Type").build();

        return Register.newBuilder().setAgentId(agentId).setSoftwareVersionInfo(softwareVersionInfo).setApiVersion("456").setScope("Alpha").build();
    }

}
