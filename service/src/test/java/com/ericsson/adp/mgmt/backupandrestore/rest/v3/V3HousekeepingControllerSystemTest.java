/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.v3;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.HOUSEKEEPING_BACKUP_MANAGER;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.HOUSEKEEPING_BACKUP_MANAGER_URL_V3;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V3_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_DISABLED;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.EmptyPayload;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMHousekeepingJson;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.UpdateHousekeepingRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.error.ErrorResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.HousekeepingSystemTest;

public class V3HousekeepingControllerSystemTest extends HousekeepingSystemTest {

    private static final String HOUSEKEEPING = "/housekeeping";
    private UpdateHousekeepingRequest request;
    private ResponseEntity<CreateActionResponse> response;
    private CreateActionRequest backupRequest;

    private static final String NON_EXISTING_BACKUP_MANAGER_URL_V3 = V3_BASE_URL + "backup-managers/nonExistingBRM/";

    @Test
    public void getHousekeeping_invalidBackupManagerId_housekeepingInformation() throws Exception {
        final ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(NON_EXISTING_BACKUP_MANAGER_URL_V3 + HOUSEKEEPING, ErrorResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatusCode());
        assertEquals("Request was unsuccessful as Backup Manager <nonExistingBRM> was not found", response.getBody().getMessage());
    }

    @Test
    public void getHousekeeping_getDefaltValuesWithAgent_getValues() throws Exception {
        final BRMHousekeepingJson responseEntity = restTemplate.getForObject(HOUSEKEEPING_BACKUP_MANAGER_URL_V3.toString() + HOUSEKEEPING, BRMHousekeepingJson.class);
        assertEquals(AUTO_DELETE_ENABLED, responseEntity.getAutoDelete());
        assertEquals(1, responseEntity.getMaxNumberBackups());
    }

    @Test
    public void getHousekeeping_backupManagerWithPersistedHousekeepingInfoFile_persistedHousekeepingInformation() {

        final BRMHousekeepingJson responseEntity = restTemplate.getForObject(V3_BASE_URL + "backup-managers/backupManagerWithBackupToDelete/housekeeping", BRMHousekeepingJson.class);
        assertEquals(AUTO_DELETE_ENABLED, responseEntity.getAutoDelete());
        assertEquals(6, responseEntity.getMaxNumberBackups());
    }

    @Test
    public void getHousekeeping_backupManagerWithoutPersistedHousekeepingInfoFile_housekeepingInformationWithCurrentNumberOfBackups() {

        final BRMHousekeepingJson responseEntity = restTemplate.getForObject(V3_BASE_URL + "backup-managers/" + HOUSEKEEPING_BACKUP_MANAGER.toString() + HOUSEKEEPING, BRMHousekeepingJson.class);
        assertEquals(AUTO_DELETE_ENABLED, responseEntity.getAutoDelete());
        assertEquals(1, responseEntity.getMaxNumberBackups());
    }

    @Test
    public void housekeeping_updateHousekeepingWithAgent_SettingChanged() throws Exception {
        response=setHouseKeepingResponse(AUTO_DELETE_DISABLED,5);
        wait5SecForActionToFinish (response.getBody().getActionId());

        final BRMHousekeepingJson responseEntity = restTemplate
                .getForObject(HOUSEKEEPING_BACKUP_MANAGER_URL_V3.toString() + HOUSEKEEPING, BRMHousekeepingJson.class);

        assertEquals(AUTO_DELETE_DISABLED, responseEntity.getAutoDelete());
        assertEquals(5, responseEntity.getMaxNumberBackups());
    }

    @Test
    public void housekeeping_updateHousekeepingWithoutAgent_SettingChanged() throws Exception {

        shutdownAgent();
        response=setHouseKeepingResponse(AUTO_DELETE_DISABLED, 5);
        wait5SecForActionToFinish (response.getBody().getActionId());
        final BRMHousekeepingJson responseEntity = restTemplate
                .getForObject(HOUSEKEEPING_BACKUP_MANAGER_URL_V3.toString() + HOUSEKEEPING, BRMHousekeepingJson.class);
        assertEquals(AUTO_DELETE_DISABLED, responseEntity.getAutoDelete());
        assertEquals(5, responseEntity.getMaxNumberBackups());
    }

    @Test
    public void housekeeping_createBackup_deletebackup() throws Exception {
        response=setHouseKeepingResponse(AUTO_DELETE_ENABLED, 2);
        wait5SecForActionToFinish (response.getBody().getActionId());

        backupRequest = getRandomBackupMessage();
        doBackup(backupRequest);

        backupRequest = getRandomBackupMessage();
        doBackup(backupRequest);

        assertEquals(2, getNumberBackups(HOUSEKEEPING_BACKUP_MANAGER.toString()));
        wait5SecForActionToFinish(setHouseKeepingResponse(AUTO_DELETE_ENABLED, 1).getBody().getActionId());
        restTemplate.getForEntity(HOUSEKEEPING_BACKUP_MANAGER_URL_V3.toString() + HOUSEKEEPING,
                BRMHousekeepingJson.class);
        assertEquals(1, getNumberBackups(HOUSEKEEPING_BACKUP_MANAGER.toString()));
    }

    @Test
    public void housekeeping_postAction_InvalidNumberOfBackupsAllowed() {
        final ResponseEntity<CreateActionResponse> response = setHouseKeepingResponse(AUTO_DELETE_ENABLED, 65536);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    }

    @Test
    public void housekeeping_postAction_InvalidAutoDeleteValue() {
        request = new UpdateHousekeepingRequest();
        request.setAction(ActionType.HOUSEKEEPING);
        request.setPayload(new EmptyPayload("data"));
        request.setAutoDelete("ENABLED");
        request.setMaximumManualBackupsNumberStored(123);
        final ResponseEntity<CreateActionResponse> response = restTemplate.postForEntity(HOUSEKEEPING_BACKUP_MANAGER_URL_V3.toString() + HOUSEKEEPING, request, CreateActionResponse.class);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    }

    private ResponseEntity<CreateActionResponse> setHouseKeepingResponse(final String autodelete, final int maxNumberBackup) {
        request = new UpdateHousekeepingRequest();
        request.setAction(ActionType.HOUSEKEEPING);
        request.setPayload(new EmptyPayload("data"));
        request.setAutoDelete(autodelete);
        request.setMaximumManualBackupsNumberStored(maxNumberBackup);
        final ResponseEntity<CreateActionResponse> response = restTemplate.postForEntity(HOUSEKEEPING_BACKUP_MANAGER_URL_V3.toString() + HOUSEKEEPING, request, CreateActionResponse.class);
        return response;
    }

}
