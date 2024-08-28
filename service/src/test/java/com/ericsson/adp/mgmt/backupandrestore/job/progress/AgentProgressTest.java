package com.ericsson.adp.mgmt.backupandrestore.job.progress;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class AgentProgressTest {

    private AgentProgress agentProgress;

    @Before
    public void setup() {
        this.agentProgress = new AgentProgress();
    }

    @Test
    public void didFinish_stillWaitingResult_returnFalse() {
        assertFalse(this.agentProgress.didFinish());
    }

    @Test
    public void didFinish_receivedResult_returnTrue() {
        this.agentProgress.setProgress(Progress.SUCCESSFUL);

        assertTrue(this.agentProgress.didFinish());
    }

    @Test
    public void didFinish_receivedOneFragment_returnFalse() {
        this.agentProgress.handleNewFragment("FragmentA");

        assertFalse(this.agentProgress.didFinish());
    }

    @Test
    public void didFinish_receivedOneFragmentAndReceivedResultOfFragment_returnFalse() {
        this.agentProgress.handleNewFragment("FragmentA");
        this.agentProgress.setFragmentProgress("FragmentA", Progress.SUCCESSFUL);

        assertFalse(this.agentProgress.didFinish());
    }

    @Test
    public void didFinish_agentFinishedAndFragmentFinished_returnTrue() {
        this.agentProgress.handleNewFragment("FragmentA");
        this.agentProgress.setFragmentProgress("FragmentA", Progress.SUCCESSFUL);

        this.agentProgress.setProgress(Progress.SUCCESSFUL);

        assertTrue(this.agentProgress.didFinish());
    }

    @Test
    public void didFinish_agentFinishedAndOneOfTwoFragmentsDidNot_returnFalse() {
        this.agentProgress.handleNewFragment("FragmentA");
        this.agentProgress.setFragmentProgress("FragmentA", Progress.SUCCESSFUL);

        this.agentProgress.handleNewFragment("FragmentB");

        this.agentProgress.setProgress(Progress.SUCCESSFUL);

        assertFalse(this.agentProgress.didFinish());
    }

    @Test
    public void didSucceed_fragmentSuccessfulAndAgentSuccessful_returnTrue() {
        this.agentProgress.handleNewFragment("FragmentA");
        this.agentProgress.setFragmentProgress("FragmentA", Progress.SUCCESSFUL);

        this.agentProgress.setProgress(Progress.SUCCESSFUL);

        assertTrue(this.agentProgress.didSucceed());
    }

    @Test
    public void didSucceed_fragmentFailedAndAgentSuccessful_returnFalse() {
        this.agentProgress.handleNewFragment("FragmentA");
        this.agentProgress.setFragmentProgress("FragmentA", Progress.FAILED);

        this.agentProgress.setProgress(Progress.SUCCESSFUL);

        assertFalse(this.agentProgress.didSucceed());
    }

    @Test
    public void didSucceed_fragmentSuccessfulAndAgentFailed_returnFalse() {
        this.agentProgress.handleNewFragment("FragmentA");
        this.agentProgress.setFragmentProgress("FragmentA", Progress.SUCCESSFUL);

        this.agentProgress.setProgress(Progress.FAILED);

        assertFalse(this.agentProgress.didSucceed());
    }

    @Test
    public void failWaitingFragments_failsFragmentsThatAreWaiting() {
        this.agentProgress.handleNewFragment("FragmentA");
        this.agentProgress.handleNewFragment("FragmentB");
        assertFalse(this.agentProgress.didFinish());

        this.agentProgress.failWaitingFragments();
        this.agentProgress.setProgress(Progress.SUCCESSFUL);

        assertTrue(this.agentProgress.didFinish());
    }

}
