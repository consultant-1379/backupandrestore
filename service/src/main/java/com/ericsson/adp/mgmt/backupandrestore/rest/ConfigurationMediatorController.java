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
package com.ericsson.adp.mgmt.backupandrestore.rest;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.URLMAPPING;
import static org.springframework.http.HttpStatus.OK;

import com.ericsson.adp.mgmt.backupandrestore.rest.v3.V3Controller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.adp.mgmt.backupandrestore.action.MediatorNotificationHandler;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.MediatorRequest;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;

/**
 * Receives the publish updates from CMMediator
 */
@RestController
public class ConfigurationMediatorController extends V3Controller {

    private static final Logger log = LogManager.getLogger(ConfigurationMediatorController.class);

    private MediatorNotificationHandler mediatorNotificationHandler;

    /**
     * Receives the updates from CMMediator
     * @param request elements modified
     * @return message to complement the REST response
     */
    @PostMapping(URLMAPPING)
    @ResponseStatus(OK)
    public String updateConfiguration(
                                      @RequestBody final MediatorRequest request) {
        final JsonService jsonService = new JsonService();
        if (log.isDebugEnabled()) {
            log.debug("Notification received from Mediator: {}", jsonService.toJsonString(request));
        }

        mediatorNotificationHandler.handleMediatorRequest(request);
        return "notification received";
    }

    @Autowired
    public void setMediatorService(final MediatorNotificationHandler mediatorNotificationHandler) {
        this.mediatorNotificationHandler = mediatorNotificationHandler;
    }
}
