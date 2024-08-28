/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2021
 * <p>
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */
package com.ericsson.adp.mgmt.backupandrestore.rest.v4;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.HOUSEKEEPING_BACKUP_MANAGER;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.HOUSEKEEPING_BACKUP_MANAGER_URL_V4;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V4_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;
import static com.ericsson.adp.mgmt.backupandrestore.util.OSUtils.sleep;
import static org.junit.Assert.assertEquals;

import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.V4BRMHousekeepingJson;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.V4UpdateHousekeepingRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.error.ErrorResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.HousekeepingSystemTest;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

public class V4HousekeepingControllerSystemTest extends HousekeepingSystemTest {

    private static final String HOUSEKEEPING = "/configuration/housekeeping";

    private static final String NON_EXISTING_BACKUP_MANAGER_URL_V4 = V4_BASE_URL + "backup-managers/nonExistingBRM";

    @Test
    public void getHousekeeping_invalidBackupManagerId_housekeepingInformation() {
        final ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(NON_EXISTING_BACKUP_MANAGER_URL_V4 + HOUSEKEEPING, ErrorResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, Objects.requireNonNull(response.getBody()).getStatusCode());
        assertEquals("Request was unsuccessful as Backup Manager <nonExistingBRM> was not found", response.getBody().getMessage());
    }

    @Test
    public void getHousekeeping_getDefaultValuesWithAgent_getValues() {
        final V4BRMHousekeepingJson responseEntity = restTemplate.getForObject(HOUSEKEEPING_BACKUP_MANAGER_URL_V4.toString() + HOUSEKEEPING, V4BRMHousekeepingJson.class);
        assertEquals(AUTO_DELETE_ENABLED, responseEntity.getAutoDelete());
        assertEquals(1, responseEntity.getMaxNumberBackups());
    }


    @Test
    public void getHousekeeping_backupManagerWithPersistedHousekeepingInfoFile_persistedHousekeepingInformation() {
        final V4BRMHousekeepingJson responseEntity = restTemplate.getForObject(V4_BASE_URL + "backup-managers/backupManagerWithBackupToDelete" + HOUSEKEEPING, V4BRMHousekeepingJson.class);
        assertEquals(AUTO_DELETE_ENABLED, responseEntity.getAutoDelete());
        assertEquals(6, responseEntity.getMaxNumberBackups());
    }

    @Test
    public void getHousekeeping_backupManagerWithoutPersistedHousekeepingInfoFile_housekeepingInformationWithCurrentNumberOfBackups() {
        final V4BRMHousekeepingJson responseEntity = restTemplate.getForObject(V4_BASE_URL + "backup-managers/" + HOUSEKEEPING_BACKUP_MANAGER.toString() + HOUSEKEEPING, V4BRMHousekeepingJson.class);
        assertEquals(AUTO_DELETE_ENABLED, responseEntity.getAutoDelete());
        assertEquals(1, responseEntity.getMaxNumberBackups());
    }

    @Test
    public void putHousekeeping_validInput_succeed() {
        final V4UpdateHousekeepingRequest request = new V4UpdateHousekeepingRequest();
        request.setAutoDelete("enabled");
        request.setMaxStoredBackups(10);
        HttpEntity<V4UpdateHousekeepingRequest> requestEntity = new HttpEntity<>(request);
        ResponseEntity<Void> response = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/" + HOUSEKEEPING_BACKUP_MANAGER.toString() + HOUSEKEEPING,
                HttpMethod.PUT,
                requestEntity,
                Void.class
        );
        sleep(800);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void putHousekeeping_no_autoDelete_failed() {
        final V4UpdateHousekeepingRequest request = new V4UpdateHousekeepingRequest();
        request.setMaxStoredBackups(2);
        HttpEntity<V4UpdateHousekeepingRequest> requestEntity = new HttpEntity<>(request);
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/" + HOUSEKEEPING_BACKUP_MANAGER.toString() + HOUSEKEEPING,
                HttpMethod.PUT,
                requestEntity,
                ErrorResponse.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Missing required field autoDelete", response.getBody().getMessage());
    }

    @Test
    public void putHousekeeping_no_maxStoredBackups_failed() {
        final V4UpdateHousekeepingRequest request = new V4UpdateHousekeepingRequest();
        request.setAutoDelete("enabled");
        HttpEntity<V4UpdateHousekeepingRequest> requestEntity = new HttpEntity<>(request);
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/" + HOUSEKEEPING_BACKUP_MANAGER.toString() + HOUSEKEEPING,
                HttpMethod.PUT,
                requestEntity,
                ErrorResponse.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Missing required field maxStoredBackups", response.getBody().getMessage());
    }

    @Test
    public void patchHousekeeping_AutoDelete_succeed() {
        final V4UpdateHousekeepingRequest request = new V4UpdateHousekeepingRequest();
        request.setAutoDelete("enabled");
        HttpEntity<V4UpdateHousekeepingRequest> requestEntity = new HttpEntity<>(request);
        ResponseEntity<Void> response = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/" + HOUSEKEEPING_BACKUP_MANAGER.toString() + HOUSEKEEPING,
                HttpMethod.PATCH,
                requestEntity,
                Void.class
        );
        sleep(800);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void patchHousekeeping_MaxStoredBackups_succeed() {
        final V4UpdateHousekeepingRequest request = new V4UpdateHousekeepingRequest();
        request.setMaxStoredBackups(10);
        HttpEntity<V4UpdateHousekeepingRequest> requestEntity = new HttpEntity<>(request);
        ResponseEntity<Void> response = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/" + HOUSEKEEPING_BACKUP_MANAGER.toString() + HOUSEKEEPING,
                HttpMethod.PATCH,
                requestEntity,
                Void.class
        );
        sleep(800);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }


}
