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

import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStage;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStageName;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.AgentMessageType;
import com.ericsson.adp.mgmt.control.Register;
import com.ericsson.adp.mgmt.control.StageComplete;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

public class PostActionRestoreStateTest {

    private RestoreJob restoreJob;
    private PostActionRestoreState state;
    private com.ericsson.adp.mgmt.backupandrestore.action.Action action;
    private JobStage<RestoreJob> jobStage;

    @Before
    public void setup() {
        action = createMock(com.ericsson.adp.mgmt.backupandrestore.action.Action.class);
        restoreJob = createMock(RestoreJob.class);
        jobStage = createMock(JobStage.class);
        state = new PostActionRestoreState(getRegistrationMessage("A"), restoreJob);
    }

    @Test
    public void processMessage_stageCompletedSuccessfully_staysInSameStateAndUpdatesJob() throws Exception {
        final AgentControl stageCompleteMessage = getStageCompleteMessage(Action.RESTORE, true);

        expect(restoreJob.getAction()).andReturn(action).anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(restoreJob.getActionId()).andReturn("1111").anyTimes();
        expect(restoreJob.getBackupManagerId()).andReturn("222").anyTimes();
        expect(restoreJob.getJobStage()).andReturn(jobStage).anyTimes();
        expect(jobStage.getStageName()).andReturn(JobStageName.POST_ACTIONS).anyTimes();
        restoreJob.updateProgress("A", stageCompleteMessage.getStageComplete());
        expectLastCall();

        replay(action, jobStage, restoreJob);

        final AgentStateChange stateChange = state.processMessage(stageCompleteMessage);
        stateChange.postAction(null, null);

        assertEquals(state, stateChange.getNextState());
        verify(action, jobStage, restoreJob);
    }

    @Test
    public void processMessage_stageCompletedunsuccessfully_staysInTheSameStateAndUpdatesJob() throws Exception {
        final AgentControl stageCompleteMessage = getStageCompleteMessage(Action.RESTORE, false);

        expect(restoreJob.getAction()).andReturn(action).anyTimes();
        expect(action.getName()).andReturn(ActionType.RESTORE).anyTimes();
        expect(restoreJob.getActionId()).andReturn("1111").anyTimes();
        expect(restoreJob.getBackupManagerId()).andReturn("222").anyTimes();
        expect(restoreJob.getJobStage()).andReturn(jobStage).anyTimes();
        expect(jobStage.getStageName()).andReturn(JobStageName.POST_ACTIONS).anyTimes();
        restoreJob.updateProgress("A", stageCompleteMessage.getStageComplete());
        expectLastCall();

        replay(action, jobStage, restoreJob);

        final AgentStateChange stateChange = state.processMessage(stageCompleteMessage);
        stateChange.postAction(null, null);

        assertEquals(state, stateChange.getNextState());
        verify(action, jobStage, restoreJob);
    }

    private AgentControl getStageCompleteMessage(final Action action, final boolean success) {
        final StageComplete stageComplete = StageComplete.newBuilder().setSuccess(success).setMessage("boo").build();

        return AgentControl.newBuilder().setAction(action).setAgentMessageType(AgentMessageType.STAGE_COMPLETE)
                .setStageComplete(stageComplete).build();
    }

    private Register getRegistrationMessage(final String agentId) {
        final SoftwareVersionInfo softwareVersionInfo = SoftwareVersionInfo.newBuilder().setProductName("ProductName")
                .setProductNumber("ProductNumber").setRevision("Revision").setProductionDate("ProductionDate").setDescription("Description")
                .setType("Type").build();

        return Register.newBuilder().setAgentId(agentId).setSoftwareVersionInfo(softwareVersionInfo).setApiVersion("456").setScope("Alpha").build();
    }

}
