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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStage;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStageName;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentInputStream;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.AgentMessageType;
import com.ericsson.adp.mgmt.control.Register;
import com.ericsson.adp.mgmt.control.StageComplete;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

public class RestoreStateTest {

    private RestoreState state;
    private RestoreJob restoreJob;
    private com.ericsson.adp.mgmt.backupandrestore.action.Action action;
    private JobStage<RestoreJob> jobStage;

    @Before
    public void setup() {
        action = createMock(com.ericsson.adp.mgmt.backupandrestore.action.Action.class);
        restoreJob = createMock(RestoreJob.class);
        jobStage = createMock(JobStage.class);
        state = new RestoreStateStub(getRegistrationMessage("123"), restoreJob);
    }

    @Test
    public void processMessage_stageCompleteRestoreMessage_movesToNextStateAndUpdatesJob() throws Exception {
        final AgentControl stageCompleteMessage = getStageCompleteMessage();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(restoreJob.getAction()).andReturn(action).anyTimes();
        expect(restoreJob.getActionId()).andReturn("1111").anyTimes();
        expect(restoreJob.getBackupManagerId()).andReturn("222").anyTimes();
        expect(restoreJob.getJobStage()).andReturn(jobStage).anyTimes();
        expect(jobStage.getStageName()).andReturn(JobStageName.PREPARATION).anyTimes();
        restoreJob.updateProgress("123", stageCompleteMessage.getStageComplete());
        expectLastCall();
        replay(action, jobStage, restoreJob);

        final AgentStateChange stateChange = state.processMessage(stageCompleteMessage);
        stateChange.postAction(null, null);

        assertEquals(state, stateChange.getNextState());
        verify(action, jobStage, restoreJob);
    }

    @Test
    public void processMessage_anyMessageOtherThanStageCompleteRestore_ignoresAndContinuesOnSameState() throws Exception {
        replay(restoreJob);

        final AgentStateChange stateChange = state
                .processMessage(AgentControl.newBuilder().setAction(Action.BACKUP).setAgentMessageType(AgentMessageType.STAGE_COMPLETE).build());

        assertEquals(state, stateChange.getNextState());
        verify(restoreJob);
    }

    @Test
    public void handleClosedConnection_restoreState_updatesJob() throws Exception {
        restoreJob.handleAgentDisconnecting("123");
        expectLastCall();
        replay(restoreJob);

        state.handleClosedConnection();

        verify(restoreJob);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void prepareForBackup_restoreState_throwsException() throws Exception {
        state.prepareForBackup(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void prepareForRestore_restoreState_throwsException() throws Exception {
        state.prepareForRestore(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void resetState_restoreState_throwsException() throws Exception {
        state.resetState();
    }

    @Test
    public void cancelAction_restoreState_movesToCancelingActionStateAndTriggerCancellation() throws Exception {
        final AgentInputStream inputStream = createMock(AgentInputStream.class);
        inputStream.cancelAction(Action.RESTORE);
        expectLastCall();

        replay(inputStream);

        final AgentStateChange stateChange = state.cancelAction(inputStream);
        stateChange.postAction(null, null);
        assertTrue(stateChange.getNextState() instanceof CancelingActionState);
        verify(inputStream);
    }

    private class RestoreStateStub extends RestoreState {

        public RestoreStateStub(final Register registrationInformation, final RestoreJob job) {
            super(registrationInformation, job);
        }

    }

    private AgentControl getStageCompleteMessage() {
        final StageComplete stageComplete = StageComplete.newBuilder().setSuccess(false).setMessage("boo").build();

        return AgentControl.newBuilder().setAction(Action.RESTORE).setAgentMessageType(AgentMessageType.STAGE_COMPLETE)
                .setStageComplete(stageComplete).build();
    }

    private Register getRegistrationMessage(final String agentId) {
        final SoftwareVersionInfo softwareVersionInfo = SoftwareVersionInfo.newBuilder().setProductName("ProductName")
                .setProductNumber("ProductNumber").setRevision("Revision").setProductionDate("ProductionDate").setDescription("Description")
                .setType("Type").build();

        return Register.newBuilder().setAgentId(agentId).setSoftwareVersionInfo(softwareVersionInfo).setApiVersion("456").setScope("Alpha").build();
    }

}
