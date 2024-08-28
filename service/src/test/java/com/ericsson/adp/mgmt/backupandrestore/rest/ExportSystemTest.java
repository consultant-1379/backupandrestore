/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_BASE_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ExportPayload;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.ActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;

public class ExportSystemTest extends SystemTest {

    public static final URI SFTP_USER_1_222_REMOTE = URI.create("sftp://user@[::1]:222/remote");

    @Test
    public void createExportAction_validRequest_failureActionWithNoPassword() throws Exception {

        final String BACKUP_MANAGER_ID = "backupManagerWithBackupToDelete";

        final String BACKUP_MANAGER_ACTION_URL = V1_BASE_URL + "backup-manager/" + BACKUP_MANAGER_ID + "/action";

        final CreateActionRequest request = new CreateActionRequest();

        final ExportPayload payload = new ExportPayload();
        payload.setUri(SFTP_USER_LOCALHOST_222_REMOTE);
        payload.setPassword("password");
        payload.setBackupName("backupToKeep");
        request.setAction(ActionType.EXPORT);
        request.setPayload(payload);

        final String actionId = restTemplate.postForEntity(BACKUP_MANAGER_ACTION_URL, request, CreateActionResponse.class).getBody().getActionId();

        waitForActionToFinish(BACKUP_MANAGER_ID, actionId);

        final ResponseEntity<ActionResponse> responseEntity = restTemplate.getForEntity(BACKUP_MANAGER_ACTION_URL + "/" + actionId, ActionResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(ActionType.EXPORT, responseEntity.getBody().getName());
        assertEquals(ResultType.FAILURE, responseEntity.getBody().getResult());
        assertTrue(responseEntity.getBody().getPayload() instanceof ExportPayload);
        assertNull(((ExportPayload) responseEntity.getBody().getPayload()).getPassword());

    }

    @Test
    public void createExportAction_validRequest_failureActionWithNoPassword_IPv6() throws Exception {

        final String BACKUP_MANAGER_ID = "backupManagerWithBackupToDelete";

        final String BACKUP_MANAGER_ACTION_URL = V1_BASE_URL + "backup-manager/" + BACKUP_MANAGER_ID + "/action";

        final CreateActionRequest request = new CreateActionRequest();

        final ExportPayload payload = new ExportPayload();
        payload.setUri(SFTP_USER_1_222_REMOTE);
        payload.setPassword("password");
        payload.setBackupName("backupToKeep");
        request.setAction(ActionType.EXPORT);
        request.setPayload(payload);

        final String actionId = restTemplate.postForEntity(BACKUP_MANAGER_ACTION_URL, request, CreateActionResponse.class).getBody().getActionId();

        waitForActionToFinish(BACKUP_MANAGER_ID, actionId);

        final ResponseEntity<ActionResponse> responseEntity = restTemplate.getForEntity(BACKUP_MANAGER_ACTION_URL + "/" + actionId, ActionResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(ActionType.EXPORT, responseEntity.getBody().getName());
        assertEquals(ResultType.FAILURE, responseEntity.getBody().getResult());
        assertTrue(responseEntity.getBody().getPayload() instanceof ExportPayload);
        assertNull(((ExportPayload) responseEntity.getBody().getPayload()).getPassword());

    }
}
