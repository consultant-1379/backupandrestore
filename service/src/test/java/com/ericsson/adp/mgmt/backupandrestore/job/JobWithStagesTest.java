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
package com.ericsson.adp.mgmt.backupandrestore.job;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.exception.JobFailedException;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStage;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStageName;
import com.ericsson.adp.mgmt.control.StageComplete;
import com.ericsson.adp.mgmt.data.Metadata;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class JobWithStagesTest {

    private TestJob job;
    private Action action;
    private TestJobStage stage;
    private BackupManager backupManager;
    private CMMediatorService cmMediatorService;

    @Before
    public void setup() {
        job = new TestJob();
        action = createMock(Action.class);
        cmMediatorService = createMock(CMMediatorService.class);
        job.setCmMediatorService(cmMediatorService);
        expect(action.getActionId()).andReturn("1").anyTimes();
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();

        stage = createMock(TestJobStage.class);
        stage.trigger();
        expectLastCall().anyTimes();
        ReflectionTestUtils.setField(stage, "wasTriggerCalled", new AtomicBoolean(true));

        backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn("DEFAULT").anyTimes();

        job.setAction(action);
        job.setJobStage(stage);
        job.setBackupManager(backupManager);
    }

    @Test
    public void didFinish_stageIndicatesJobIsDone_didFinish() throws Exception {
        expect(stage.isJobFinished()).andReturn(true);
        replay(stage);

        assertTrue(job.didFinish());
        verify(stage);
    }

    @Test
    public void fail_jobFailed_updatesProgressPercentageAndMoveToFailedStage() throws Exception {
        expect(stage.moveToFailedStage()).andReturn(stage);
        expect(stage.getProgressPercentage()).andReturn(0.98d);

        action.setProgressPercentage(0.98d);
        expectLastCall();
        action.persist();
        expectLastCall();

        replay(stage, action);

        job.fail();

        verify(stage);
        verify(action);
    }

    @Test(expected = JobFailedException.class)
    public void completeJob_stageFailed_throwsException() throws Exception {
        expect(stage.isStageSuccessful()).andReturn(false);
        replay(stage);

        job.completeJob();
    }

    @Test
    public void completeJob_stageSuccessful_doesNothing() throws Exception {
        expect(stage.isStageSuccessful()).andReturn(true);
        replay(stage);

        job.completeJob();

        verify(stage);
    }

    @Test
    public void handleAgentDisconnecting_agentId_updatesJobStageAndProgressPercentage() {
        expect(stage.getStageName()).andReturn(JobStageName.PREPARATION).anyTimes();
        expect(stage.getProgressPercentage()).andReturn(0.54d);
        expect(stage.changeStages()).andReturn(stage);
        stage.handleAgentDisconnecting("1");
        expectLastCall();


        action.setProgressPercentage(0.54d);
        expectLastCall();
        action.persist();
        expectLastCall().anyTimes();

        replay(stage, action, backupManager);

        job.handleAgentDisconnecting("1");

        verify(stage);
        verify(action);
    }

    @Test
    public void updateProgress_agentIdAndStageCompleteMessage_updatesJobStageAndProgressPercentage() {
        final StageComplete stageComplete = getStageCompleteMessage(false);

        expect(stage.getStageName()).andReturn(JobStageName.EXECUTION).anyTimes();
        expect(stage.getProgressPercentage()).andReturn(0.54d);
        expect(stage.changeStages()).andReturn(stage);
        stage.updateAgentProgress("1", stageComplete);
        expectLastCall();

        action.setProgressPercentage(0.54d);
        expectLastCall();
        action.addMessage(anyString());
        expectLastCall();
        action.persist();
        expectLastCall();
        expect(action.getProgressInfo()).andReturn(null).anyTimes();
        action.setProgressInfo(anyString());
        expectLastCall().anyTimes();
        replay(stage, action, backupManager);

        job.updateProgress("1", stageComplete);

        verify(stage);
        verify(action);
    }

    @Test
    public void handleUnexpectedDataChannel_agentId_updatesJobStageAndProgressPercentage() {
        expect(stage.getProgressPercentage()).andReturn(0.54d);
        expect(stage.changeStages()).andReturn(stage);
        stage.handleUnexpectedDataChannel("1");
        expectLastCall();

        action.setProgressPercentage(0.54d);
        expectLastCall();
        action.persist();
        expectLastCall();

        replay(stage, action);

        job.handleUnexpectedDataChannel("1");

        verify(stage);
        verify(action);
    }

    private StageComplete getStageCompleteMessage(final boolean success) {
        return StageComplete.newBuilder().setSuccess(success).setMessage("boo").build();
    }

    private class TestJob extends JobWithStages<TestJob> {

        @Override
        public FragmentFolder getFragmentFolder(final Metadata metadata) {
            return new FragmentFolder(Paths.get("/"));
        }

        @Override
        protected void triggerJob() {
            //Not needed
        }

    }

    private abstract class TestJobStage extends JobStage<TestJob> {

        public TestJobStage(final List<Agent> agents, final TestJob job) {
            super(agents, job, null);
        }

    }



}
