/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.v4;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.AdminState;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.CalendarEvent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.DayOfWeek;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.DayOfWeekOccurrence;
import com.ericsson.adp.mgmt.backupandrestore.rest.error.ErrorResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.CreateEventResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.CreateEventRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.EventResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.PeriodicEventRequestOrResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.SchedulerRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.SchedulerResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.V4CalendarEventRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.V4CalendarEventsResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.V4PeriodicSchedulesResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.V4SchedulerResponse;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;
import com.ericsson.adp.mgmt.backupandrestore.util.YangEnabledDisabled;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;

import java.util.Objects;
import java.util.Optional;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V4_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V4_SCHEDULER_CALENDAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@FixMethodOrder(MethodSorters.JVM)
public class V4SchedulerControllerTest extends SystemTest {

    JsonService jsonService = new JsonService();
    final String V4_SCHEDULER_URL = "backup-managers/backupManagerWithSchedulerInfo/configuration/scheduling";

    @Autowired
    private SchedulerFileService schedulerFileService;

    @Test
    public void getScheduler_v4RestCallWithPersistedBackupManagerId_returnsPersistedSchedulerInformation() {
        final ResponseEntity<V4SchedulerResponse> responseEntity = restTemplate.getForEntity(V4_BASE_URL +
                        V4_SCHEDULER_URL,
                V4SchedulerResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(AdminState.UNLOCKED, Objects.requireNonNull(responseEntity.getBody()).getAdminState());
        assertNull(responseEntity.getBody().getScheduledBackupName());
        assertEquals("SCHEDULED_BUP", responseEntity.getBody().getScheduledBackupNamePrefix());
        assertNull(responseEntity.getBody().getMostRecentlyCreatedAutoBackup());
        assertNull(responseEntity.getBody().getNextScheduledTime());
    }

    @Test
    public void patchScheduler_v4RestCallWithPersistedBackupManagerId_returnsPersistedSchedulerInformation() {
        final SchedulerRequest request = new SchedulerRequest();
        request.setAdminState(AdminState.UNLOCKED);
        HttpEntity<SchedulerRequest> requestEntity = new HttpEntity<>(request);
        ResponseEntity<Void> response = restTemplate.exchange(
                V4_BASE_URL + V4_SCHEDULER_URL,
                HttpMethod.PATCH,
                requestEntity,
                Void.class
        );
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void putScheduler_v4RestCall_validBody() {
        final SchedulerRequest request = new SchedulerRequest();
        request.setAdminState(AdminState.UNLOCKED);
        request.setAutoExport(YangEnabledDisabled.ENABLED);
        request.setAutoExportUri(SFTP_USER_LOCALHOST_222_REMOTE);
        request.setScheduledBackupName("SCHEDULED_BUP");
        HttpEntity<SchedulerRequest> requestEntity = new HttpEntity<>(request);
        ResponseEntity<Void> response = restTemplate.exchange(
                V4_BASE_URL + V4_SCHEDULER_URL,
                HttpMethod.PUT,
                requestEntity,
                Void.class
        );
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void putScheduler_v4RestCall_missingParameters() {
        final SchedulerRequest request = new SchedulerRequest();
        request.setAdminState(AdminState.UNLOCKED);
        HttpEntity<SchedulerRequest> requestEntity = new HttpEntity<>(request);
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                V4_BASE_URL + V4_SCHEDULER_URL,
                HttpMethod.PUT,
                requestEntity,
                ErrorResponse.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void putPeriodicEvent_v4RestCall() {
        // Create a periodic event
        final ResponseEntity<CreateEventResponse> responseEntity = createPeriodicEvent("DEFAULT", 1, "9018-06-27T11:45:56Z");
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody().getEventId());

        // Update Periodic Event
        PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setHours(2);
        HttpEntity<PeriodicEventRequestOrResponse> entity = new HttpEntity<>(request);
        ResponseEntity<PeriodicEventRequestOrResponse> response = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/periodic-schedules/" + responseEntity.getBody().getEventId(),
                HttpMethod.PUT,
                entity,
                PeriodicEventRequestOrResponse.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals((Integer)0, response.getBody().getMinutes());
        assertEquals((Integer)2, response.getBody().getHours());
        assertEquals((Integer)0, response.getBody().getDays());
        assertEquals((Integer)0, response.getBody().getWeeks());
    }

    @Test
    public void patchPeriodicEvent_v4RestCall() {
        // Create a periodic event
        final ResponseEntity<CreateEventResponse> responseEntity = createPeriodicEvent("DEFAULT", 1, "9018-06-27T11:45:56Z");
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody().getEventId());

        // Update Periodic Event
        PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setHours(2);
        request.setHours(5);
        request.setDays(7);
        request.setWeeks(6);
        HttpEntity<PeriodicEventRequestOrResponse> entity = new HttpEntity<>(request);
        ResponseEntity<PeriodicEventRequestOrResponse> response = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/periodic-schedules/" + responseEntity.getBody().getEventId(),
                HttpMethod.PATCH,
                entity,
                PeriodicEventRequestOrResponse.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals((Integer)2, response.getBody().getMinutes());
        assertEquals((Integer)5, response.getBody().getHours());
        assertEquals((Integer)7, response.getBody().getDays());
        assertEquals((Integer)6, response.getBody().getWeeks());
    }

    @Test
    public void postPeriodicEvent_v4RestCallWithDefaultBackupManagerId_returnsPeriodicEventId() {
        final ResponseEntity<CreateEventResponse> responseEntity = createPeriodicEvent("DEFAULT", 1, "9018-06-27T11:45:56Z");

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody().getEventId());
    }

    @Test
    public void deletePeriodicEvent_v4RestCallWithDefaultBackupManagerId() {
        final ResponseEntity<CreateEventResponse> responseEntity = createPeriodicEvent("DEFAULT", 1, "9018-06-27T11:45:56Z");
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody().getEventId());

        restTemplate.delete(V4_BASE_URL + "backup-managers/DEFAULT/periodic-schedules/" + responseEntity.getBody().getEventId());
        final ResponseEntity<PeriodicEventRequestOrResponse> responseEntity2 = restTemplate.getForEntity(V4_BASE_URL + "backup-managers/DEFAULT/periodic-schedules/" + responseEntity.getBody().getEventId(), PeriodicEventRequestOrResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity2.getStatusCode());
    }

    @Test
    public void getScheduler_v4RestCallWithNonExistingBackupManagerId_notFoundResponse() {
        final ResponseEntity<SchedulerResponse> responseEntity = restTemplate.getForEntity(V4_BASE_URL + "backup-managers/123/configuration/scheduling",
                SchedulerResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void getPeriodicEvents_v4RestCallWithNonExistingBackupManagerId_notFoundResponse() {
        final ResponseEntity<V4PeriodicSchedulesResponse> responseEntity = restTemplate.getForEntity(V4_BASE_URL + "backup-managers/123/scheduler/periodic-events",
                V4PeriodicSchedulesResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void getPeriodicEvents_v4RestCallWithPersistedBackupManagerId_returnsPeriodicEventList() {
        final ResponseEntity<V4PeriodicSchedulesResponse> responseEntity = restTemplate.getForEntity(V4_BASE_URL +
                        "backup-managers/backupManagerWithBackupToDelete/periodic-schedules",
                V4PeriodicSchedulesResponse.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(Objects.requireNonNull(responseEntity.getBody()).getSchedules().size() > 0);
    }

    @Test
    public void getPeriodicEvent_v4RestCallWithNonExistingEventId_notFoundResponse() {
        final ResponseEntity<PeriodicEventRequestOrResponse> responseEntity = restTemplate.getForEntity(V4_BASE_URL + "backup-managers/DEFAULT/periodic-schedules/123",
                PeriodicEventRequestOrResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void postCalenderEvent_empty_BadRequest() {
        final V4CalendarEventRequest request = new V4CalendarEventRequest();
        HttpEntity<V4CalendarEventRequest> entity = new HttpEntity<>(request);
        final ResponseEntity<ErrorResponse> responseEntity = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void postCalenderEvent_request_withBothDayOfWeekAndDayOfMonth_BadRequest() {
        final V4CalendarEventRequest request = new V4CalendarEventRequest();
        request.setDayOfMonth(20);
        request.setDayOfWeek(DayOfWeek.FRIDAY);
        HttpEntity<V4CalendarEventRequest> entity = new HttpEntity<>(request);
        final ResponseEntity<ErrorResponse> responseEntity = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void postCalenderEvent_request_withBothDayOfWeekOccurrenceAndDayOfMonth_BadRequest() {
        final V4CalendarEventRequest request = new V4CalendarEventRequest();
        request.setDayOfMonth(20);
        request.setDayOfWeekOccurrence(DayOfWeekOccurrence.FIRST);
        HttpEntity<V4CalendarEventRequest> entity = new HttpEntity<>(request);
        final ResponseEntity<ErrorResponse> responseEntity = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void postCalenderEvent_request_createValidEventAndPatchInvalid_rejectEvent() {
        V4CalendarEventRequest request = new V4CalendarEventRequest();
        // valid time
        request.setTime("19:10:00");
        request.setStartTime("2022-05-30T08:00:00Z");
        HttpEntity<V4CalendarEventRequest> entity = new HttpEntity<>(request);
        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                HttpMethod.POST,
                entity,
                CreateEventResponse.class
        );
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        String eventId = responseEntity.getBody().getEventId();

        request.setTime("19:10:00");
        // Update with invalid start time
        request.setStartTime("2022-05-32T08:00:00Z");
        HttpEntity<V4CalendarEventRequest> entity1 = new HttpEntity<>(request);
        final ResponseEntity<ErrorResponse> responseEntity1 = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.PATCH,
                entity1,
                ErrorResponse.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity1.getStatusCode());

        // Get events and test
        final ResponseEntity<V4CalendarEventsResponse> responseEntity2 = restTemplate.getForEntity(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                V4CalendarEventsResponse.class
        );
        // start time remains same
        assertEquals("2022-05-30T08:00:00Z", responseEntity2.getBody().getSchedules().get(0).getStartTime());
    }

    @Test
    public void postCalenderEvent_request_createValidEventAndPutInvalid_rejectEvent() {
        V4CalendarEventRequest request = new V4CalendarEventRequest();
        // valid time
        request.setTime("19:10:00");
        request.setStartTime("2022-05-30T08:00:00Z");
        HttpEntity<V4CalendarEventRequest> entity = new HttpEntity<>(request);
        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                HttpMethod.POST,
                entity,
                CreateEventResponse.class
        );
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        String eventId = responseEntity.getBody().getEventId();

        request.setTime("11:00:00");
        // Replace with invalid start time
        request.setStartTime("2022-05-32T08:00:00Z");
        HttpEntity<V4CalendarEventRequest> entity1 = new HttpEntity<>(request);
        final ResponseEntity<ErrorResponse> responseEntity1 = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.PUT,
                entity1,
                ErrorResponse.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity1.getStatusCode());

        // Delete calendar event
        ResponseEntity<Void> responseEntity2 = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
    }

    @Test
    public void patchCalendarEvent_valid() {
        V4CalendarEventRequest request = new V4CalendarEventRequest();
        request.setTime("09:20:00");
        request.setMonth(5);
        request.setDayOfMonth(25);
        // Post event
        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                HttpMethod.POST,
                new HttpEntity<>(request),
                CreateEventResponse.class
        );
        String eventId = responseEntity.getBody().getEventId();
        // Patch the event and test
        request = new V4CalendarEventRequest();
        request.setMonth(6);
        request.setDayOfMonth(26);
        request.setTime("10:20:00");
        request.setStartTime("2022-05-22T08:00:00Z");
        request.setStopTime("3022-05-22T08:00:00Z");


        ResponseEntity<String> calendarEventResponseEntity  = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                String.class);
        String jsonResponse = calendarEventResponseEntity.getBody();
        Optional<CalendarEvent> calendar = jsonService.parseJsonString(jsonResponse , CalendarEvent.class);
        CalendarEvent calendarEvent = calendar.get();
        assertFalse("dayOfWeek should not be present in the JSON response " + jsonResponse, jsonResponse.contains("dayOfWeek"));
        assertFalse("dayOfWeekOccurrence should not be present in the JSON response" + jsonResponse, jsonResponse.contains("dayOfWeekOccurrence"));
        assertEquals(HttpStatus.OK, calendarEventResponseEntity.getStatusCode());
        assertEquals(26, calendarEvent.getDayOfMonth());
        assertEquals("10:20:00", calendarEvent.getTime());
        assertEquals("2022-05-22T08:00:00Z", calendarEvent.getStartTime());
        assertEquals("3022-05-22T08:00:00Z", calendarEvent.getStopTime());

        // Delete the event
        ResponseEntity<Void> responseEntity3 = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
    }

    @Test
    public void patchCalendarEvent_startTimeIsBlank_valid() {
        V4CalendarEventRequest request = new V4CalendarEventRequest();
        request.setTime("09:20:00");
        request.setMonth(5);
        request.setDayOfWeek(DayOfWeek.FRIDAY);
        // Post event
        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                HttpMethod.POST,
                new HttpEntity<>(request),
                CreateEventResponse.class
        );
        String eventId = responseEntity.getBody().getEventId();
        // Patch the event and test
        request = new V4CalendarEventRequest();
        request.setStartTime("");
        request.setStopTime("");
        request.setTime("09:20:00");
        request.setMonth(5);
        request.setDayOfWeekOccurrence(DayOfWeekOccurrence.ALL);
        request.setDayOfWeek(DayOfWeek.ALL);

        ResponseEntity<CalendarEvent> calendarEventResponseEntity  = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                CalendarEvent.class);

        assertEquals(HttpStatus.OK, calendarEventResponseEntity.getStatusCode());
        assertEquals("09:20:00", calendarEventResponseEntity.getBody().getTime());
        assertEquals(5, calendarEventResponseEntity.getBody().getMonth());
        assertEquals(DayOfWeekOccurrence.ALL, calendarEventResponseEntity.getBody().getDayOfWeekOccurrence());
        assertEquals(DayOfWeek.ALL, calendarEventResponseEntity.getBody().getDayOfWeek());

        // Delete the event
        ResponseEntity<Void> responseEntity3 = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
    }

    @Test
    public void patchCalendarEvent_dayOfWeek_dayOfMonth_conflict_invalid() {
        V4CalendarEventRequest request = new V4CalendarEventRequest();
        request.setTime("09:20:00");
        request.setDayOfWeek(DayOfWeek.FRIDAY);
        // Post event
        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                HttpMethod.POST,
                new HttpEntity<>(request),
                CreateEventResponse.class
        );
        String eventId = responseEntity.getBody().getEventId();
        // Patch the event and test
        request = new V4CalendarEventRequest();
        request.setStartTime("");
        request.setStopTime("");
        request.setTime("09:20:00");
        request.setDayOfMonth(22);

        ResponseEntity<CalendarEvent> calendarEventResponseEntity  = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                CalendarEvent.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, calendarEventResponseEntity.getStatusCode());

        // Delete the event
        ResponseEntity<Void> responseEntity2 = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
    }

    @Test
    public void patchCalendarEvent_dayOfWeekOccurrence_dayOfMonth_conflict_invalid() {
        V4CalendarEventRequest request = new V4CalendarEventRequest();
        request.setTime("09:20:00");
        request.setDayOfWeekOccurrence(DayOfWeekOccurrence.FIRST);
        // Post event
        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                HttpMethod.POST,
                new HttpEntity<>(request),
                CreateEventResponse.class
        );
        String eventId = responseEntity.getBody().getEventId();
        // Patch the event and test
        request = new V4CalendarEventRequest();
        request.setStartTime("");
        request.setStopTime("");
        request.setTime("09:20:00");
        request.setDayOfMonth(22);

        ResponseEntity<CalendarEvent> calendarEventResponseEntity  = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                CalendarEvent.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, calendarEventResponseEntity.getStatusCode());

        // Delete the event
        ResponseEntity<Void> responseEntity2 = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
    }

    @Test
    public void patchCalendarEvent_month_dayOfMonth_invalid() {
        V4CalendarEventRequest request = new V4CalendarEventRequest();
        request.setTime("09:20:00");
        request.setMonth(3);
        request.setDayOfMonth(12);
        // Post event
        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                HttpMethod.POST,
                new HttpEntity<>(request),
                CreateEventResponse.class
        );
        String eventId = responseEntity.getBody().getEventId();
        // Patch the event and test
        request = new V4CalendarEventRequest();
        request.setMonth(13);
        request.setDayOfMonth(32);

        ResponseEntity<CalendarEvent> calendarEventResponseEntity  = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                CalendarEvent.class);

        assertEquals(HttpStatus.BAD_REQUEST, calendarEventResponseEntity.getStatusCode());

        // Delete the event
        ResponseEntity<Void> responseEntity2 = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
    }

    @Test
    public void patchCalendarEvent_time_invalid() {
        V4CalendarEventRequest request = new V4CalendarEventRequest();
        request.setTime("09:20:00");
        request.setMonth(3);
        request.setDayOfMonth(12);
        // Post event
        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                HttpMethod.POST,
                new HttpEntity<>(request),
                CreateEventResponse.class
        );
        String eventId = responseEntity.getBody().getEventId();
        // Patch the event and test
        request = new V4CalendarEventRequest();
        request.setTime("09:61:00");

        ResponseEntity<CalendarEvent> calendarEventResponseEntity  = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                CalendarEvent.class);

        assertEquals(HttpStatus.BAD_REQUEST, calendarEventResponseEntity.getStatusCode());

        // Delete the event
        ResponseEntity<Void> responseEntity2 = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
    }

    @Test
    public void deleteCalendarEvent_with_invalid_eventId() {
        V4CalendarEventRequest request = new V4CalendarEventRequest();
        request.setTime("09:20:00");
        request.setMonth(3);
        request.setDayOfMonth(12);
        // Post event
        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString(),
                HttpMethod.POST,
                new HttpEntity<>(request),
                CreateEventResponse.class
        );
        String eventId = responseEntity.getBody().getEventId();

        // Delete calendar event using an invalid id and test
        final String invalidEventId = "8888888888";
        ResponseEntity<Void> responseEntity2 = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + invalidEventId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
        assertEquals(HttpStatus.NOT_FOUND, responseEntity2.getStatusCode());

        // Delete first calendar event using a valid id
        ResponseEntity<Void> responseEntity3 = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
    }

    @Test
    public void putCalendarEvent_NoTime_Invalid() {
        V4CalendarEventRequest request = new V4CalendarEventRequest();
        request.setTime("10:20:00");
        request.setMonth(5);
        request.setDayOfMonth(25);
        // Post event
        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                HttpMethod.POST,
                new HttpEntity<>(request),
                CreateEventResponse.class
        );
        String eventId = responseEntity.getBody().getEventId();
        // Patch the event and test
        request = new V4CalendarEventRequest();
        request.setMonth(3);
        request.setDayOfMonth(26);

        ResponseEntity<CalendarEvent> calendarEventResponseEntity  = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.PUT,
                new HttpEntity<>(request),
                CalendarEvent.class);
        assertEquals(HttpStatus.BAD_REQUEST, calendarEventResponseEntity.getStatusCode());

        // Get events and check if the old event exists
        ResponseEntity<CalendarEvent> ResponseEntity2  = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.GET,
                new HttpEntity<>(new V4CalendarEventRequest()),
                CalendarEvent.class);
        assertEquals(eventId, ResponseEntity2.getBody().getEventId());

        // Delete the event
        ResponseEntity<Void> responseEntity3 = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
    }

    @Test
    public void postCalenderEvent_GetIt_DeleteIt() {
        final V4CalendarEventRequest request = new V4CalendarEventRequest();
        request.setTime("09:20:00");
        request.setMonth(5);
        request.setDayOfMonth(25);
        // Post event and test
        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                HttpMethod.POST,
                new HttpEntity<>(request),
                CreateEventResponse.class
        );
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        String eventId = responseEntity.getBody().getEventId();
        // Get events and test
        final ResponseEntity<V4CalendarEventsResponse> responseEntity1 = restTemplate.getForEntity(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                V4CalendarEventsResponse.class
        );
        assertEquals(HttpStatus.OK, responseEntity1.getStatusCode());

        // Get a event and test
        final ResponseEntity<CalendarEvent> responseEntity2 = restTemplate.getForEntity(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                CalendarEvent.class
        );
        assertEquals(HttpStatus.OK, responseEntity2.getStatusCode());
        assertEquals(eventId, responseEntity2.getBody().getEventId());

        // Delete the event and test
        ResponseEntity<Void> responseEntity3 = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
        assertEquals(HttpStatus.NO_CONTENT, responseEntity3.getStatusCode());
    }

    @Test
    public void putCalendarEvent_Valid() {
        V4CalendarEventRequest request = new V4CalendarEventRequest();
        request.setTime("10:20:00");
        request.setMonth(5);
        request.setDayOfMonth(25);
        // Post event
        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/DEFAULT/calendar-schedules",
                HttpMethod.POST,
                new HttpEntity<>(request),
                CreateEventResponse.class
        );
        String eventId = responseEntity.getBody().getEventId();
        // Put the event and test
        request = new V4CalendarEventRequest();
        request.setTime("11:20:00");
        request.setMonth(6);
        request.setDayOfWeek(DayOfWeek.FRIDAY);
        request.setDayOfWeekOccurrence(DayOfWeekOccurrence.FIRST);


        ResponseEntity<CalendarEvent> calendarEventResponseEntity  = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.PUT,
                new HttpEntity<>(request),
                CalendarEvent.class);

        assertEquals(HttpStatus.OK, calendarEventResponseEntity.getStatusCode());
        assertEquals("11:20:00", calendarEventResponseEntity.getBody().getTime());
        assertEquals(6, calendarEventResponseEntity.getBody().getMonth());
        assertEquals(0, calendarEventResponseEntity.getBody().getDayOfMonth());
        // Delete the event
        ResponseEntity<Void> responseEntity3 = restTemplate.exchange(
                V4_SCHEDULER_CALENDAR.toString() + eventId,
                HttpMethod.DELETE,
                null,
                Void.class
        );
    }

    @Test
    public void getSingleEvents_notImplemented() {
        final ResponseEntity<EventResponse> responseEntity = restTemplate.getForEntity(V4_BASE_URL +
                        "backup-managers/brm-id/single-schedules",
                EventResponse.class);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }

    @Test
    public void postSingleEvent_notImplemented() {
        CreateEventRequest request = new CreateEventRequest();
        request.setDayOfMonth(23);
        request.setDayOfWeek("Wednesday");
        request.setScheduledTime("2022-05-22T08:00:00Z");
        request.setEndTime("3022-05-22T08:00:00Z");
        final ResponseEntity<CreateEventResponse> responseEntity = restTemplate.postForEntity(V4_BASE_URL +
               "backup-managers/brm-id/single-schedules", request, CreateEventResponse.class);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }

    @Test
    public void getSingleEvent_notImplemented() {
        final ResponseEntity<EventResponse> responseEntity = restTemplate.getForEntity(V4_BASE_URL +
                        "backup-managers/brm-id/single-schedules" + "/schedule-id",
                EventResponse.class);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }

    @Test
    public void putSingleEvent_notImplemented() {
        CreateEventRequest request = new CreateEventRequest();
        request.setDayOfMonth(23);
        request.setDayOfWeek("Wednesday");
        request.setScheduledTime("2022-05-22T08:00:00Z");
        request.setEndTime("3022-05-22T08:00:00Z");
        ResponseEntity<EventResponse> responseEntity  = restTemplate.exchange(
                V4_BASE_URL + "backup-managers/brm-id/single-schedules" + "/schedule-id",
                HttpMethod.PUT,
                new HttpEntity<>(request),
                EventResponse.class);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }

    @Test
    public void deleteSingleEvent_notImplemented() {
        ResponseEntity<Void> responseEntity = restTemplate.exchange( V4_BASE_URL + "backup-managers/brm-id/single-schedules" + "/schedule-id", HttpMethod.DELETE, null, Void.class);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
    }

    private ResponseEntity<CreateEventResponse> createPeriodicEvent(final String backupManagerId, final int hours, final String startTime) {
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setHours(hours);
        request.setMinutes(2);
        request.setDays(3);
        request.setWeeks(4);
        request.setStartTime(startTime);
        request.setStopTime("9018-06-28T11:45:56Z");

        return restTemplate.postForEntity(V4_BASE_URL + "backup-managers/" + backupManagerId + "/periodic-schedules", request, CreateEventResponse.class);
    }
}
