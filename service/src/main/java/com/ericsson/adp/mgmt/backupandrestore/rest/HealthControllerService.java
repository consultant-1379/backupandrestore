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
package com.ericsson.adp.mgmt.backupandrestore.rest;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentRepository;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.job.Job;
import com.ericsson.adp.mgmt.backupandrestore.job.JobExecutor;
import com.ericsson.adp.mgmt.backupandrestore.rest.health.HealthResponse;

/**
 * Health endpoint response handling.
 */
@Service
public class HealthControllerService {
    private AgentRepository agentRepository;
    private JobExecutor jobExecutor;
    private BackupManagerRepository backupManagerRepository;

    /**
     * Get orchestrator's health.
     *
     * @return health of the orchestrator.
     */
    public HealthResponse getHealth() {
        backupManagerRepository.cycle();

        final List<Job> runningJobs = jobExecutor.getRunningJobs();

        /**
         * The health endpoint will show that BRO is busy as long as there is
         * one action running in the execution threads.
         *
         * The v4 health endpoint shows the progressURL.
         * If there are parallel actions running, the first action picked
         * up by the execution threads will be shown in the progressURL.
         *
         * The action shown in the progressURL will be refined in a future story.
         */
        if (!runningJobs.isEmpty()) {
            final Job firstRunningJob = runningJobs.get(0);
            return new HealthResponse(
                    firstRunningJob.getAction(),
                    firstRunningJob.getBackupManagerId(),
                    agentRepository.getAgents().stream().map(Agent::getAgentId)
                            .collect(Collectors.toList()));
        }

        return new HealthResponse(
                agentRepository.getAgents().stream().map(Agent::getAgentId)
                        .collect(Collectors.toList()));
    }

    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

    @Autowired
    public void setJobExecutor(final JobExecutor jobExecutor) {
        this.jobExecutor = jobExecutor;
    }

    @Autowired
    public void setAgentRepository(final AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

}
