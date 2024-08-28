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
package com.ericsson.adp.mgmt.backupandrestore.rest.action;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_DEFAULT_BACKUP_MANAGER;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V2_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V4_BASE_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ericsson.adp.mgmt.backupandrestore.SpringContext;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionStateType;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.yang.YangActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.error.ErrorResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;
import com.ericsson.adp.mgmt.backupandrestore.util.SetTimeouts;

import io.micrometer.core.instrument.MeterRegistry;

public class DeleteBackupSystemTest extends SystemTest {

    private static final String BACKUP_MANAGER_ID = "backupManagerWithBackupToDelete";

    @Test
    public void deleteBackup_inexistingBackupManager_notFound() throws Exception {
        final ResponseEntity<CreateActionResponse> responseEntity = restTemplate.postForEntity(V1_BASE_URL + "backup-manager/qwe/action", getDeleteBackupPayload("1"), CreateActionResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void deleteBackup_inexistentBackup_unprocessableEntityResponse() {
        final CreateActionRequest request = getRandomCreateActionRequest(ActionType.DELETE_BACKUP);
        final ErrorResponse response = restTemplate.postForObject(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, ErrorResponse.class);
        assertEquals(422, response.getStatusCode());
    }

    @Test
    public void deleteBackup_backupName_deletesBackupInformationAndFragments() throws Exception {

        final ResponseEntity<CreateActionResponse> responseEntity = restTemplate.postForEntity(V1_BASE_URL + "backup-manager/backupManagerWithBackupToDelete/action", getDeleteBackupPayload("backupToDelete"), CreateActionResponse.class);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        waitForActionToFinish(BACKUP_MANAGER_ID, responseEntity.getBody().getActionId());

        final Path backupFileLocation = BACKUP_MANAGERS_LOCATION.resolve(BACKUP_MANAGER_ID).resolve("backups").resolve("backupToDelete.json");
        final Path backupFragmentLocation = BACKUP_DATA_LOCATION.resolve(BACKUP_MANAGER_ID).resolve("backupToDelete");

        assertFalse(backupFileLocation.toFile().exists());
        assertFalse(backupFragmentLocation.toFile().exists());
        assertEquals(HttpStatus.NOT_FOUND, restTemplate.getForEntity(V1_BASE_URL + "backup-manager/backupManagerWithBackupToDelete/backup/backupToDelete", String.class).getStatusCode());

        final ActionResponse action = getAction(BACKUP_MANAGER_ID, responseEntity.getBody().getActionId());
        assertEquals(ResultType.SUCCESS, action.getResult());
        assertEquals(ActionStateType.FINISHED, action.getState());
        assertEquals(Double.valueOf(1.0), action.getProgressPercentage());

        // Verify that last action is present in bro.operation.info
        // and bro.operation.end.time metrics
        SpringContext.getBean(MeterRegistry.class).ifPresent(meterRegistry -> {
            assertNotNull(meterRegistry
                    .find("bro.operation.info")
                    .tag("action", ActionType.DELETE_BACKUP.name())
                    .tag("backup_type", BACKUP_MANAGER_ID)
                    .tag("backup_name", "backupToDelete")
                    .gauge());
            assertNotNull(meterRegistry
                    .find("bro.operation.end.time")
                    .tag("action", ActionType.DELETE_BACKUP.name())
                    .tag("backup_type", BACKUP_MANAGER_ID)
                    .gauge());
        });
    }

    @Test
    public void deleteBackup_v2Endpoint_deletesBackupInformationAndFragments() throws Exception {
        final YangActionResponse response = put(
                V2_BASE_URL + "ericsson-brm:brm::backup-manager::delete-backup",
                getYangNamePayload("backupManagerWithBackupToDelete", "otherBackupToDelete"),
                YangActionResponse.class);

        waitForActionToFinish(BACKUP_MANAGER_ID, String.valueOf(response.getActionId()));

        final Path backupFileLocation = BACKUP_MANAGERS_LOCATION.resolve(BACKUP_MANAGER_ID).resolve("backups").resolve("otherBackupToDelete.json");
        final Path backupFragmentLocation = BACKUP_DATA_LOCATION.resolve(BACKUP_MANAGER_ID).resolve("otherBackupToDelete");

        assertFalse(backupFileLocation.toFile().exists());
        assertFalse(backupFragmentLocation.toFile().exists());
        assertEquals(HttpStatus.NOT_FOUND, restTemplate.getForEntity(V1_BASE_URL + "backup-manager/backupManagerWithBackupToDelete/backup/otherBackupToDelete", String.class).getStatusCode());

        final ActionResponse action = getAction(BACKUP_MANAGER_ID, String.valueOf(response.getActionId()));
        assertEquals(ResultType.SUCCESS, action.getResult());
        assertEquals(ActionStateType.FINISHED, action.getState());
        assertEquals(Double.valueOf(1.0), action.getProgressPercentage());

        // Verify that last action is present in bro.operation.info
        // and bro.operation.end.time metrics
        SpringContext.getBean(MeterRegistry.class).ifPresent(meterRegistry -> {
            assertNotNull(meterRegistry
                    .find("bro.operation.info")
                    .tag("action", ActionType.DELETE_BACKUP.name())
                    .tag("backup_type", BACKUP_MANAGER_ID)
                    .tag("backup_name", "otherBackupToDelete")
                    .gauge());
            assertNotNull(meterRegistry
                    .find("bro.operation.end.time")
                    .tag("action", ActionType.DELETE_BACKUP.name())
                    .tag("backup_type", BACKUP_MANAGER_ID)
                    .gauge());
        });
    }

    @Test
    public void deleteBackup_backupWithZeroFragments_deletesBackup() throws Exception {
        final ResponseEntity<CreateActionResponse> responseEntity = restTemplate.postForEntity(V1_BASE_URL + "backup-manager/backupManagerWithBackupToDelete/action", getDeleteBackupPayload("emptyBackup"), CreateActionResponse.class);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        waitForActionToFinish(BACKUP_MANAGER_ID, responseEntity.getBody().getActionId());

        final Path backupFileLocation = BACKUP_MANAGERS_LOCATION.resolve(BACKUP_MANAGER_ID).resolve("backups").resolve("emptyBackup.json");
        final Path backupFragmentLocation = BACKUP_DATA_LOCATION.resolve(BACKUP_MANAGER_ID).resolve("emptyBackup");

        assertFalse(backupFileLocation.toFile().exists());
        assertFalse(backupFragmentLocation.toFile().exists());
        assertEquals(HttpStatus.NOT_FOUND, restTemplate.getForEntity(V1_BASE_URL + "backup-manager/backupManagerWithBackupToDelete/backup/emptyBackup", String.class).getStatusCode());

        final ActionResponse action = getAction(BACKUP_MANAGER_ID, responseEntity.getBody().getActionId());
        assertEquals(ResultType.SUCCESS, action.getResult());
        assertEquals(ActionStateType.FINISHED, action.getState());
        assertEquals(Double.valueOf(1.0), action.getProgressPercentage());
    }

    @Test
    public void v4_deleteBackup_backupWithZeroFragments_deletesBackup() {
        restTemplate.delete(V4_BASE_URL + "backup-managers/" + BACKUP_MANAGER_ID + "/backups/v4BackupToDelete");

        Awaitility.await().atMost(SetTimeouts.TIMEOUT_SECONDS, TimeUnit.SECONDS).until(() -> isLastTaskDone(BACKUP_MANAGER_ID));

        final Path backupFileLocation = BACKUP_MANAGERS_LOCATION.resolve(BACKUP_MANAGER_ID).resolve("backups").resolve("v4BackupToDelete.json");
        final Path backupFragmentLocation = BACKUP_DATA_LOCATION.resolve(BACKUP_MANAGER_ID).resolve("v4BackupToDelete");

        assertFalse(backupFileLocation.toFile().exists());
        assertFalse(backupFragmentLocation.toFile().exists());
        assertEquals(HttpStatus.NOT_FOUND, restTemplate.getForEntity(V1_BASE_URL + "backup-manager/backupManagerWithBackupToDelete/backup/v4BackupToDelete", String.class).getStatusCode());
    }

    private boolean isLastTaskDone(final String backupManagerId) {
        Optional<Action> action = backupManagerRepository.getBackupManager(backupManagerId).getLastAction();
        return ActionStateType.FINISHED.equals(action.get().getState());
    }
    
    private CreateActionRequest getDeleteBackupPayload(final String backupName) {
        final BackupNamePayload payload = new BackupNamePayload();
        payload.setBackupName(backupName);
        final CreateActionRequest request = new CreateActionRequest();
        request.setAction(ActionType.DELETE_BACKUP);
        request.setPayload(payload);
        return request;
    }

}
