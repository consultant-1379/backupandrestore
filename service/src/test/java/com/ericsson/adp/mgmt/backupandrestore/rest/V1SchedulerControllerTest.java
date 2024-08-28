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

import static org.junit.Assert.assertEquals;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_BASE_URL;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.SchedulerResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;

public class V1SchedulerControllerTest extends SystemTest {

    @Test
    public void getScheduler_v1RestCallWithBackupManagerId_notImplemented() throws Exception {
        final ResponseEntity<SchedulerResponse> responseEntity = restTemplate.getForEntity(V1_BASE_URL + "backup-manager/123/scheduler",
                SchedulerResponse.class);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }
}
