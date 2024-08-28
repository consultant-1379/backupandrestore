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

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_DEFAULT_BACKUP_MANAGER_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V3_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V3_DEFAULT_BACKUP_MANAGER_BACKUP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ericsson.adp.mgmt.backupandrestore.backup.BackupCreationType;
import com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus;
import com.ericsson.adp.mgmt.backupandrestore.rest.DisableMimeSniffingFilter;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.BackupResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.BackupsResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.backup.UpdateBackupRequest;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class V3BackupControllerSystemTest extends SystemTest {

    @Test
    public void getBackupsForOneBackupManager_backupManagerId_getsAllBackupsForBackupManager() throws Exception {
        final String expectedBackupId = "backupToKeep";
        final ResponseEntity<BackupsResponse> responseEntity = restTemplate.getForEntity(getScopedBackupUrl(V3_BASE_URL, "backupManagerWithBackupToDelete"),
                BackupsResponse.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        final BackupsResponse response = responseEntity.getBody();

        assertFalse(response.getBackups().isEmpty());
        assertTrue(response.getBackups().stream().map(BackupResponse::getBackupId).anyMatch(obtainedId -> obtainedId.equals(expectedBackupId)));
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getBackupsForOneBackupManager_inexistingBackupManagerId_notFound() throws Exception {
        final ResponseEntity<BackupsResponse> responseEntity = restTemplate.getForEntity(V3_BASE_URL + "backup-manager/qwe/backup",
                BackupsResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getOneBackupForOneBackupManager_backupManagerIdAndBackupId_getBackup() throws Exception {
        final String backupId = "backupToKeep";

        final ResponseEntity<BackupResponse> responseEntity = restTemplate
                .getForEntity(getScopedBackupUrl(V3_BASE_URL, "backupManagerWithBackupToDelete") + "/" + backupId, BackupResponse.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        final BackupResponse response = responseEntity.getBody();

        assertEquals(backupId, response.getBackupId());
        assertEquals(backupId, response.getName());
        assertEquals(BackupCreationType.MANUAL, response.getCreationType());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void getOneBackupForOneBackupManager_backupWasIncompleteWhenSystemStarted_backupIsCorrupted() throws Exception {
        final BackupResponse backup = restTemplate.getForObject(getScopedBackupUrl(V3_BASE_URL, "backupManagerWithBackupToDelete") + "/incompleteBackup", BackupResponse.class);

        assertEquals(BackupStatus.CORRUPTED, backup.getStatus());
    }

    @Test
    public void getOneBackupForOneBackupManager_inexistingBackupId_notFound() throws Exception {
        final ResponseEntity<BackupResponse> responseEntity = restTemplate.getForEntity(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + "notFound",
                BackupResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));

        final ResponseEntity<BackupResponse> responseEntityV3 = restTemplate.getForEntity(V3_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + "notFound",
            BackupResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, responseEntityV3.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
            responseEntityV3.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void updateOneBackupForOneBackupManager_backupManagerIdAndBackupIdAndRequest_updatesBackup() throws Exception {
        final String scope = "backupManagerWithBackupToDelete";
        final String backupId = "backupToKeep";

        final UpdateBackupRequest request = new UpdateBackupRequest();
        request.setUserLabel("aaaa");

        final ResponseEntity<String> updateResponseEntity = restTemplate.postForEntity(getScopedBackupUrl(V1_BASE_URL, scope) + "/" + backupId, request, String.class);
        assertEquals(HttpStatus.NO_CONTENT, updateResponseEntity.getStatusCode());

        final ResponseEntity<BackupResponse> responseEntity = restTemplate.getForEntity(getScopedBackupUrl(V3_BASE_URL, scope) + "/" + backupId, BackupResponse.class);

        final BackupResponse response = responseEntity.getBody();

        assertEquals(backupId, response.getBackupId());
        assertEquals("aaaa", response.getUserLabel());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void updateOneBackupForOneBackupManager_inexistingBackupId_notFound() throws Exception {
        final ResponseEntity<String> responseEntity = restTemplate.postForEntity(V1_DEFAULT_BACKUP_MANAGER_BACKUP.toString() + "notFound",
                new UpdateBackupRequest(), String.class);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

    @Test
    public void updateOneBackupForOneBackupManager_inexistingBackupManagerId_notFound() throws Exception {
        final ResponseEntity<String> responseEntity = restTemplate.postForEntity(V1_BASE_URL + "backup-manager/notFound/backup/notFound",
                new UpdateBackupRequest(), String.class);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE,
                responseEntity.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }

}
