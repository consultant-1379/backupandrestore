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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager.DEFAULT_BACKUP_MANAGER_ID;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_BASE_URL;
import java.util.List;

import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.BackupManagerResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.BackupManagersResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.manager.UpdateBackupManagerRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.error.ErrorResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;

public class BackupManagerControllerSystemTest extends SystemTest {

    @Test
    public void getBackupManagers_noMatterWhatHasHappened_returnsListContainingDefaultBackupManager() throws Exception {
        final ResponseEntity<BackupManagersResponse> response = restTemplate.getForEntity(V1_BASE_URL + "backup-manager", BackupManagersResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        final List<BackupManagerResponse> backupManagers = response.getBody().getBackupManagers();
        assertFalse(backupManagers.isEmpty());

        final BackupManagerResponse defaultBackupManager = backupManagers.stream().filter(manager -> DEFAULT_BACKUP_MANAGER_ID.equals(manager.getBackupManagerId())).findAny().get();
        assertEquals(DEFAULT_BACKUP_MANAGER_ID, defaultBackupManager.getBackupManagerId());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                response.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getBackupManager_existingBackupManagerId_backupManager() throws Exception {
        final ResponseEntity<BackupManagerResponse> response = restTemplate.getForEntity(V1_BASE_URL + "backup-manager/" + DEFAULT_BACKUP_MANAGER_ID, BackupManagerResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(DEFAULT_BACKUP_MANAGER_ID, response.getBody().getBackupManagerId());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                response.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getBackupManager_inexistingBackupManagerId_notFound() throws Exception {
        final ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(V1_BASE_URL + "backup-manager/qqqqqqqqqqqqq", ErrorResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatusCode());
        assertEquals("Request was unsuccessful as Backup Manager <qqqqqqqqqqqqq> was not found", response.getBody().getMessage());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                response.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void updateBackupManager_backupManagerIdAndRequest_noContentResponseAndBackupManagerIsUpdated() throws Exception {
        final UpdateBackupManagerRequest request = new UpdateBackupManagerRequest();
        request.setBackupDomain("xdsghbdfhnfdhnfghn");
        request.setBackupType("ngnhfghncvbzsdfzsf");

        final ResponseEntity<String> response = restTemplate.postForEntity(V1_BASE_URL + "backup-manager/" + DEFAULT_BACKUP_MANAGER_ID, request, String.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        final BackupManagerResponse backupManager = restTemplate.getForObject(V1_BASE_URL + "backup-manager/" + DEFAULT_BACKUP_MANAGER_ID, BackupManagerResponse.class);

        assertEquals(DEFAULT_BACKUP_MANAGER_ID, backupManager.getBackupManagerId());
        assertEquals("xdsghbdfhnfdhnfghn", backupManager.getBackupDomain());
        assertEquals("ngnhfghncvbzsdfzsf", backupManager.getBackupType());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                response.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void updateBackupManager_inexistingBackupManagerId_notFound() throws Exception {
        final UpdateBackupManagerRequest request = new UpdateBackupManagerRequest();
        request.setBackupDomain("AbC");
        request.setBackupType("dEf");

        final ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(V1_BASE_URL + "backup-manager/imalittleteapot", request, ErrorResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatusCode());
        assertEquals("Request was unsuccessful as Backup Manager <imalittleteapot> was not found", response.getBody().getMessage());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                response.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void updateBackupManager_badRequest_badRequest() throws Exception {
        final MultiValueMap<String, String> headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        final HttpEntity<String> requestEntity = new HttpEntity<>("null", headers);

        final ResponseEntity<String> responseEntity = restTemplate.exchange(V1_BASE_URL + "backup-manager/123", HttpMethod.POST, requestEntity, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

}
