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
package com.ericsson.adp.mgmt.backupandrestore.agent;

import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_BRO_REGISTERED_AGENTS;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.agent.exception.AlreadyRegisteredAgentException;
import com.ericsson.adp.mgmt.backupandrestore.agent.exception.RegisteringAgentWithoutIdException;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.util.MetricTags;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Holds agents.
 */
@Service
public class AgentRepository {

    private static final String SCOPE_SEPARATOR = ";";
    private static final Logger logger = LogManager.getLogger(AgentRepository.class);
    private BackupManagerRepository backupManagerRepository;
    private final List<Agent> agents = new ArrayList<>();
    private MeterRegistry meterRegistry;
    private VBRMAutoCreate vbrmAutoCreate = VBRMAutoCreate.ALL; //Default to making a vBRM for all agents for all BRMs

    /**
     * Adds agent.
     * @param agent to be saved.
     */
    public void addAgent(final Agent agent) {
        modifyList(() -> {
            validateAgent(agent);
            agents.add(agent);
            logger.info("Added agent {}", agent.getAgentId());
            if (vbrmAutoCreate == VBRMAutoCreate.DEFAULT || vbrmAutoCreate == VBRMAutoCreate.ALL) {
                backupManagerRepository.createBackupManager(
                        BackupManager.DEFAULT_BACKUP_MANAGER_ID,
                        getAutoVBRMScope(BackupManager.DEFAULT_BACKUP_MANAGER_ID, agent),
                        List.of(agent));
            }
            for (final String scope : agent.getScope().split(SCOPE_SEPARATOR)) {
                backupManagerRepository.createBackupManager(scope);
                if (vbrmAutoCreate == VBRMAutoCreate.ALL && !scope.isEmpty()) {
                    backupManagerRepository.createBackupManager(scope, getAutoVBRMScope(scope, agent), List.of(agent));
                }
            }
        });

        // Add agent to bro.registered.agents metric
        METRIC_BRO_REGISTERED_AGENTS.unRegister();
        Gauge.builder(METRIC_BRO_REGISTERED_AGENTS.identification(), () -> 1)
                .description(METRIC_BRO_REGISTERED_AGENTS.description())
                .baseUnit("agents")
                .tag(MetricTags.AGENT.identification(), agent.getAgentId())
                .tag(MetricTags.BACKUP_TYPE_LIST.identification(), agent.getScopeWithDefault())
                .register(this.meterRegistry);
    }

    private static String getAutoVBRMScope(final String scope, final Agent agent) {
        return scope + "-" + agent.getAgentId();
    }

    /**
     * Remove agent.
     * @param agent to be removed.
     */
    public void removeAgent(final Agent agent) {
        modifyList(() -> agents.remove(agent));

        // Remove metric for agent from bro.registered.agents
        try {
            final var meter = meterRegistry
                    .find(METRIC_BRO_REGISTERED_AGENTS.identification())
                    .tag(MetricTags.AGENT.identification(), agent.getAgentId())
                    .meter();
            if (meter != null)  {
                this.meterRegistry.remove(meter);
            }
            registerEmptyAgent();
        } catch (UnsupportedOperationException ex) {
            // If agent being removed has an UnrecognizedState then
            // the agent.getAgentId() call above will throw an
            // UnsupportedOperationException
            logger.debug("Skip metric delete for agent with UnrecognizedState");
        }
    }

    /**
     * If all the agents metrics were removed, then it register an empty agent metric
     */
    private void registerEmptyAgent() {
        final var meter = meterRegistry
                .find(METRIC_BRO_REGISTERED_AGENTS.identification())
                .meter();
        if (meter == null)  {
            METRIC_BRO_REGISTERED_AGENTS.register();
        }
    }

    public List<Agent> getAgents() {
        return new ArrayList<>(agents);
    }

    private synchronized void modifyList(final Runnable modification) {
        modification.run();
    }

    private void validateAgent(final Agent agent) {
        if (agentDoesNotHaveId(agent)) {
            throw new RegisteringAgentWithoutIdException();
        }
        if (alreadyHaveAgentWithTheSameId(agent)) {
            throw new AlreadyRegisteredAgentException(agent.getAgentId());
        }
    }

    private boolean agentDoesNotHaveId(final Agent agent) {
        try {
            return agent.getAgentId().isEmpty();
        } catch (final UnsupportedOperationException e) {
            throw new RegisteringAgentWithoutIdException();
        }
    }

    private boolean alreadyHaveAgentWithTheSameId(final Agent newAgent) {
        return agents
                .stream()
                .anyMatch(agent -> agent.getAgentId().equals(newAgent.getAgentId()));
    }

    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

    @Autowired
    public void setMeterRegistry(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Value("${vBRMAutoCreate:ALL}")
    public void setVbrmAutoCreate(final VBRMAutoCreate autoCreate) {
        this.vbrmAutoCreate = autoCreate;
    }

}
