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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static com.ericsson.adp.mgmt.action.Action.BACKUP;
import static com.ericsson.adp.mgmt.control.AgentMessageType.STAGE_COMPLETE;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStage;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStageName;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.Register;
import com.ericsson.adp.mgmt.control.StageComplete;
import com.ericsson.adp.mgmt.metadata.SoftwareVersionInfo;

public class PostActionBackupStateTest {

    private PostActionBackupState state;
    private CreateBackupJob job;
    private Action action;
    private JobStage<CreateBackupJob> jobStage;

    @Before
    public void setUp() {
        action = createMock(Action.class);
        job = createMock(CreateBackupJob.class);
        jobStage = createMock(JobStage.class);
        state = new PostActionBackupState(getRegistrationMessage("abc"), job);
    }

    @Test
    public void processMessage_stageCompletedSuccessfully_staysInSameStateAndUpdatesJob() {
        final AgentControl stageCompleteMessage = getStageCompleteMessage(true);

        expect(job.getAction()).andReturn(action).anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(job.getActionId()).andReturn("1111").anyTimes();
        expect(job.getBackupManagerId()).andReturn("222").anyTimes();
        expect(job.getJobStage()).andReturn(jobStage).anyTimes();
        expect(jobStage.getStageName()).andReturn(JobStageName.POST_ACTIONS).anyTimes();
        job.updateProgress("abc", stageCompleteMessage.getStageComplete());
        expectLastCall();
        replay(action, jobStage, job);

        final AgentStateChange stateChange = state.processMessage(stageCompleteMessage);
        stateChange.postAction(null, null);

        assertEquals(state, stateChange.getNextState());
        verify(action, jobStage, job);
    }

    @Test
    public void processMessage_stageCompletedUnsuccessfully_staysInSameStateAndUpdatesJob() {
        final AgentControl stageCompleteMessage = getStageCompleteMessage(false);

        expect(job.getAction()).andReturn(action).anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(job.getActionId()).andReturn("1111").anyTimes();
        expect(job.getBackupManagerId()).andReturn("222").anyTimes();
        expect(job.getJobStage()).andReturn(jobStage).anyTimes();
        expect(jobStage.getStageName()).andReturn(JobStageName.POST_ACTIONS).anyTimes();
        job.updateProgress("abc", stageCompleteMessage.getStageComplete());
        expectLastCall();
        replay(action, jobStage, job);

        final AgentStateChange stateChange = state.processMessage(stageCompleteMessage);
        stateChange.postAction(null, null);

        assertEquals(state, stateChange.getNextState());
        verify(action, jobStage, job);
    }

    private AgentControl getStageCompleteMessage(final Boolean success) {
        final StageComplete stageComplete = StageComplete.newBuilder().setSuccess(success).setMessage("boo").build();

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
