/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.agent.state;

import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.action.Action.RESTORE;
import static com.ericsson.adp.mgmt.control.AgentMessageType.STAGE_COMPLETE;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStage;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStageName;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.agent.AgentInputStream;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.Register;
import com.ericsson.adp.mgmt.control.StageComplete;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

public class AgentBackupStateTest {
    private AgentBackupState state;
    private CreateBackupJob job;
    private Action action;
    private JobStage<CreateBackupJob> jobStage;

    @Before
    public void setup() {
        action = createMock(Action.class);
        job = createMock(CreateBackupJob.class);
        jobStage = createMock(JobStage.class);
        state = new BackupStateStub(getRegistrationMessage("123"), job);
    }

    @Test
    public void processMessage_stageCompleteMessage_movesToNextStateAndUpdatesJob() throws Exception {
        final AgentControl stageCompleteMessage = getStageCompleteMessage();

        expect(job.getAction()).andReturn(action).anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(job.getActionId()).andReturn("1111").anyTimes();
        expect(job.getBackupManagerId()).andReturn("222").anyTimes();
        expect(job.getJobStage()).andReturn(jobStage).anyTimes();
        expect(jobStage.getStageName()).andReturn(JobStageName.PREPARATION).anyTimes();
        job.updateProgress("123", stageCompleteMessage.getStageComplete());
        expectLastCall();
        replay(action, jobStage, job);

        final AgentStateChange stateChange = state.processMessage(stageCompleteMessage);
        stateChange.postAction(null, null);

        assertEquals(state, stateChange.getNextState());
        verify(action, jobStage, job);
    }

    @Test
    public void processMessage_anyMessageOtherThanStageCompleteBackup_ignoresAndContinuesOnSameState() throws Exception {
        replay(job);

        final AgentStateChange stateChange = state
                .processMessage(AgentControl.newBuilder().setAction(RESTORE).setAgentMessageType(STAGE_COMPLETE).build());

        assertEquals(state, stateChange.getNextState());
        verify(job);
    }

    @Test
    public void handleClosedConnection_agentBackupState_updatesJob() throws Exception {
        job.handleAgentDisconnecting("123");
        expectLastCall();
        replay(job);

        state.handleClosedConnection();

        verify(job);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void prepareForBackup_agentBackupState_throwsException() throws Exception {
        state.prepareForBackup(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void prepareForRestore_agentBackupState_throwsException() throws Exception {
        state.prepareForRestore(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void resetState_agentBackupState_throwsException() throws Exception {
        state.resetState();
    }

    @Test
    public void cancelAction_agentBackupState_movesToCancelingActionStateAndTriggerCancellation() throws Exception {
        final AgentInputStream inputStream = createMock(AgentInputStream.class);
        inputStream.cancelAction(BACKUP);
        expectLastCall();

        replay(inputStream);

        final AgentStateChange stateChange = state.cancelAction(inputStream);
        stateChange.postAction(null, null);
        assertTrue(stateChange.getNextState() instanceof CancelingActionState);
        verify(inputStream);
    }

    private class BackupStateStub extends AgentBackupState {

        public BackupStateStub(final Register registrationInformation, final CreateBackupJob job) {
            super(registrationInformation, job);
        }

    }

    private AgentControl getStageCompleteMessage() {
        final StageComplete stageComplete = StageComplete.newBuilder().setSuccess(false).setMessage("boo").build();

        return AgentControl.newBuilder().setAction(BACKUP).setAgentMessageType(STAGE_COMPLETE)
                .setStageComplete(stageComplete).build();
    }

    private Register getRegistrationMessage(final String agentId) {
        final SoftwareVersionInfo softwareVersionInfo = SoftwareVersionInfo.newBuilder().setProductName("ProductName")
                .setProductNumber("ProductNumber").setRevision("Revision").setProductionDate("ProductionDate").setDescription("Description")
                .setType("Type").build();

        return Register.newBuilder().setAgentId(agentId).setSoftwareVersionInfo(softwareVersionInfo).setApiVersion("456").setScope("Alpha").build();
    }

}
