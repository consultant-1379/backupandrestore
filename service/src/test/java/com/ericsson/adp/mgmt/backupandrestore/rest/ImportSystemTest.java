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

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_DEFAULT_BACKUP_MANAGER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.ImportPayload;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.ActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;

public class ImportSystemTest extends SystemTest {

    @Test
    public void createImportAction_validRequest_failureActionWithNoPassword() throws Exception {

        final CreateActionRequest request = new CreateActionRequest();

        final ImportPayload payload = new ImportPayload();
        payload.setUri(URI.create("sftp://user@localhost:222/remote"));
        payload.setPassword("password");
        request.setAction(ActionType.IMPORT);
        request.setPayload(payload);

        final String actionId = restTemplate.postForEntity(V1_DEFAULT_BACKUP_MANAGER.toString() + "action", request, CreateActionResponse.class).getBody()
                .getActionId();

        waitForActionToFinish("DEFAULT", actionId);

        final ResponseEntity<ActionResponse> responseEntity = restTemplate.getForEntity(V1_DEFAULT_BACKUP_MANAGER.toString() + "action/" + actionId,
                ActionResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(ActionType.IMPORT, responseEntity.getBody().getName());
        assertEquals(ResultType.FAILURE, responseEntity.getBody().getResult());
        assertTrue(responseEntity.getBody().getPayload() instanceof ImportPayload);
        assertNull(((ImportPayload) responseEntity.getBody().getPayload()).getPassword());

    }

}
