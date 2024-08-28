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

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.CreateEventRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.CreateEventResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.EventResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.EventsResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.SchedulerRequest;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V1_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V3_BASE_URL;

public class SchedulerControllerSystemTest extends SystemTest {

    @Test
    public void updateScheduler_v1RestCallWithBackupManagerIdAndRequest_notImplemented() throws Exception {
        final SchedulerRequest request = new SchedulerRequest();

        final ResponseEntity<String> responseEntity = restTemplate.postForEntity(V1_BASE_URL + "backup-manager/123/scheduler", request, String.class);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }

    @Test
    public void getEvents_v1RestCallWithBackupManagerId_notImplemented() throws Exception {
        final ResponseEntity<EventsResponse> responseEntity = restTemplate.getForEntity(V1_BASE_URL + "backup-manager/123/scheduler/event",
                EventsResponse.class);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }

    @Test
    public void createEvent_v1RestCallWithBackupManagerIdAndRequest_notImplemented() throws Exception {
        final CreateEventRequest request = new CreateEventRequest();

        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.postForEntity(V1_BASE_URL + "backup-manager/123/scheduler/event",
                request, CreateEventResponse.class);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }

    @Test
    public void getEvent_v1RestCallWithBackupManagerIdAndEventId_notImplemented() throws Exception {
        final ResponseEntity<EventResponse> responseEntity = restTemplate.getForEntity(V1_BASE_URL + "backup-manager/123/scheduler/event/789",
                EventResponse.class);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }

    @Test
    public void deleteEvent_v1RestCallWithBackupManagerIdAndEventId_notImplemented() throws Exception {
        final ResponseEntity<String> responseEntity = restTemplate.exchange(V1_BASE_URL + "backup-manager/123/scheduler/event/789", HttpMethod.DELETE,
                null, String.class);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }

    @Test
    public void updateScheduler_v3RestCallWithBackupManagerIdAndRequest_notImplemented() throws Exception {
        final SchedulerRequest request = new SchedulerRequest();

        final ResponseEntity<String> responseEntity = restTemplate.postForEntity(V3_BASE_URL + "backup-manager/123/scheduler", request, String.class);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }

    @Test
    public void getEvents_v3RestCallWithBackupManagerId_notImplemented() throws Exception {
        final ResponseEntity<EventsResponse> responseEntity = restTemplate.getForEntity(V3_BASE_URL + "backup-manager/123/scheduler/event",
                EventsResponse.class);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }

    @Test
    public void createEvent_v3RestCallWithBackupManagerIdAndRequest_notImplemented() throws Exception {
        final CreateEventRequest request = new CreateEventRequest();

        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.postForEntity(V3_BASE_URL + "backup-manager/123/scheduler/event",
                request, CreateEventResponse.class);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }

    @Test
    public void getEvent_v3RestCallWithBackupManagerIdAndEventId_notImplemented() throws Exception {
        final ResponseEntity<EventResponse> responseEntity = restTemplate.getForEntity(V3_BASE_URL + "backup-manager/123/scheduler/event/789",
                EventResponse.class);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }

    @Test
    public void deleteEvent_v3RestCallWithBackupManagerIdAndEventId_notImplemented() throws Exception {
        final ResponseEntity<String> responseEntity = restTemplate.exchange(V3_BASE_URL + "backup-manager/123/scheduler/event/789", HttpMethod.DELETE,
                null, String.class);

        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }
}
