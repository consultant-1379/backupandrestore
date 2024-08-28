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
package com.ericsson.adp.mgmt.backupandrestore.job.progress;

import static com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress.WAITING_RESULT;
import static com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress.SUCCESSFUL;
import static com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress.DISCONNECTED;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible for holding the progress of one agent's action.
 */
public class AgentProgress {

    private Progress progress = WAITING_RESULT;
    private final Map<String, Progress> fragmentProgress = new ConcurrentHashMap<>();

    /**
     * Check whether the agent's progress and fragmentProgress is still WAITING_RESULT
     * @return true if neither are WAITING_RESULT
     */
    public boolean didFinish() {
        return !WAITING_RESULT.equals(progress) &&
            this.fragmentProgress.values().stream().noneMatch(WAITING_RESULT::equals);
    }

    /**
     * Check whether the agent's progress and fragmentProgress is SUCCESSFUL
     * @return true if both are successful.
     */
    public boolean didSucceed() {
        return this.fragmentProgress.values().stream().allMatch(SUCCESSFUL::equals) &&
                SUCCESSFUL.equals(progress);
    }

    /**
     * Check whether the agent is connected.
     * @return true if the agent is not disconnected.
     */
    public boolean isConnected() {
        return !DISCONNECTED.equals(progress);
    }

    /**
     * Receive a new fragment.
     * @param fragmentId the fragment id.
     */
    public void handleNewFragment(final String fragmentId) {
        this.fragmentProgress.put(fragmentId, WAITING_RESULT);
    }

    /**
     * Set the fragment progress of fragments in waiting state to failed
     */
    public void failWaitingFragments() {
        final Iterator<String> fragments = this.fragmentProgress.keySet().iterator();
        while (fragments.hasNext()) {
            final String fragmentId = fragments.next();
            if (isFragmentWaiting(fragmentId) ) {
                setFragmentProgress(fragmentId, Progress.FAILED);
            }
        }
    }

    /**
     * Set the fragment's progress
     * @param fragmentId fragment id
     * @param progress fragment's progress
     */
    public void setFragmentProgress(final String fragmentId, final Progress progress) {
        this.fragmentProgress.put(fragmentId, progress);
    }

    /**
     * Used for V2 handling. Don't use for anything else.
     *
     * @param progress - A Progress to set for the Agent.
     */
    public void setProgress(final Progress progress) {
        this.progress = progress;
    }

    public Progress getProgress() {
        return progress;
    }

    @Override
    public String toString() {
        return "{" + "progress=" + progress + ", fragments=" + fragmentProgress + "}";
    }

    private boolean isFragmentWaiting(final String fragmentId) {
        return fragmentProgress.get(fragmentId).equals(WAITING_RESULT);
    }


}
