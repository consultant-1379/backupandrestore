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

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_ACTION;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V3_DEFAULT_BACKUP_MANAGER_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApiVersion.API_V3_0;
import static org.junit.Assert.assertEquals;

import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.rest.DisableMimeSniffingFilter;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestAgent;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTestWithAgents;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class V3ActionControllerSystemTest extends SystemTestWithAgents {
    @Test
    public void getActionByBackupManagerBackupIdAndActionId_actionExists_actionFound() {
        final SystemTestAgent agent = createTestAgent("testAgent", API_V3_0);
        final CreateActionRequest randomBackupMessage = getRandomBackupMessage();
        final BackupNamePayload payload = (BackupNamePayload) randomBackupMessage.getPayload();
        final String backup = payload.getBackupName();
        final CreateActionResponse postResponse = restTemplate.postForEntity(V1_ACTION + "", randomBackupMessage, CreateActionResponse.class)
                .getBody();
        final String testActionId = postResponse.getActionId();
        waitUntil(agent::isParticipatingInAction);

        final ResponseEntity<JsonNode> getResponse = restTemplate
                .getForEntity(V3_DEFAULT_BACKUP_MANAGER_BACKUP + backup + "/actions/" + testActionId, JsonNode.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE, getResponse.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));

        final ObjectNode getResponseBody = (ObjectNode) getResponse.getBody();
        assertEquals(testActionId, getResponseBody.get("id").textValue());
        assertEquals(backup, getResponseBody.get("payload").get("backupName").textValue());

        // Force the job to finish to avoid leaving other tests in an unknown state
        agent.shutdown();
        waitForActionToFinish(getResponseBody.get("id").textValue());
    }

    @Test
    public void getAction_ByBackupManageBackupIdAndActionId_actionDoesNotExist_NotFoundStatusCode() {
        final ResponseEntity<JsonNode> getResponse = restTemplate
                .getForEntity(V3_DEFAULT_BACKUP_MANAGER_BACKUP + "does_not/actions/exist", JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
        assertEquals(DisableMimeSniffingFilter.NOSNIFF_HEADER_VALUE, getResponse.getHeaders().getFirst(DisableMimeSniffingFilter.NOSNIFF_HEADER_KEY));
    }
}
