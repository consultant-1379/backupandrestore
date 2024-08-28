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
package com.ericsson.adp.mgmt.backupandrestore.rest.v4;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responsible for v4 agent specific endpoints.
 */
@RestController
public class V4AgentController extends V4Controller {

    /**
     * Gets all agents registered to a backupManager.
     * @param backupManagerId of backupManager to look for.
     * @return json list response of all the agents registered to a backupManager.
     */
    @GetMapping("backup-managers/{backupManagerId}/agents")
    public List<String> getAgentsOfABackupManager(@PathVariable("backupManagerId") final String backupManagerId) {
        return getAgents(getBackupManager(backupManagerId));
    }


}
