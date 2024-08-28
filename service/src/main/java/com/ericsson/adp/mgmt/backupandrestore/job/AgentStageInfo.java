/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.job;

import com.ericsson.adp.mgmt.backupandrestore.job.progress.Progress;
import com.ericsson.adp.mgmt.backupandrestore.job.stage.JobStageName;

/**
 * Store Agent information per Stages
 */
public class AgentStageInfo {

    private final String agentId;
    private final JobStageName stage;
    private final Progress agentProgress;

    /**
     * Creates an agentStageInfo with an agentId, stage, and agentProgress.
     * @param agentId Agents ID.
     * @param stage that the agent was in.
     * @param agentProgress Progress of that agent in the stage
     */
    public AgentStageInfo(final String agentId, final JobStageName stage, final Progress agentProgress) {
        this.agentId = agentId;
        this.stage = stage;
        this.agentProgress = agentProgress;

    }

    public String getAgentId() {
        return agentId;
    }

    public JobStageName getStage() {
        return stage;
    }

    public Progress getAgentProgress() {
        return agentProgress;
    }

    @Override
    public String toString() {
        return "AgentStageInfo{" +
                "agentId='" + agentId + '\'' +
                ", stage=" + stage +
                ", agentProgress=" + agentProgress +
                '}';
    }
}
