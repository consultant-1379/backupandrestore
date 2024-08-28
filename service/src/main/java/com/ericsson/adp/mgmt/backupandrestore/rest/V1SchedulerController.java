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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.adp.mgmt.backupandrestore.exception.NotImplementedException;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.SchedulerResponse;

/**
 * Responsible for V1 scheduler endpoints.
 */
@RestController
public class V1SchedulerController extends V1Controller {

    /**
     * Gets a backupManager's scheduler.
     * @param backupManagerId which backupManager to look for.
     * @return backupManager's scheduler.
     */
    @GetMapping("backup-manager/{backupManagerId}/scheduler")
    public SchedulerResponse getScheduler(@PathVariable("backupManagerId") final String backupManagerId) {
        throw new NotImplementedException();
    }
}
