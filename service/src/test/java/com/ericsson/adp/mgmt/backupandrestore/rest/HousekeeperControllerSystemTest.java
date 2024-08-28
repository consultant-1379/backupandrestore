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
package com.ericsson.adp.mgmt.backupandrestore.rest;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_BASE_URL;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMHousekeepingJson;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;

public class HousekeeperControllerSystemTest extends SystemTest {

    @Test
    public void createAction_createBackupAction_notImplemented() throws Exception {
        final BRMHousekeepingJson response = new BRMHousekeepingJson();

        final ResponseEntity<String> responseEntity = restTemplate.postForEntity(V1_BASE_URL + "backup-manager/123/housekeeper", response, String.class);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getHousekeeper_backupManagerId_notImplemented() throws Exception {
        final ResponseEntity<BRMHousekeepingJson> responseEntity = restTemplate.getForEntity(V1_BASE_URL + "backup-manager/123/housekeeper", BRMHousekeepingJson.class);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

}
