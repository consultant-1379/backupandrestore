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

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V4_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V4_DEFAULT_BACKUP_MANAGER_BACKUP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupCreationType;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.rest.DisableMimeSniffingFilter;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.BackupResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.UpdateBackupRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.V4BackupResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class V4BackupControllerSystemTest extends SystemTest {

    @Test
    public void getBackupsForOneBackupManager_backupManagerId_getsAllBackupsForBackupManager() throws Exception {
        final String expectedBackupId = "backupToKeep";
        final ResponseEntity<List<V4BackupResponse>> responseEntity = restTemplate.exchange(getScopedBackupUrl(V4_BASE_URL, "backupManagerWithBackupToDelete"),
                HttpMethod.GET, null, new ParameterizedTypeReference<List<V4BackupResponse>>() {});

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        final List<V4BackupResponse> response = responseEntity.getBody();

        assertFalse(response.isEmpty());
        assertTrue(response.stream().map(BackupResponse::getBackupId).anyMatch(obtainedId -> obtainedId.equals(expectedBackupId)));
        assertTrue(response.stream().allMatch(backup -> backup.getTask() == null));
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getBackupsForOneBackupManager_inexistentBackupManagerId_notFound() throws Exception {
        final ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity(V4_BASE_URL + "backup-managers/qwe/backup",
                JsonNode.class);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getOneBackupForOneBackupManager_backupManagerIdAndBackupId_getBackup() throws Exception {
        final String backupId = "backupToKeep";

        final ResponseEntity<V4BackupResponse> responseEntity = restTemplate
                .getForEntity(getScopedBackupUrl(V4_BASE_URL, "backupManagerWithBackupToDelete") + "/" + backupId, V4BackupResponse.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        final V4BackupResponse response = responseEntity.getBody();

        assertNull(response.getUserLabel());
        assertEquals(ActionStateType.FINISHED, response.getTask().getState());
        assertEquals(ResultType.SUCCESS, response.getTask().getResult());
        assertEquals(backupId, response.getBackupId());
        assertEquals(backupId, response.getName());
        assertEquals(BackupCreationType.MANUAL, response.getCreationType());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getOneBackupForOneBackupManager_backupWasIncompleteWhenSystemStarted_backupIsCorrupted() throws Exception {
        final BackupResponse backup = restTemplate.getForObject(getScopedBackupUrl(V4_BASE_URL, "backupManagerWithBackupToDelete") + "/incompleteBackup", BackupResponse.class);

        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());
    }

    @Test
    public void getOneBackupForOneBackupManager_inexistentBackupId_notFound() throws Exception {
        final ResponseEntity<V4BackupResponse> responseEntityV4 = restTemplate.getForEntity(V4_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + "/notFound",
            V4BackupResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, responseEntityV4.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntityV4.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void updateOneBackupForOneBackupManager_backupManagerIdAndBackupIdAndRequest_updatesBackup() throws Exception {
        final String scope = "backupManagerWithBackupToDelete";
        final String backupId = "backupToKeep";

        final UpdateBackupRequest request = new UpdateBackupRequest();
        request.setUserLabel("aaaa");

        final ResponseEntity<String> updateResponseEntity = restTemplate.postForEntity(getScopedBackupUrl(V1_BASE_URL, scope) + "/" + backupId, request, String.class);
        assertEquals(HttpStatus.NO_CONTENT, updateResponseEntity.getStatusCode());

        final ResponseEntity<BackupResponse> responseEntity = restTemplate.getForEntity(getScopedBackupUrl(V4_BASE_URL, scope) + "/" + backupId, BackupResponse.class);

        final BackupResponse response = responseEntity.getBody();

        assertEquals(backupId, response.getBackupId());
        assertNull(response.getUserLabel());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

}
