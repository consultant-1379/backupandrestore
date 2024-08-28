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
import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.action.Action.RESTORE;
import static com.ericsson.adp.mgmt.control.AgentMessageType.STAGE_COMPLETE;

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

public class ExecutingBackupStateTest {

    private ExecutingBackupState state;
    private CreateBackupJob job;
    private Action action;
    private JobStage<CreateBackupJob> jobStage;

    @Before
    public void setup() {
        action = createMock(Action.class);
        job = createMock(CreateBackupJob.class);
        jobStage = createMock(JobStage.class);
        state = new ExecutingBackupState(getRegistrationMessage("456"), job);
    }

    @Test
    public void processMessage_stageCompleteBackupMessage_movesToRecognizedStateAndIndicatesBackupFinished() throws Exception {
        final AgentControl stageCompleteMessage = getStageCompleteMessage();
        expect(job.getAction()).andReturn(action).anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(job.getActionId()).andReturn("1111").anyTimes();
        expect(job.getBackupManagerId()).andReturn("222").anyTimes();
        expect(job.getJobStage()).andReturn(jobStage).anyTimes();
        expect(jobStage.getStageName()).andReturn(JobStageName.EXECUTION).anyTimes();
        job.updateProgress("456", stageCompleteMessage.getStageComplete());
        expectLastCall();
        replay(action, jobStage, job);

        final AgentStateChange stateChange = state.processMessage(stageCompleteMessage);
        stateChange.postAction(null, null);

        assertEquals(state, stateChange.getNextState());
        assertEquals("456", stateChange.getNextState().getAgentId());
        verify(action, jobStage, job);
    }

    @Test
    public void processMessage_anyMessageOtherThanStageCompleteBackup_ignoresAndContinuesOnSameState() throws Exception {
        final AgentStateChange stateChange = state
                .processMessage(AgentControl.newBuilder().setAction(RESTORE).setAgentMessageType(STAGE_COMPLETE).build());

        assertEquals(state, stateChange.getNextState());
    }

    @Test
    public void executeBackupPostAction_executingBackupState_movesToPostActionBackupStateAndTriggersBackupPostAction() {
        final AgentInputStream inputStream = createMock(AgentInputStream.class);
        inputStream.executeBackupPostAction();
        expectLastCall();
        replay(inputStream);
        final AgentStateChange stateChange = state.executeBackupPostAction(inputStream);
        assertEquals(PostActionBackupState.class, stateChange.getNextState().getClass());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void prepareForBackup_executingBackupState_throwsException() throws Exception {
        state.prepareForBackup(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void executeBackup_executingBackupState_throwsException() throws Exception {
        state.executeBackup(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void prepareForRestore_executingBackupState_throwsException() throws Exception {
        state.prepareForRestore(null, null);
    }

    @Test
    public void handleClosedConnection_executingBackupState_updatesJob() {
        job.handleAgentDisconnecting(state.getAgentId());
        expectLastCall();
        replay(job);

        state.handleClosedConnection();
        verify(job);
    }

    private AgentControl getStageCompleteMessage() {
        final StageComplete stageComplete = StageComplete.newBuilder().setSuccess(false).setMessage("boo").build();

        return AgentControl.newBuilder().setAction(BACKUP).setAgentMessageType(STAGE_COMPLETE).setStageComplete(stageComplete)
                .build();
    }

    private Register getRegistrationMessage(final String agentId) {
        final SoftwareVersionInfo softwareVersionInfo = SoftwareVersionInfo.newBuilder().setProductName("ProductName")
                .setProductNumber("ProductNumber").setRevision("Revision").setProductionDate("ProductionDate").setDescription("Description")
                .setType("Type").build();

        return Register.newBuilder().setAgentId(agentId).setSoftwareVersionInfo(softwareVersionInfo).setApiVersion("456").setScope("Alpha").build();
    }

}
