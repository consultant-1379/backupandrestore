/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.backupandrestore.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.adp.mgmt.backupandrestore.rest.health.HealthResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.health.HealthResponseView;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * Facade for V1 REST API HealthController
 */
@RestController
public class HealthController extends V1Controller {
    private HealthControllerService healthControllerService;

    /**
     * Get orchestrator's health.
     *
     * @return health of the orchestrator.
     */
    @GetMapping("health")
    @JsonView(HealthResponseView.V1.class)
    public HealthResponse getHealth() {
        return healthControllerService.getHealth();
    }

    @Autowired
    public void setHealthControllerService(final HealthControllerService healthControllerService) {
        this.healthControllerService = healthControllerService;
    }
}
