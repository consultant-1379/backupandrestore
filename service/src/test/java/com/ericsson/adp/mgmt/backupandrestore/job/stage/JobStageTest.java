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
package com.ericsson.adp.mgmt.backupandrestore.job.stage;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.test.MockedAgentFactory;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.job.Job;
import com.ericsson.adp.mgmt.control.StageComplete;

public class JobStageTest {

    private FirstTestJobStage jobStage;
    private SecondTestJobStage secondStage;
    private MockedAgentFactory agentMock;

    @Before
    public void setup() {
        agentMock = new MockedAgentFactory();
        jobStage = new FirstTestJobStage(Arrays.asList(agentMock.mockedAgent(1), agentMock.mockedAgent(2)), mockJob());
        secondStage = new SecondTestJobStage(Arrays.asList(agentMock.mockedAgent(1), agentMock.mockedAgent(2)), null);
    }

    @Test
    public void updateAgentProgress_unfinishedAgents_doesntChangeStage() throws Exception {
        jobStage.updateAgentProgress("1", getMessage(true));
        assertEquals(jobStage, jobStage.changeStages());
    }

    @Test
    public void updateAgentProgress_allAgentsFinished_goesToNextStageAndTriggersIt() throws Exception {
        jobStage.updateAgentProgress("1", getMessage(true));
        jobStage.updateAgentProgress("2", getMessage(false));
        JobStage<Job> nextStage = jobStage.changeStages();

        final SecondTestJobStage otherTestJobStage = (SecondTestJobStage) nextStage;
        otherTestJobStage.trigger();
        assertTrue(otherTestJobStage.isTriggered());
    }

    @Test
    public void getProgressPercentage_firstStageOfTwoAndNoAgentsFinalized_zeroPercent() throws Exception {
        assertEquals(Double.valueOf(0.0d), Double.valueOf(jobStage.getProgressPercentage()));
    }

    @Test
    public void getProgressPercentage_firstStageOfStwoAndAllUnsuccessfulAgents_zeroPercent() throws Exception {
        jobStage.updateAgentProgress("1", getMessage(false));
        jobStage.updateAgentProgress("2", getMessage(false));

        assertEquals(Double.valueOf(0.0d), Double.valueOf(jobStage.getProgressPercentage()));
    }

    @Test
    public void getProgressPercentage_firstStageOfTwoAndSomeSuccessfulAgents_twentyFivePercent() throws Exception {
        jobStage.updateAgentProgress("1", getMessage(true));

        assertEquals(Double.valueOf(0.25d), Double.valueOf(jobStage.getProgressPercentage()));
    }

    @Test
    public void getProgressPercentage_firstStageOfTwoAndAllAgentsSuccessful_fiftyPercent() throws Exception {
        jobStage.updateAgentProgress("1", getMessage(true));
        jobStage.updateAgentProgress("2", getMessage(true));

        assertEquals(Double.valueOf(0.5d), Double.valueOf(jobStage.getProgressPercentage()));
    }

    @Test
    public void getProgressPercentage_firstStageOfTwoAndAllAgentsFinishedButOneUnsuccessfully_onlySuccessfulAgentsAreCountedForProgressPercentage() throws Exception {
        jobStage.updateAgentProgress("1", getMessage(true));
        jobStage.updateAgentProgress("2", getMessage(false));

        assertEquals(Double.valueOf(0.25d), Double.valueOf(jobStage.getProgressPercentage()));
    }

    @Test
    public void getProgressPercentage_secondStageOfTwoAndNoAgentsFinalized_fiftyPercent() throws Exception {
        assertEquals(Double.valueOf(0.5d), Double.valueOf(secondStage.getProgressPercentage()));
    }

    @Test
    public void getProgressPercentage_secondStageOfStwoAndAllUnsuccessfulAgents_fiftyPercent() throws Exception {
        secondStage.updateAgentProgress("1", getMessage(false));
        secondStage.updateAgentProgress("2", getMessage(false));

        assertEquals(Double.valueOf(0.5d), Double.valueOf(secondStage.getProgressPercentage()));
    }

    @Test
    public void getProgressPercentage_secondStageOfTwoAndSomeSuccessfulAgents_seventyFivePercent() throws Exception {
        secondStage.updateAgentProgress("1", getMessage(true));

        assertEquals(Double.valueOf(0.75d), Double.valueOf(secondStage.getProgressPercentage()));
    }

    @Test
    public void getProgressPercentage_secondStageOfTwoAndAllAgentsSuccessful_oneHundredPercent() throws Exception {
        secondStage.updateAgentProgress("1", getMessage(true));
        secondStage.updateAgentProgress("2", getMessage(true));

        assertEquals(Double.valueOf(1.0d), Double.valueOf(secondStage.getProgressPercentage()));
    }

    @Test
    public void getProgressPercentage_secondStageOfTwoAndAllAgentsFinishedButOneUnsuccessfully_onlySuccessfulAgentsAreCountedForProgressPercentage() throws Exception {
        secondStage.updateAgentProgress("1", getMessage(true));
        secondStage.updateAgentProgress("2", getMessage(false));

        assertEquals(Double.valueOf(0.75d), Double.valueOf(secondStage.getProgressPercentage()));
    }

    @Test
    public void handleUnexpectedDataChannel_allAgentOpensUnexpectedDataChannel_movesToNextStageAndTriggersIt() {
        jobStage.handleUnexpectedDataChannel("1");
        JobStage<Job> nextStage = jobStage.changeStages();
        assertTrue(nextStage instanceof FirstTestJobStage);

        jobStage.handleUnexpectedDataChannel("2");
        nextStage = jobStage.changeStages();
        final SecondTestJobStage otherTestJobStage = (SecondTestJobStage) nextStage;
        otherTestJobStage.trigger();

        assertFalse(jobStage.isStageSuccessful());
        assertTrue(otherTestJobStage.isTriggered());
    }

    @Test
    public void handleUnexpectedDataChannel_agentOneOpensUnexpectedDataChannel_doesNotMoveToNextStage() {
        jobStage.handleUnexpectedDataChannel("1");
        final JobStage<Job> nextStage = jobStage.changeStages();
        assertTrue(nextStage instanceof FirstTestJobStage);

        assertFalse(jobStage.isStageSuccessful());
    }

    @Test
    public void handleAgentDisconnecting_allAgentsDisconnected_movesToNextStageAndTriggersIt() {
        jobStage.handleAgentDisconnecting("1");
        JobStage<Job> nextStage = jobStage.changeStages();
        assertTrue(nextStage instanceof FirstTestJobStage);

        jobStage.handleAgentDisconnecting("2");
        nextStage = jobStage.changeStages();
        final SecondTestJobStage otherTestJobStage = (SecondTestJobStage) nextStage;
        otherTestJobStage.trigger();
        assertTrue(otherTestJobStage.isTriggered());
    }

    @Test
    public void fragmentSucceeded_agentFinishedAndFragmentSucceeded_movesToNextStageAndTriggersIt() {
        jobStage.fragmentSucceeded("1", "fragmentA");
        JobStage<Job> nextStage = jobStage.changeStages();
        assertTrue(nextStage instanceof FirstTestJobStage);
    }

    @Test
    public void fragmentFailed_agentFinishedAndFragmentFailed_stageFailedAndMovesToNextStageAndTriggersIt() {
        jobStage.fragmentFailed("1", "fragmentA");
        JobStage<Job> nextStage = jobStage.changeStages();
        assertTrue(nextStage instanceof FirstTestJobStage);
    }

    private Job mockJob() {
        final Job job = createMock(Job.class);
        final Action action = createMock(Action.class);
        expect(action.getName()).andReturn(ActionType.CREATE_BACKUP).anyTimes();
        expect(job.getBackupManagerId()).andReturn("abc").anyTimes();
        expect(action.getActionId()).andReturn("1111").anyTimes();
        expect(job.getAction()).andReturn(action).anyTimes();
        replay(job, action);
        return job;
    }

    private StageComplete getMessage(final boolean success) {
        return StageComplete.newBuilder().setSuccess(success).build();
    }

    private class FirstTestJobStage extends JobStage<Job> {

        public FirstTestJobStage(final List<Agent> agents, final Job job) {
            super(agents, job, null);
        }

        @Override
        protected void handleTrigger() {
            //Does nothing
        }

        @Override
        protected JobStage<Job> getNextStageSuccess() {
            return new SecondTestJobStage(agents, job);
        }

        @Override
        protected JobStage<Job> getNextStageFailure() { return new SecondTestJobStage(agents, job); }

        @Override
        public JobStage<Job> moveToFailedStage() {
            return this;
        }

        @Override
        protected int getNumberOfNonFinalStages() {
            return 2;
        }

        @Override
        protected int getStageOrder() {
            return 1;
        }

        @Override
        public JobStageName getStageName() {
            return JobStageName.EXECUTION;
        }

    }

    private class SecondTestJobStage extends JobStage<Job> {

        private boolean triggered;

        public SecondTestJobStage(final List<Agent> agents, final Job job) {
            super(agents, job, null);
        }

        @Override
        protected void handleTrigger() {
            triggered = true;
        }

        @Override
        protected JobStage<Job> getNextStageSuccess() {
            return this;
        }

        @Override
        protected JobStage<Job> getNextStageFailure() {
            return this;
        }

        @Override
        public JobStage<Job> moveToFailedStage() {
            return this;
        }

        public boolean isTriggered() {
            return triggered;
        }

        @Override
        protected int getNumberOfNonFinalStages() {
            return 2;
        }

        @Override
        protected int getStageOrder() {
            return 2;
        }

        @Override
        public JobStageName getStageName() {
            return JobStageName.COMPLETE;
        }

    }

}
