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
package com.ericsson.adp.mgmt.backupandrestore.agent.discovery;

import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;

/**
 * Responsible for validating if all agents required for an action are registered.
 */
public interface AgentDiscoveryService {

    /**
     * Responsible for validating if all agents required for an action are registered.
     * @param backupManager owner of action.
     * @param registeredAgents currently registered agents.
     */
    void validateRegisteredAgents(BackupManager backupManager, List<Agent> registeredAgents);

}
