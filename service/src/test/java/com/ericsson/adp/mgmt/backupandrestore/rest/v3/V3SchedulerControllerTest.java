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
package com.ericsson.adp.mgmt.backupandrestore.rest.v3;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V3_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.util.YangEnabledDisabled.ENABLED;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.HttpMethod.PUT;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.AdminState;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerInformation;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.CreateEventResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.PeriodicEventRequestOrResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.PeriodicEventsResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.SchedulerRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.SchedulerResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class V3SchedulerControllerTest extends SystemTest {

    @Autowired
    private PeriodicEventFileService eventFileService;
    @Autowired
    private SchedulerFileService schedulerFileService;

    private static final URI SFTP_USER_LOCALHOST_222_REMOTE = URI.create("sftp://user@localhost:222/remote");

    @Test
    public void getScheduler_v3RestCallWithPersistedBackupManagerId_returnsPersistedSchedulerInformation() throws Exception {
        final ResponseEntity<SchedulerResponse> responseEntity = restTemplate.getForEntity(V3_BASE_URL +
                "backup-managers/backupManagerWithSchedulerInfo/scheduler",
                SchedulerResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(AdminState.UNLOCKED, responseEntity.getBody().getAdminState());
        assertEquals("SCHEDULED_BUP", responseEntity.getBody().getScheduledBackupName());
        assertNull(responseEntity.getBody().getMostRecentlyCreatedAutoBackup());
        assertNull(responseEntity.getBody().getNextScheduledTime());
    }

    @Test
    public void getScheduler_v3RestCallWithNonExistingBackupManagerId_notFoundResponse() throws Exception {
        final ResponseEntity<SchedulerResponse> responseEntity = restTemplate.getForEntity(V3_BASE_URL + "backup-managers/123/scheduler",
                SchedulerResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void getPeriodicEvents_v3RestCallWithNonExistingBackupManagerId_notFoundResponse() throws Exception {
        final ResponseEntity<PeriodicEventsResponse> responseEntity = restTemplate.getForEntity(V3_BASE_URL + "backup-managers/123/scheduler/periodic-events",
                PeriodicEventsResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void getPeriodicEvents_v3RestCallWithPersistedBackupManagerId_returnsPeriodicEventList() throws Exception {
        final ResponseEntity<PeriodicEventsResponse> responseEntity = restTemplate.getForEntity(V3_BASE_URL +
                "backup-managers/backupManagerWithBackupToDelete/scheduler/periodic-events",
                PeriodicEventsResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().getEvents().size() > 0);
    }

    @Test
    public void getPeriodicEvent_v3RestCallWithPersistedBackupManagerId_returnsPeriodicEvent() throws Exception {
        final ResponseEntity<PeriodicEventRequestOrResponse> responseEntity = restTemplate.getForEntity(V3_BASE_URL +
                "backup-managers/backupManagerWithBackupToDelete/scheduler/periodic-events/periodic",
                PeriodicEventRequestOrResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("periodic", responseEntity.getBody().getEventId());
        assertEquals(Integer.valueOf(2), responseEntity.getBody().getHours());
        assertEquals(Integer.valueOf(3), responseEntity.getBody().getMinutes());
        assertEquals(Integer.valueOf(0), responseEntity.getBody().getWeeks());
        assertEquals(Integer.valueOf(0), responseEntity.getBody().getDays());
        assertEquals("2020-09-30T11:23:22.265Z", responseEntity.getBody().getStartTime());
    }

    @Test
    public void getPeriodicEvent_v3RestCallWithNonExistingEventId_notFoundResponse() throws Exception {
        final ResponseEntity<PeriodicEventRequestOrResponse> responseEntity = restTemplate.getForEntity(V3_BASE_URL + "backup-managers/DEFAULT/scheduler/periodic-events/123",
                PeriodicEventRequestOrResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void postPeriodicEvent_v3RestCallWithDefaultBackupManagerId_returnsPeriodicEventId() {
        final ResponseEntity<CreateEventResponse> responseEntity = createPeriodicEvent("DEFAULT", 1, "9018-06-27T11:45:56Z");

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody().getEventId());
    }

    @Test
    public void postPeriodicEvent_invalidHours_UnProcessibleEntity() {
        final ResponseEntity<CreateEventResponse> responseEntity = createPeriodicEvent("DEFAULT", -1, "9018-06-27T11:45:56Z");

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody().getEventId());
    }

    @Test
    public void postPeriodicEvent_invalidUInt16_UnProcessibleEntity() {
        final ResponseEntity<CreateEventResponse> responseEntity = createPeriodicEvent("DEFAULT", 65536, "9018-06-27T11:45:56Z");

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody().getEventId());
    }

    @Test
    public void postPeriodicEvent_emptyDate_UnProcessibleEntity() {
        final ResponseEntity<CreateEventResponse> responseEntity = createPeriodicEvent("DEFAULT", 1, "");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody().getEventId());
    }

    @Test
    public void postPeriodicEvent_invalidDate_UnProcessibleEntity() {
        //31st September
        final ResponseEntity<CreateEventResponse> responseEntity = createPeriodicEvent("DEFAULT", 1, "9018-09-31T11:45:56");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody().getEventId());
    }

    @Test
    public void postPeriodicEvent_invalidTime_UnProcessibleEntity() {
        //63rd minute
        final ResponseEntity<CreateEventResponse> responseEntity = createPeriodicEvent("DEFAULT", 1, "9018-09-31T11:63:56");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody().getEventId());
    }

    @Test
    public void postPeriodicEvent_missingNonRequiredValues_returnsPeriodicEventWithDefaultValues() {
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setHours(2);

        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.postForEntity(V3_BASE_URL + "backup-managers/" + "DEFAULT" + "/scheduler/periodic-events",
                request, CreateEventResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody().getEventId());

        final ResponseEntity<PeriodicEventRequestOrResponse> responseEntity2 = restTemplate.getForEntity(V3_BASE_URL + "backup-managers/DEFAULT/scheduler/periodic-events/" + responseEntity.getBody().getEventId(),
                PeriodicEventRequestOrResponse.class);

        assertEquals(Integer.valueOf(0), responseEntity2.getBody().getMinutes());
        assertEquals(Integer.valueOf(0), responseEntity2.getBody().getWeeks());
        assertEquals(Integer.valueOf(0), responseEntity2.getBody().getDays());
        assertEquals(Integer.valueOf(2), responseEntity2.getBody().getHours());

        assertEquals(OffsetDateTime.now().getDayOfYear(),
                OffsetDateTime.parse(responseEntity2.getBody().getStartTime()).getDayOfYear());
        assertNull(responseEntity2.getBody().getStopTime());
    }

    @Test
    public void postPeriodicEvent_duplicateId_UnProcessibleEntity() {
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setEventId("existingId");
        request.setHours(2);

        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.postForEntity(V3_BASE_URL + "backup-managers/" + "DEFAULT" + "/scheduler/periodic-events",
                request, CreateEventResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("existingId", responseEntity.getBody().getEventId());

        // Existing id
        final PeriodicEventRequestOrResponse request2 = new PeriodicEventRequestOrResponse();
        request2.setEventId("existingId");
        request2.setHours(3);

        final ResponseEntity<CreateEventResponse> responseEntity2 = restTemplate.postForEntity(V3_BASE_URL + "backup-managers/" + "DEFAULT" + "/scheduler/periodic-events",
                request2, CreateEventResponse.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity2.getStatusCode());
    }

    @Test
    public void postPeriodicEvent_missingHours_UnProcessibleEntity() {
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setMinutes(2);
        request.setDays(3);
        request.setWeeks(4);
        request.setStopTime("9018-06-28T11:45:56Z");

        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.postForEntity(V3_BASE_URL + "backup-managers/" + "DEFAULT" + "/scheduler/periodic-events",
                request, CreateEventResponse.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody().getEventId());
    }

    @Test
    public void getPeriodicEvent_v3RestCallWithDefaultBackupManagerId_returnsPeriodicEvent() {
        final ResponseEntity<CreateEventResponse> responseEntity = createPeriodicEvent("DEFAULT", 1, "9018-06-28T11:45:56+02:00");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        final ResponseEntity<PeriodicEventRequestOrResponse> responseEntity2 = restTemplate.getForEntity(V3_BASE_URL + "backup-managers/DEFAULT/scheduler/periodic-events/" + responseEntity.getBody().getEventId(),
                PeriodicEventRequestOrResponse.class);

        assertEquals(responseEntity2.getBody().getEventId(), responseEntity.getBody().getEventId());
        assertEquals(Integer.valueOf(1), responseEntity2.getBody().getHours());
        assertEquals(Integer.valueOf(2), responseEntity2.getBody().getMinutes());
        assertEquals(Integer.valueOf(3), responseEntity2.getBody().getDays());
        assertEquals(Integer.valueOf(4), responseEntity2.getBody().getWeeks());
        assertEquals("9018-06-28T11:45:56+02:00", responseEntity2.getBody().getStartTime());
        assertEquals("9018-06-28T11:45:56Z", responseEntity2.getBody().getStopTime());
    }

    @Test
    public void getPeriodicEvents_v3RestCallWithExistingBackupManagerId_returnsPeriodicEvent() {
        final ResponseEntity<CreateEventResponse> responseEntity = createPeriodicEvent("backupManagerWithBackupToDelete", 1, "9018-06-27T11:45:56");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        final ResponseEntity<PeriodicEventsResponse> responseEntity2 = restTemplate.getForEntity(V3_BASE_URL + "backup-managers/backupManagerWithBackupToDelete/scheduler/periodic-events",
                PeriodicEventsResponse.class);

        assertEquals(1, responseEntity2.getBody().getEvents().stream().filter(e -> e.getEventId().equals(responseEntity.getBody().getEventId())).count());
        assertEquals(Integer.valueOf(1), responseEntity2.getBody().getEvents().stream().filter(e -> e.getEventId().equals(responseEntity.getBody().getEventId())).findFirst().get().getHours());
        assertEquals(DateTimeUtils.parseToOffsetDateTime("9018-06-27T11:45:56").toString(), responseEntity2.getBody().getEvents().stream().filter(e -> e.getEventId().equals(responseEntity.getBody().getEventId())).findFirst().get().getStartTime());
    }

    @Test
    public void deletePeriodicEvent_v3RestCallWithExistingBackupManagerId_backupNoLongerPresent() {
        //Create event 1
        ResponseEntity<CreateEventResponse> createResponseEntity = createPeriodicEvent("backupManagerWithBackupToDelete", 1, "9018-06-27T11:45:56Z");
        assertEquals(HttpStatus.OK, createResponseEntity.getStatusCode());
        final CreateEventResponse eventResponse1 = createResponseEntity.getBody();

        //Create event 2
        createResponseEntity = createPeriodicEvent("backupManagerWithBackupToDelete", 1, "9018-06-27T11:45:56Z");
        assertEquals(HttpStatus.OK, createResponseEntity.getStatusCode());
        final CreateEventResponse eventResponse2 = createResponseEntity.getBody();

        //Check they're created
        ResponseEntity<PeriodicEventsResponse> eventsResponseEntity = restTemplate.getForEntity(V3_BASE_URL +
                "backup-managers/backupManagerWithBackupToDelete/scheduler/periodic-events",
                PeriodicEventsResponse.class);
        assertEquals(1,
                eventsResponseEntity.getBody().getEvents().stream()
                .filter(e -> e.getEventId().equals(eventResponse1.getEventId()))
                .count());
        assertEquals(1,
                eventsResponseEntity.getBody().getEvents().stream()
                .filter(e -> e.getEventId().equals(eventResponse2.getEventId()))
                .count());

        //Check the events are present on disk as well
        assertEquals(1,
                eventFileService.getEvents("backupManagerWithBackupToDelete").stream()
                .filter(e -> e.getEventId().equals(eventResponse1.getEventId()))
                .count());
        assertEquals(1,
                eventFileService.getEvents("backupManagerWithBackupToDelete").stream()
                .filter(e -> e.getEventId().equals(eventResponse2.getEventId()))
                .count());

        //Delete event 1
        restTemplate.delete(V3_BASE_URL + "backup-managers/backupManagerWithBackupToDelete/scheduler/periodic-events/" + eventResponse1.getEventId());

        //Check it's deleted
        eventsResponseEntity = restTemplate.getForEntity(V3_BASE_URL +
                "backup-managers/backupManagerWithBackupToDelete/scheduler/periodic-events",
                PeriodicEventsResponse.class);
        assertEquals(0,
                eventsResponseEntity.getBody().getEvents().stream()
                .filter(e -> e.getEventId().equals(eventResponse1.getEventId()))
                .count());

        //Check event 1 is deleted from disk as well
        assertEquals(0,
                eventFileService.getEvents("backupManagerWithBackupToDelete").stream()
                .filter(e -> e.getEventId().equals(eventResponse1.getEventId()))
                .count());

        //Check event 2 is still present in memory and on disk
        eventsResponseEntity = restTemplate.getForEntity(V3_BASE_URL +
                "backup-managers/backupManagerWithBackupToDelete/scheduler/periodic-events",
                PeriodicEventsResponse.class);
        assertEquals(1,
                eventsResponseEntity.getBody().getEvents().stream()
                .filter(e -> e.getEventId().equals(eventResponse2.getEventId()))
                .count());

        assertEquals(1,
                eventFileService.getEvents("backupManagerWithBackupToDelete").stream()
                .filter(e -> e.getEventId().equals(eventResponse2.getEventId()))
                .count());
    }

    @Test
    public void updateSchedulerConfig_setValues_updatedInMemoryAndOnDiskTest() {
        final SchedulerRequest update = new SchedulerRequest();
        update.setAdminState(AdminState.LOCKED);
        update.setAutoExport(ENABLED);
        update.setAutoExportPassword("test password");
        update.setAutoExportUri(SFTP_USER_LOCALHOST_222_REMOTE);
        update.setMostRecentlyCreatedAutoBackup("should be ignored"); // Should not be set as part of update
        update.setNextScheduledTime(OffsetDateTime.now().toString()); // Should not be set as part of update
        update.setScheduledBackupName("next_backup_name");
        final HttpEntity<SchedulerRequest> request = new HttpEntity<>(update);
        final ResponseEntity<SchedulerResponse> responseEntity = restTemplate.exchange(
                V3_BASE_URL + "backup-managers/backupManagerWithBackupToDelete/scheduler/configuration",
                PUT,
                request,
                SchedulerResponse.class);

        final SchedulerResponse response = responseEntity.getBody();

        //Straightforwardly should be updated values
        assertEquals(update.getAdminState(), response.getAdminState());
        assertEquals(update.getScheduledBackupName(), response.getScheduledBackupName());
        assertEquals(update.getAutoExport(), response.getAutoExport());
        assertEquals(update.getAutoExportUri(), response.getAutoExportUri());

        //Password returned should be obfuscated
        assertNotEquals(update.getAutoExportPassword(), response.getAutoExportPassword());
        assertEquals("*****", response.getAutoExportPassword());

        //Check values that should not have been updated
        assertNotEquals(update.getMostRecentlyCreatedAutoBackup(), response.getMostRecentlyCreatedAutoBackup());
        assertNotEquals(update.getNextScheduledTime(), response.getNextScheduledTime());

        //Verify information has been updated on disk
        final SchedulerInformation persisted = schedulerFileService.getPersistedSchedulerInformation("backupManagerWithBackupToDelete");

        //values which should be updated
        assertEquals(update.getAdminState(), persisted.getAdminState());
        assertEquals(update.getScheduledBackupName(), persisted.getScheduledBackupName());
        assertEquals(update.getAutoExport(), persisted.getAutoExport());
        assertEquals(update.getAutoExportUri(), persisted.getAutoExportUri());
        //Password on disk should not be obfuscated
        assertEquals(update.getAutoExportPassword(), persisted.getAutoExportPassword());

        //Check values that should not have been updated
        assertNotEquals(update.getMostRecentlyCreatedAutoBackup(), persisted.getMostRecentlyCreatedAutoBackup());
        assertNotEquals(update.getNextScheduledTime(), persisted.getNextScheduledTime());
    }

    @Test
    public void updateSchedulerConfig_unsetValues_schedulerConfigNotUpdatedTest() {
        updateSchedulerConfig_setValues_updatedInMemoryAndOnDiskTest(); // To get this managers config into a known state since test ordering is unknown

        // Get the scheduler config from disk before false update
        final SchedulerInformation persistedBefore = schedulerFileService.getPersistedSchedulerInformation("backupManagerWithBackupToDelete");

        // Do a "false" update - no config parameters should change as all update values are invalid / null
        final SchedulerRequest updateNothing = new SchedulerRequest();
        updateNothing.setScheduledBackupName("");
        updateNothing.setAutoExportUri(null);
        updateNothing.setAutoExport(null);
        updateNothing.setAutoExportPassword("");
        updateNothing.setAdminState(null);

        restTemplate.exchange(
                V3_BASE_URL + "backup-managers/backupManagerWithBackupToDelete/scheduler/configuration",
                PUT,
                new HttpEntity<>(updateNothing),
                SchedulerResponse.class);

        // Get the scheduler config from disk after false update
        final SchedulerInformation persistedAfter = schedulerFileService.getPersistedSchedulerInformation("backupManagerWithBackupToDelete");

        // Check nothing was set to the requested update value
        assertNotEquals(updateNothing.getScheduledBackupName(), persistedAfter.getScheduledBackupName());
        assertNotEquals(updateNothing.getAutoExportUri(), persistedAfter.getAutoExportUri());
        assertNotEquals(updateNothing.getAutoExport(), persistedAfter.getAutoExport());
        assertNotEquals(updateNothing.getAutoExportPassword(), persistedAfter.getAutoExportPassword());
        assertNotEquals(updateNothing.getAdminState(), persistedAfter.getAdminState());

        // Check the scheduler configs before and after update are identical
        assertEquals(persistedBefore.getScheduledBackupName(), persistedAfter.getScheduledBackupName());
        assertEquals(persistedBefore.getAutoExportUri(), persistedAfter.getAutoExportUri());
        assertEquals(persistedBefore.getAutoExport(), persistedAfter.getAutoExport());
        assertEquals(persistedBefore.getAutoExportPassword(), persistedAfter.getAutoExportPassword());
        assertEquals(persistedBefore.getAdminState(), persistedAfter.getAdminState());
    }

    @Test
    public void createEvent_startTimeInPast_updateToFuture_updatePeriod_updateStartTimeToPast() {
        // Delete any outstanding events
        final String manager = "DEFAULT";
        getEvents(manager).getBody().getEvents().forEach(e -> deleteEvent(e.getEventId(), manager));

        // Create the event and check it's start time is what we expect
        final OffsetDateTime now = OffsetDateTime.now().truncatedTo(SECONDS);
        final OffsetDateTime startTime = now.minusHours(2).minusMinutes(30);
        final OffsetDateTime expectedNextRun = now.plusMinutes(30);
        PeriodicEventRequestOrResponse eventRequest = new PeriodicEventRequestOrResponse();
        eventRequest.setHours(1);
        eventRequest.setStartTime(startTime.toString());
        final String eventId = createEvent(manager, eventRequest).getBody().getEventId();
        final SchedulerResponse schedulerConfig = getSchedulerConfig(manager).getBody();
        assertEquals(expectedNextRun.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            OffsetDateTime.parse(schedulerConfig.getNextScheduledTime()).truncatedTo(SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        // Update the event to have a start time in the future, and check it's what we expect
        eventRequest = new PeriodicEventRequestOrResponse();
        eventRequest.setStartTime(now.plusHours(3).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).toString());
        PeriodicEventRequestOrResponse updatedEvent = updateEvent(eventId, eventRequest, manager).getBody();
        assertEquals(Integer.valueOf(1), updatedEvent.getHours());
        SchedulerResponse updatedSchedulerConfig = getSchedulerConfig(manager).getBody();
        assertEquals(now.plusHours(3).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            OffsetDateTime.parse(updatedSchedulerConfig.getNextScheduledTime()).truncatedTo(SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        // This time, set the start time to in the future but less than 1 period (1 hour) away, and check it's correct
        eventRequest = new PeriodicEventRequestOrResponse();
        eventRequest.setStartTime(now.plusMinutes(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        updatedEvent = updateEvent(eventId, eventRequest, manager).getBody();
        assertEquals(Integer.valueOf(1), updatedEvent.getHours());
        updatedSchedulerConfig = getSchedulerConfig(manager).getBody();
        assertEquals(now.plusMinutes(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            OffsetDateTime.parse(updatedSchedulerConfig.getNextScheduledTime()).truncatedTo(SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        // Now we should set start time to in the past, but less than 1 period, and make sure the the nextRunTime is what we expect
        eventRequest = new PeriodicEventRequestOrResponse();
        eventRequest.setStartTime(now.minusMinutes(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        updatedEvent = updateEvent(eventId, eventRequest, manager).getBody();
        assertEquals(Integer.valueOf(1), updatedEvent.getHours());
        updatedSchedulerConfig = getSchedulerConfig(manager).getBody();
        assertEquals(now.plusMinutes(50).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            OffsetDateTime.parse(updatedSchedulerConfig.getNextScheduledTime()).truncatedTo(SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        // Now we should reduce the period, and check the next run time is what we expect
        // Since the last step set start time to now - 10 minutes, and we're setting a period of 20 minutes, we expect
        // a next run time of now + 10 minutes
        eventRequest = new PeriodicEventRequestOrResponse();
        eventRequest.setHours(0);
        eventRequest.setMinutes(20);
        updatedEvent = updateEvent(eventId, eventRequest, manager).getBody();
        assertEquals(now.minusMinutes(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).toString(),
            updatedEvent.getStartTime());
        updatedSchedulerConfig = getSchedulerConfig(manager).getBody();
        assertEquals(now.plusMinutes(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                OffsetDateTime.parse(updatedSchedulerConfig.getNextScheduledTime()).truncatedTo(SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            );
    }

    private void deleteEvent(final String eventId, final String managerId) {
        restTemplate.delete(V3_BASE_URL + "backup-managers/" + managerId + "/scheduler/periodic-events/" + eventId);
    }

    private ResponseEntity<PeriodicEventsResponse> getEvents(final String managerId) {
        return restTemplate.getForEntity(
                V3_BASE_URL + "backup-managers/" + managerId + "/scheduler/periodic-events",
                PeriodicEventsResponse.class);
    }

    private ResponseEntity<PeriodicEventRequestOrResponse> updateEvent(final String eventId, final PeriodicEventRequestOrResponse update, final String managerId) {
        return restTemplate.exchange(
                V3_BASE_URL + "backup-managers/" + managerId + "/scheduler/periodic-events/" + eventId,
                PUT,
                new HttpEntity<>(update),
                PeriodicEventRequestOrResponse.class);
    }

    private ResponseEntity<CreateEventResponse> createEvent(final String managerId, final PeriodicEventRequestOrResponse event) {
        return restTemplate.postForEntity(V3_BASE_URL + "backup-managers/" + managerId + "/scheduler/periodic-events", event, CreateEventResponse.class);
    }

    private ResponseEntity<SchedulerResponse> getSchedulerConfig(final String managerId){
        return restTemplate.getForEntity(V3_BASE_URL + "backup-managers/" + managerId + "/scheduler", SchedulerResponse.class);
    }

    private ResponseEntity<CreateEventResponse> createPeriodicEvent(final String backupManagerId, final int hours, final String startTime) {
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setHours(hours);
        request.setMinutes(2);
        request.setDays(3);
        request.setWeeks(4);
        request.setStartTime(startTime);
        request.setStopTime("9018-06-28T11:45:56Z");

        return restTemplate.postForEntity(V3_BASE_URL + "backup-managers/" + backupManagerId + "/scheduler/periodic-events", request, CreateEventResponse.class);
    }

    @Test
    public void autoExport_invalidRequest_invalidBackupName() throws Exception {
        updateSchedulerConfig_setValues_updatedInMemoryAndOnDiskTest();

        // Get the scheduler config from disk before update
        final SchedulerInformation persistedBefore = schedulerFileService.getPersistedSchedulerInformation("backupManagerWithBackupToDelete");

        final SchedulerRequest invalidBackupNameRequest = new SchedulerRequest();
        invalidBackupNameRequest.setScheduledBackupName("invalid/backup");
        invalidBackupNameRequest.setAutoExportUri(SFTP_USER_LOCALHOST_222_REMOTE);
        invalidBackupNameRequest.setAutoExport(ENABLED);
        invalidBackupNameRequest.setAutoExportPassword("password");
        invalidBackupNameRequest.setAdminState(AdminState.UNLOCKED);

        final ResponseEntity<SchedulerResponse> responseEntity = restTemplate.exchange(
                V3_BASE_URL + "backup-managers/backupManagerWithBackupToDelete/scheduler/configuration",
                PUT,
                new HttpEntity<>(invalidBackupNameRequest),
                SchedulerResponse.class);

        // Get the scheduler config from disk after update
        final SchedulerInformation persistedAfter = schedulerFileService.getPersistedSchedulerInformation("backupManagerWithBackupToDelete");

        // Check the scheduler configs before and after update are identical
        assertEquals(persistedBefore.getScheduledBackupName(), persistedAfter.getScheduledBackupName());
        assertEquals(persistedBefore.getAutoExportUri(), persistedAfter.getAutoExportUri());
        assertEquals(persistedBefore.getAutoExport(), persistedAfter.getAutoExport());
        assertEquals(persistedBefore.getAutoExportPassword(), persistedAfter.getAutoExportPassword());
        assertEquals(persistedBefore.getAdminState(), persistedAfter.getAdminState());

        // Check the status code returned
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());

    }

    @Test
    public void autoExport_invalidRequest_emptyBackupName() throws Exception {
        updateSchedulerConfig_setValues_updatedInMemoryAndOnDiskTest();

        // Get the scheduler config from disk before update
        final SchedulerInformation persistedBefore = schedulerFileService.getPersistedSchedulerInformation("backupManagerWithBackupToDelete");

        final SchedulerRequest noBackupNameRequest = new SchedulerRequest();
        noBackupNameRequest.setScheduledBackupName("");
        noBackupNameRequest.setAutoExportUri(SFTP_USER_LOCALHOST_222_REMOTE);
        noBackupNameRequest.setAutoExport(ENABLED);
        noBackupNameRequest.setAutoExportPassword("password");
        noBackupNameRequest.setAdminState(AdminState.UNLOCKED);

        final ResponseEntity<SchedulerResponse> responseEntity = restTemplate.exchange(
                V3_BASE_URL + "backup-managers/backupManagerWithBackupToDelete/scheduler/configuration",
                PUT,
                new HttpEntity<>(noBackupNameRequest),
                SchedulerResponse.class);

        // Get the scheduler config from disk after update
        final SchedulerInformation persistedAfter = schedulerFileService.getPersistedSchedulerInformation("backupManagerWithBackupToDelete");

        // Check the scheduler configs before and after update are identical
        assertEquals(persistedBefore.getScheduledBackupName(), persistedAfter.getScheduledBackupName());
        assertEquals(persistedBefore.getAutoExportUri(), persistedAfter.getAutoExportUri());
        assertEquals(persistedBefore.getAutoExport(), persistedAfter.getAutoExport());
        assertEquals(persistedBefore.getAutoExportPassword(), persistedAfter.getAutoExportPassword());
        assertEquals(persistedBefore.getAdminState(), persistedAfter.getAdminState());

        // Check the status code returned
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());

    }

    @Test
    public void autoExport_invalidRequest_invalidURIFormat() throws Exception {
        updateSchedulerConfig_setValues_updatedInMemoryAndOnDiskTest();

        // Get the scheduler config from disk before update
        final SchedulerInformation persistedBefore = schedulerFileService.getPersistedSchedulerInformation("backupManagerWithBackupToDelete");

        final SchedulerRequest invalidURIFormatRequest = new SchedulerRequest();
        invalidURIFormatRequest.setScheduledBackupName("backup");
        invalidURIFormatRequest.setAutoExportUri(URI.create("sftp://user@@localhost:222/remote"));
        invalidURIFormatRequest.setAutoExport(ENABLED);
        invalidURIFormatRequest.setAutoExportPassword("password");
        invalidURIFormatRequest.setAdminState(AdminState.UNLOCKED);

        final ResponseEntity<SchedulerResponse> responseEntity = restTemplate.exchange(
                V3_BASE_URL + "backup-managers/backupManagerWithBackupToDelete/scheduler/configuration",
                PUT,
                new HttpEntity<>(invalidURIFormatRequest),
                SchedulerResponse.class);

        final SchedulerResponse response = responseEntity.getBody();

        // Get the scheduler config from disk after update
        final SchedulerInformation persistedAfter = schedulerFileService.getPersistedSchedulerInformation("backupManagerWithBackupToDelete");

        // Check the scheduler configs before and after update are identical
        assertEquals(persistedBefore.getScheduledBackupName(), persistedAfter.getScheduledBackupName());
        assertEquals(persistedBefore.getAutoExportUri(), persistedAfter.getAutoExportUri());
        assertEquals(persistedBefore.getAutoExport(), persistedAfter.getAutoExport());
        assertEquals(persistedBefore.getAutoExportPassword(), persistedAfter.getAutoExportPassword());
        assertEquals(persistedBefore.getAdminState(), persistedAfter.getAdminState());

        // Check the status code returned
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
    }

    @Test
    public void autoExport_validRequest_noURIorBackupNameProvided() throws Exception {
        updateSchedulerConfig_setValues_updatedInMemoryAndOnDiskTest();

        // Get the scheduler config from disk before update
        final SchedulerInformation persistedBefore = schedulerFileService.getPersistedSchedulerInformation("backupManagerWithBackupToDelete");

        final SchedulerRequest noURIProvidedRequest = new SchedulerRequest();
        noURIProvidedRequest.setAutoExport(ENABLED);
        noURIProvidedRequest.setAutoExportPassword("password");
        noURIProvidedRequest.setAdminState(AdminState.UNLOCKED);

        final ResponseEntity<SchedulerResponse> responseEntity = restTemplate.exchange(
                V3_BASE_URL + "backup-managers/backupManagerWithBackupToDelete/scheduler/configuration",
                PUT,
                new HttpEntity<>(noURIProvidedRequest),
                SchedulerResponse.class);

        final SchedulerResponse response = responseEntity.getBody();

        // Get the scheduler config from disk after update
        final SchedulerInformation persistedAfter = schedulerFileService.getPersistedSchedulerInformation("backupManagerWithBackupToDelete");

        // Check the scheduler was updated as expected
        assertEquals(persistedBefore.getScheduledBackupName(), persistedAfter.getScheduledBackupName());
        assertEquals(persistedBefore.getAutoExportUri(), persistedAfter.getAutoExportUri());
        assertEquals(persistedBefore.getAutoExport(), persistedAfter.getAutoExport());
        assertEquals("password", persistedAfter.getAutoExportPassword());
        assertEquals(AdminState.UNLOCKED, persistedAfter.getAdminState());

        // Check the status code returned
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

}
