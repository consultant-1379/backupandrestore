/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.backupandrestore.job;

import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.agent.state.AgentState;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStage;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStageName;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;
import com.ericsson.adp.mgmt.data.Metadata;

public class JobMonitorTest {

    @Test
    public void monitor_JobMonitorCallsMonitor_monitorShouldBeCalledThreeTimes() {
        final TestJob job = new TestJob();
        final JobMonitor monitor = new JobMonitor(job, 1);
        Awaitility.dontCatchUncaughtExceptions().await().atLeast(2500, TimeUnit.MILLISECONDS).until(job::callCountThree);
        monitor.stop();
    }

    @Test
    public void monitor_JobWithStagesMonitor_monitorShouldBeCalledThreeTimes() {
        final TestJobWithStages job = new TestJobWithStages();
        final List<Agent> agents = Arrays.asList(new FakeAgent("1"), new FakeAgent("2"), new FakeAgent("3"));
        job.setAgents(agents);
        final TestJobStage testJobStage = new TestJobStage(agents,job, null);
        job.setJobStage(testJobStage);
        assertEquals(3,testJobStage.idsOfAgentsInProgress().size());
        final JobMonitor monitor = new JobMonitor(job, 1);
        Awaitility.dontCatchUncaughtExceptions().await().atLeast(2500, TimeUnit.MILLISECONDS).until(job::callCountThree);
        monitor.stop();
    }

    private class TestJob extends Job {

        private int callCount = 0;

        public boolean callCountThree() {
            return callCount == 3;
        }

        @Override
        protected void triggerJob() {

        }

        @Override
        protected boolean didFinish() {
            return false;
        }

        @Override
        protected void completeJob() {

        }

        @Override
        protected void fail() {

        }

        @Override
        protected void monitor() {
            callCount++;
            super.monitor();
        }
    }

    private class TestJobWithStages extends JobWithStages<TestJobWithStages> {

        private int callCount = 0;

        public boolean callCountThree() {
            return callCount == 3;
        }

        @Override
        public FragmentFolder getFragmentFolder(final Metadata metadata) {
            return new FragmentFolder(Paths.get("/"));
        }

        @Override
        protected void triggerJob() {
            //Not needed
        }

        @Override
        protected void monitor() {
            callCount++;
            super.monitor();
        }
    }

    private class TestJobStage extends JobStage<TestJobWithStages> {

        public TestJobStage(final List<Agent> agents, final TestJobWithStages job, final NotificationService notificationService) {
            super(agents, job, notificationService);
        }

        @Override
        protected void handleTrigger() {

        }

        @Override
        public JobStage<TestJobWithStages> moveToFailedStage() {
            return null;
        }

        @Override
        protected JobStage<TestJobWithStages> getNextStageSuccess() {
            return null;
        }

        @Override
        protected JobStage<TestJobWithStages> getNextStageFailure() {
            return null;
        }

        @Override
        protected int getStageOrder() {
            return 0;
        }

        @Override
        protected int getNumberOfNonFinalStages() {
            return 0;
        }

        @Override
        public JobStageName getStageName() {
            return JobStageName.EXECUTION;
        }

    }

    private class FakeAgent extends Agent {

        private final String fakeId;

        public FakeAgent(final String fakeId) {
            super(null, null, new IdValidator());
            this.fakeId = fakeId;
        }

        @Override
        public String getAgentId() {
            return fakeId;

        }

        @Override
        public AgentState getState() {
            return null;
        }
    }
}
