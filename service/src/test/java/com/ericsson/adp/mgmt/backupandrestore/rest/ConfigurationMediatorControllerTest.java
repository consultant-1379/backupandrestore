/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.HOUSEKEEPING_BACKUP_MANAGER;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.HOUSEKEEPING_BACKUP_MANAGER_URL_V3;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V3_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_DISABLED;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.AUTO_DELETE_ENABLED;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.CONFIGURATION_RESOURCE;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.SCHEMA_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.AdminState;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMRestClient;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.EtagNotifIdBase;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMHousekeepingJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.MediatorRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.PeriodicEventRequestOrResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.PeriodicEventsResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.SchedulerResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.HousekeepingSystemTest;
import com.ericsson.adp.mgmt.backupandrestore.util.RestTemplateFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigurationMediatorControllerTest extends HousekeepingSystemTest {
    private static final String SCHEDULER = "/scheduler";
    private static final String PERIODIC_EVENT = "/periodic-event";
    private static final String PERIODIC_EVENTS = "/periodic-events";
    private static final String REMOVE_OPERATION="remove";
    private static final String SCHEDULER_BACKUP_MANAGER = "backupManagerWithoutHousekeepingInfo";
    private static final String SCHEDULER_BACKUP_MANAGER_URL_V3 = V3_BASE_URL + "backup-managers/" + SCHEDULER_BACKUP_MANAGER;
    private static final String TEST_HOST = "http://localhost:5003";
    private static final String TEST_URL = TEST_HOST + "/cm/api/v1";
    private static final String CONFIG_RESOURCE = TEST_URL + "/" + CONFIGURATION_RESOURCE + "/" + SCHEMA_NAME;
    private static final int MAX_ETAG_RETRIES_ATTEMPTS = 5;

    private static final String REST_TEMPLATE_ID = "RestTemplateConfigurationTest";
    private enum Response_expected {RESPONSE_SUCCESS, RESPONSE_4XX, RESPONSE_5XX, RESPONSE_500}
    RestTemplate restTemplateMediator;
    private MockRestServiceServer mockServer;
    @Autowired
    private CMMRestClient cmmRestClient;

    @Autowired
    private EtagNotifIdBase etagNotifIdBase;

    @Test
    public void postSubscription_updateHousekeeping_twoparameters_valid() throws Exception {
        final MediatorRequest mediatorRequest = new ObjectMapper().readValue(getJsonHousekeepingString(HOUSEKEEPING_BACKUP_MANAGER.toString(), AUTO_DELETE_DISABLED, 15), MediatorRequest.class);

        restTemplate.postForEntity(URL_MEDIATOR, mediatorRequest, String.class);
        final BRMHousekeepingJson responseEntity = restTemplate.getForObject(HOUSEKEEPING_BACKUP_MANAGER_URL_V3.toString() + HOUSEKEEPING, BRMHousekeepingJson.class);
        assertEquals(AUTO_DELETE_DISABLED, responseEntity.getAutoDelete());
        assertEquals(15, responseEntity.getMaxNumberBackups());
    }

    @Test
    public void postSubscription_updateHousekeeping_oneParameter_valid() throws Exception {
        MediatorRequest mediatorRequest = new ObjectMapper().readValue(getJsonsingleHousekeepingElement(HOUSEKEEPING_BACKUP_MANAGER.toString(), AUTO_DELETE, AUTO_DELETE_DISABLED), MediatorRequest.class);

        restTemplate.postForEntity(URL_MEDIATOR, mediatorRequest, String.class);
        BRMHousekeepingJson responseEntity = restTemplate.getForObject(HOUSEKEEPING_BACKUP_MANAGER_URL_V3.toString() + HOUSEKEEPING,
                BRMHousekeepingJson.class);
        assertEquals(AUTO_DELETE_DISABLED, responseEntity.getAutoDelete());

        mediatorRequest = new ObjectMapper().readValue(getJsonsingleHousekeepingElement(HOUSEKEEPING_BACKUP_MANAGER.toString(), MAX_STORED_BACKUP, 8), MediatorRequest.class);

        restTemplate.postForEntity(URL_MEDIATOR, mediatorRequest, String.class);
        responseEntity = restTemplate.getForObject(HOUSEKEEPING_BACKUP_MANAGER_URL_V3.toString() + HOUSEKEEPING, BRMHousekeepingJson.class);
        assertEquals(8, responseEntity.getMaxNumberBackups());
    }

    @Test
    public void postSubscription_updateHousekeeping_oneParameter_invalid() throws Exception {
        final MediatorRequest mediatorRequest = new ObjectMapper().readValue(getJsonsingleHousekeepingElement("INVALID_BACKUP", MAX_STORED_BACKUP, 15), MediatorRequest.class);

        restTemplate.postForEntity(URL_MEDIATOR, mediatorRequest, String.class);
        final BRMHousekeepingJson responseEntity = restTemplate.getForObject(HOUSEKEEPING_BACKUP_MANAGER_URL_V3.toString() + HOUSEKEEPING, BRMHousekeepingJson.class);
        assertEquals(1, responseEntity.getMaxNumberBackups());
    }

    @Test
    public void postSubscription_addNewSubscription_ignoreEvent() throws JsonMappingException, JsonProcessingException, JSONException {
        final String subscription = getJsonNotification()
                .put("patch", getPatch(HOUSEKEEPING_BACKUP_MANAGER.toString(), AUTO_DELETE, AUTO_DELETE_DISABLED, "add", HOUSEKEEPING))
                .toString();


        final MediatorRequest mediatorRequest = new ObjectMapper().readValue(subscription, MediatorRequest.class);
        restTemplate.postForEntity(URL_MEDIATOR, mediatorRequest, String.class);

        final BRMHousekeepingJson responseEntity = restTemplate.getForObject(HOUSEKEEPING_BACKUP_MANAGER_URL_V3.toString() + HOUSEKEEPING, BRMHousekeepingJson.class);
        assertEquals(AUTO_DELETE_ENABLED, responseEntity.getAutoDelete());
        assertEquals(1, responseEntity.getMaxNumberBackups());
    }

    @Test
    public void postSubscription_missingeTag_ignoreEvent_retrieveLastEtag() throws JsonMappingException, JsonProcessingException, JSONException {
        String lastEtag = etagNotifIdBase.getEtag();
        initMockMediator();
        declareRestServerExpecting(CONFIG_RESOURCE,
                Response_expected.RESPONSE_SUCCESS,
                getJsonResponse(),
                GET);

        final String subscription = getJsonNotification()
                .put("patch", getHousekeepingPatch(HOUSEKEEPING_BACKUP_MANAGER.toString(), AUTO_DELETE, AUTO_DELETE_DISABLED))
                .toString();
        final MediatorRequest mediatorRequest = new ObjectMapper().readValue(subscription, MediatorRequest.class);
        etagNotifIdBase.setNotifId(23);
        restTemplate.postForEntity(URL_MEDIATOR, mediatorRequest, String.class);

        // retrieves ETAG from Mediator, because NotifId was lower than expected
        assertEquals(CONFIG_ETAG_VALUE, etagNotifIdBase.getEtag());
    }

    @Test
    public void postSubscription_missingeTag_ignoreEvent_NoRetrieveEtagAfterRetries_5XX() throws JsonMappingException, JsonProcessingException, JSONException {
        initMockMediator();
        etagNotifIdBase.updateEtag("1111");
        declareRestServerExpecting(CONFIG_RESOURCE,
                Response_expected.RESPONSE_500,
                getJsonResponse(),
                GET);
        final String subscription = getJsonNotification()
                .put("patch", getHousekeepingPatch(HOUSEKEEPING_BACKUP_MANAGER.toString(), AUTO_DELETE, AUTO_DELETE_DISABLED))
                .put("notifId", "30")  // valid notifId
                .put("configETag", "2222")  // invalid etag
                .toString();
        final MediatorRequest mediatorRequest = new ObjectMapper().readValue(subscription, MediatorRequest.class);
        restTemplate.postForEntity(URL_MEDIATOR, mediatorRequest, String.class);
        // Will retry and get 5XX errors all the times, so not update from Mediator is expected
        // cant retrieves Config_ETAG from Mediator jsonpointer
        assertNotEquals(etagNotifIdBase.getEtag(), CONFIG_ETAG_VALUE);
    }

    @Test
    public void postSubscription_updateScheduler_twoParameters_valid() throws Exception {
        final JSONArray patches = new JSONArray();
        patches.put(getSchedulerPatchContent(SCHEDULER_BACKUP_MANAGER, REPLACE_OPERATION, "admin-state", "locked"));
        patches.put(getSchedulerPatchContent(SCHEDULER_BACKUP_MANAGER, REPLACE_OPERATION, "scheduled-backup-name", "bup"));

        final MediatorRequest mediatorRequest = new ObjectMapper().readValue(getJsonNotificationWithPatches(patches), MediatorRequest.class);

        restTemplate.postForEntity(URL_MEDIATOR, mediatorRequest, String.class);
        final ResponseEntity<SchedulerResponse> responseEntity = restTemplate.getForEntity(SCHEDULER_BACKUP_MANAGER_URL_V3 +
                SCHEDULER,
                SchedulerResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(AdminState.LOCKED, responseEntity.getBody().getAdminState());
        assertEquals("bup", responseEntity.getBody().getScheduledBackupName());
        assertNull(responseEntity.getBody().getMostRecentlyCreatedAutoBackup());
        assertNull(responseEntity.getBody().getNextScheduledTime());
        assertNull(responseEntity.getBody().getAutoExportUri());
        assertTrue(responseEntity.getBody().getAutoExportPassword().isEmpty());

        //reset the changes to avoid test failures
        final JSONArray resetPatches = new JSONArray();
        resetPatches.put(getSchedulerPatchContent(SCHEDULER_BACKUP_MANAGER, REPLACE_OPERATION, "admin-state", "unlocked"));
        resetPatches.put(getSchedulerPatchContent(SCHEDULER_BACKUP_MANAGER, REPLACE_OPERATION, "scheduled-backup-name", "SCHEDULED_BUP"));
        final MediatorRequest resetRequest = new ObjectMapper().readValue(getJsonNotificationWithPatches(resetPatches), MediatorRequest.class);
        restTemplate.postForEntity(URL_MEDIATOR, resetRequest, String.class);
    }

    @Test
    public void postSubscription_updateScheduler_Sftp_Server_Patches() throws Exception {
        // Update "auto-export-uri" and "auto-export-password",
        final String exportURI = "sftp://brsftp@192.168.5.131:22/bro_test/1/3/";
        final String exportPassword = "planbsftp";
        final JSONArray patches = new JSONArray();
        patches.put(getSchedulerPatchContent(SCHEDULER_BACKUP_MANAGER, REPLACE_OPERATION, "auto-export-uri", exportURI));
        patches.put(getSchedulerPatchContent(SCHEDULER_BACKUP_MANAGER, REPLACE_OPERATION, "auto-export-password", exportPassword));
        final MediatorRequest mediatorRequest = new ObjectMapper().readValue(getJsonNotificationWithPatches(patches), MediatorRequest.class);
        restTemplate.postForEntity(URL_MEDIATOR, mediatorRequest, String.class);
        ResponseEntity<SchedulerResponse> responseEntity = restTemplate.getForEntity(SCHEDULER_BACKUP_MANAGER_URL_V3 +
                SCHEDULER, SchedulerResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(exportURI, responseEntity.getBody().getAutoExportUri().toString());
        assertEquals("*****", responseEntity.getBody().getAutoExportPassword());
        assertTrue(responseEntity.getBody().getSftpServerName().isEmpty());

        // then update "sftp-server-name".
        final JSONArray sftpServerNamePatches = new JSONArray();
        final String sftpServerName = "testSftpServer";
        sftpServerNamePatches.put(getSchedulerPatchContent(SCHEDULER_BACKUP_MANAGER, ADD_OPERATION, "sftp-server-name", sftpServerName));
        final MediatorRequest sftpServerRequest = new ObjectMapper().readValue(getJsonNotificationWithPatches(sftpServerNamePatches), MediatorRequest.class);
        restTemplate.postForEntity(URL_MEDIATOR, sftpServerRequest, String.class);
        responseEntity = restTemplate.getForEntity(SCHEDULER_BACKUP_MANAGER_URL_V3 + SCHEDULER, SchedulerResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody().getAutoExportUri());
        assertTrue(responseEntity.getBody().getAutoExportPassword().isEmpty());
        // The REST Response should not contain SFTP server name, but the BRM should have an updated sftpServerName.
        assertTrue(responseEntity.getBody().getSftpServerName().isEmpty());
        final Scheduler scheduler = getScheduler(SCHEDULER_BACKUP_MANAGER);
        assertEquals(sftpServerName, scheduler.getSftpServerName());
        assertNull(scheduler.getAutoExportUri());
        assertTrue(scheduler.getAutoExportPassword().isEmpty());
    }

    @Test
    public void postSubscription_updatePeriodicEvent_createdEvent_empty() throws Exception {
        final MediatorRequest mediatorRequest = new ObjectMapper().readValue(getJsonAddPeriodicEventStringArray("backupManagerWithoutHousekeepingInfo", "Idarr", 5), MediatorRequest.class);

        restTemplate.postForEntity(URL_MEDIATOR, mediatorRequest, String.class);
        final ResponseEntity<PeriodicEventRequestOrResponse> responseEntity = restTemplate.getForEntity(SCHEDULER_BACKUP_MANAGER_URL_V3 +
                SCHEDULER + PERIODIC_EVENTS + "/" + "Idarr",
                PeriodicEventRequestOrResponse.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("Idarr", responseEntity.getBody().getEventId());
        assertEquals(Integer.valueOf(5), responseEntity.getBody().getHours());
        final MediatorRequest removeRequest = new ObjectMapper().readValue(getJsonRemovePeriodicEventString("backupManagerWithoutHousekeepingInfo"), MediatorRequest.class);
        final MediatorRequest removeRequest2 = new ObjectMapper().readValue(getJsonRemovePeriodicEventString("backupManagerWithoutHousekeepingInfo"), MediatorRequest.class);

        restTemplate.postForEntity(URL_MEDIATOR, removeRequest, String.class);
        restTemplate.postForEntity(URL_MEDIATOR, removeRequest2, String.class);
        }

    @Test
    public void postSubscription_updatePeriodicEvent_createdEvent() throws Exception {
        final MediatorRequest mediatorRequest = new ObjectMapper().readValue(getJsonAddPeriodicEventString("backupManagerWithoutHousekeepingInfo", "Id1", 5), MediatorRequest.class);

        restTemplate.postForEntity(URL_MEDIATOR, mediatorRequest, String.class);
        final ResponseEntity<PeriodicEventRequestOrResponse> responseEntity = restTemplate.getForEntity(SCHEDULER_BACKUP_MANAGER_URL_V3 +
                SCHEDULER + PERIODIC_EVENTS + "/" + "Id1",
                PeriodicEventRequestOrResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("Id1", responseEntity.getBody().getEventId());
        assertEquals(Integer.valueOf(5), responseEntity.getBody().getHours());

        final MediatorRequest updateRequest = new ObjectMapper().readValue(getJsonUpdatePeriodicEventString("backupManagerWithoutHousekeepingInfo", "hours", 7), MediatorRequest.class);

        restTemplate.postForEntity(URL_MEDIATOR, updateRequest, String.class);
        final ResponseEntity<PeriodicEventRequestOrResponse> updateResponseEntity = restTemplate.getForEntity(SCHEDULER_BACKUP_MANAGER_URL_V3 +
                SCHEDULER + PERIODIC_EVENTS + "/" + "Id1",
                PeriodicEventRequestOrResponse.class);

        assertEquals(HttpStatus.OK, updateResponseEntity.getStatusCode());
        assertEquals("Id1", updateResponseEntity.getBody().getEventId());
        assertEquals(Integer.valueOf(7), updateResponseEntity.getBody().getHours());

        final MediatorRequest removeRequest = new ObjectMapper().readValue(getJsonRemovePeriodicEventString("backupManagerWithoutHousekeepingInfo"), MediatorRequest.class);
        restTemplate.postForEntity(URL_MEDIATOR, removeRequest, String.class);

        final ResponseEntity<PeriodicEventsResponse> removeResponseEntity = restTemplate.getForEntity(SCHEDULER_BACKUP_MANAGER_URL_V3 +
                SCHEDULER + PERIODIC_EVENTS,
                PeriodicEventsResponse.class);

        assertEquals(HttpStatus.OK, removeResponseEntity.getStatusCode());
        assertEquals(0, removeResponseEntity.getBody().getEvents().size());
    }

    @Test
    public void postSubscription_updatePeriodicEvent_deleteStopTime() throws Exception {
        final MediatorRequest mediatorRequest = new ObjectMapper().readValue(getJsonAddPeriodicEventString("backupManagerWithoutHousekeepingInfo", "Id1", 5), MediatorRequest.class);

        restTemplate.postForEntity(URL_MEDIATOR, mediatorRequest, String.class);
        final ResponseEntity<PeriodicEventRequestOrResponse> responseEntity = restTemplate.getForEntity(SCHEDULER_BACKUP_MANAGER_URL_V3 +
                SCHEDULER + PERIODIC_EVENTS + "/" + "Id1",
                PeriodicEventRequestOrResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("Id1", responseEntity.getBody().getEventId());
        assertEquals(Integer.valueOf(5), responseEntity.getBody().getHours());
        assertNotNull(responseEntity.getBody().getStartTime());
        assertNotNull(responseEntity.getBody().getStopTime());

        final MediatorRequest updateRequest = new ObjectMapper().readValue(getJsonRemoveStopTime("backupManagerWithoutHousekeepingInfo"), MediatorRequest.class);

        restTemplate.postForEntity(URL_MEDIATOR, updateRequest, String.class);
        final ResponseEntity<PeriodicEventRequestOrResponse> updateResponseEntity = restTemplate.getForEntity(SCHEDULER_BACKUP_MANAGER_URL_V3 +
                SCHEDULER + PERIODIC_EVENTS + "/" + "Id1",
                PeriodicEventRequestOrResponse.class);

        assertEquals(HttpStatus.OK, updateResponseEntity.getStatusCode());
        assertEquals("Id1", updateResponseEntity.getBody().getEventId());
        assertEquals(Integer.valueOf(5), updateResponseEntity.getBody().getHours());
        assertNotNull(updateResponseEntity.getBody().getStartTime());
        assertNull(updateResponseEntity.getBody().getStopTime());

        final MediatorRequest removeRequest = new ObjectMapper().readValue(getJsonRemovePeriodicEventString("backupManagerWithoutHousekeepingInfo"), MediatorRequest.class);
        restTemplate.postForEntity(URL_MEDIATOR, removeRequest, String.class);

        final ResponseEntity<PeriodicEventsResponse> removeResponseEntity = restTemplate.getForEntity(SCHEDULER_BACKUP_MANAGER_URL_V3 +
                SCHEDULER + PERIODIC_EVENTS,
                PeriodicEventsResponse.class);

        assertEquals(HttpStatus.OK, removeResponseEntity.getStatusCode());
        assertEquals(0, removeResponseEntity.getBody().getEvents().size());
    }


    protected void nextEtagNotifIdBase (final EtagNotifIdBase etagNotifId) {
        etagNotifId.updateEtag(String.valueOf(Integer.parseInt(etagNotifId.getEtag()) + 1 ));
        etagNotifId.setNotifId(etagNotifId.getNotifId() + 1);
    }

    private JSONObject getSchedulerPatchContent(final String backupManager, String operation, String property, final String value) throws JSONException {
        final int position=getBackupManagerPosition(backupManager);
        return getPatchContent (position, property, value, operation, SCHEDULER);
    }

    private JSONArray getAddEventPatch(final String backupManager, final String id, final int hours) throws JSONException {
        final JSONArray patches = new JSONArray();
        final int position = getBackupManagerPosition(backupManager);
        patches.put(getAddEventPatchContent (position, id, hours, ADD_OPERATION, SCHEDULER + PERIODIC_EVENT, 0));
        return patches;
    }

    private JSONArray getAddEventPatchArray(final String backupManager, final String id, final int hours) throws JSONException {
        final JSONArray patches = new JSONArray();
        final int position = getBackupManagerPosition(backupManager);
        patches.put(getAddEventPatchContent_Array (position, id, hours, ADD_OPERATION, SCHEDULER + PERIODIC_EVENT, 0));
        return patches;
    }

    private JSONArray getRemoveEventPatch(final String backupManager) throws JSONException {
        final JSONArray patches = new JSONArray();
        final int position = getBackupManagerPosition(backupManager);
        patches.put(getRemoveEventPatchContent (position, REMOVE_OPERATION, SCHEDULER + PERIODIC_EVENT, 0));
        return patches;
    }

    private JSONArray getRemoveSTopTimePatch(final String backupManager) throws JSONException {
        final JSONArray patches = new JSONArray();
        final int position = getBackupManagerPosition(backupManager);
        patches.put(getRemoveStopTimetPatchContent (position, REMOVE_OPERATION, SCHEDULER + PERIODIC_EVENT, 0));
        return patches;
    }

    private JSONArray getUpdateEventPatch(final String backupManager, final String element, final int value) throws JSONException {
        final JSONArray patches = new JSONArray();
        final int position = getBackupManagerPosition(backupManager);
        patches.put(getUpdateEventPatchContent (position, element, value, REPLACE_OPERATION, SCHEDULER + PERIODIC_EVENT, 0));
        return patches;
    }

    protected String getJsonNotificationWithPatches(final JSONArray jsonPatches) throws JSONException {
        setEtagFrombase();
        return getJsonNotification ()
                .put("changedBy", "cmyp")
                .put("patch", jsonPatches)
                .put("baseETag", baseEtag)
                .put("configETag", configEtag)
                .put("notifId", notifId)
                .toString();
    }

    protected String getJsonAddPeriodicEventStringArray(final String backupManager, final String id, final int hours) throws JSONException {
        setEtagFrombase();
        return getJsonNotification ()
                .put("changedBy", "cmyp")
                .put("patch", getAddEventPatchArray(backupManager, id, hours))
                .put("baseETag", baseEtag)
                .put("configETag", configEtag)
                .put("notifId", notifId)
                .toString();
    }

    protected String getJsonAddPeriodicEventString(final String backupManager, final String id, final int hours) throws JSONException {
        setEtagFrombase();
        return getJsonNotification ()
                .put("changedBy", "cmyp")
                .put("patch", getAddEventPatch(backupManager, id, hours))
                .put("baseETag", baseEtag)
                .put("configETag", configEtag)
                .put("notifId", notifId)
                .toString();
    }

    protected String getJsonUpdatePeriodicEventString(final String backupManager, final String element, final int value) throws JSONException {
        setEtagFrombase();
        return getJsonNotification ()
                .put("changedBy", "cmyp")
                .put("patch", getUpdateEventPatch(backupManager, element, value))
                .put("baseETag", baseEtag)
                .put("configETag", configEtag)
                .put("notifId", notifId)
                .toString();
    }

    protected String getJsonRemoveStopTime(final String backupManager) throws JSONException {
        setEtagFrombase();
        return getJsonNotification ()
                .put("changedBy", "cmyp")
                .put("patch", getRemoveSTopTimePatch(backupManager))
                .put("baseETag", baseEtag)
                .put("configETag", configEtag)
                .put("notifId", notifId)
                .toString();
    }

    protected String getJsonRemovePeriodicEventString(final String backupManager) throws JSONException {
        setEtagFrombase();
        return getJsonNotification ()
                .put("changedBy", "cmyp")
                .put("patch", getRemoveEventPatch(backupManager))
                .put("baseETag", baseEtag)
                .put("configETag", configEtag)
                .put("notifId", notifId)
                .toString();
    }

    private void initMockMediator() {
        final RestTemplateFactory configuration = new RestTemplateFactory();
        restTemplateMediator = cmmRestClient.getRestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplateMediator).build();
        mockServer.verify();
    }

    private JsonNode getJsonResponse() throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree("{\"key\":\"value\"}");
        JsonNode completeResponseJson = objectMapper.createObjectNode()
                .putPOJO("headers", headers)
                .putPOJO("body", jsonNode);
        return completeResponseJson;
    }

    private void declareRestServerExpecting(final String resource,
                                            final Response_expected response_expected,
                                            final JsonNode responseBody,
                                            final HttpMethod method) {
        try {
            switch (response_expected) {
                case RESPONSE_4XX:
                    mockServer.expect(ExpectedCount.times(MAX_ETAG_RETRIES_ATTEMPTS),
                            requestTo(new URI(resource)))
                    .andExpect(method(method))
                    .andRespond(new DelegateResponseCreator(
                            withStatus(UNAUTHORIZED),
                            withStatus(UNAUTHORIZED),
                            withStatus(UNAUTHORIZED),
                            withSuccess(responseBody.toString(), APPLICATION_JSON).headers(headers),
                            withSuccess(responseBody.toString(), APPLICATION_JSON).headers(headers)
                            ));
                    break;
                case RESPONSE_5XX:
                    mockServer.expect(ExpectedCount.times(MAX_ETAG_RETRIES_ATTEMPTS),
                            requestTo(new URI(resource)))
                    .andExpect(method(method))
                    .andRespond(new DelegateResponseCreator(
                            withServerError(),
                            withServerError(),
                            withServerError(),
                            withSuccess(responseBody.toString(), APPLICATION_JSON).headers(headers),
                            withSuccess(responseBody.toString(), APPLICATION_JSON).headers(headers)
                            ));
                    break;
                case RESPONSE_500:
                    mockServer.expect(ExpectedCount.times(MAX_ETAG_RETRIES_ATTEMPTS),
                            requestTo(new URI(resource)))
                    .andExpect(method(method))
                    .andRespond(new DelegateResponseCreator(
                            withServerError(),
                            withServerError(),
                            withServerError(),
                            withServerError(),
                            withServerError(),
                            withServerError(),
                            withServerError()
                            ));
                    break;
                case RESPONSE_SUCCESS:
                default:
                    mockServer.expect(ExpectedCount.manyTimes(),
                            requestTo(new URI(resource)))
                    .andExpect(method(method))
                    .andRespond(MockRestResponseCreators.withSuccess(responseBody.toString(), APPLICATION_JSON)
                            .headers(headers));
                    break;
            }
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }

    }

    class DelegateResponseCreator implements ResponseCreator {
        private final ResponseCreator[] delegates;
        private int toExecute = 0;

        public DelegateResponseCreator(final ResponseCreator... delegates) {
            this.delegates = delegates;
        }

        @Override
        public ClientHttpResponse createResponse(final ClientHttpRequest request) throws IOException {
            final ClientHttpResponse ret = delegates[toExecute % delegates.length].createResponse(request);
            toExecute++;
            return ret;
        }
    }
}
