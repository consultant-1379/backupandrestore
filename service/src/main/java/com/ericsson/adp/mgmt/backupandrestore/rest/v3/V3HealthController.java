/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.backupandrestore.rest.v3;

import com.ericsson.adp.mgmt.backupandrestore.rest.HealthControllerService;
import com.ericsson.adp.mgmt.backupandrestore.rest.health.HealthResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.health.HealthResponseView;
import com.fasterxml.jackson.annotation.JsonView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Facade for V3 REST API HealthController
 */
@RestController
public class V3HealthController extends V3Controller {
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
