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

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.ScheduledEventHandler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.CalendarEvent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import com.ericsson.adp.mgmt.backupandrestore.exception.NotImplementedException;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.V4SchedulerResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.V4PeriodicSchedulesResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.EventResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.PeriodicEventRequestOrResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.CreateEventResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.SchedulerRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.V4CalendarEventRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.V4CalendarEventsResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.CreateEventRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Responsible for V4 scheduler endpoints.
 */
@RestController
public class V4SchedulerController extends V4Controller {

    private static final String SCHEDULER_BASE_URL = "backup-managers/{brm-id}/configuration/scheduling";
    private static final String SCHEDULER_PERIODIC_URL = "backup-managers/{brm-id}/periodic-schedules";
    private static final String SCHEDULER_CALENDAR_URL = "backup-managers/{brm-id}/calendar-schedules";
    private static final String SCHEDULER_SINGLE_URL = "backup-managers/{brm-id}/single-schedules";
    @Autowired
    private ScheduledEventHandler eventHandler;

    /**
     * Gets the {@link Scheduler} for the given {@link com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager}
     *
     * @param backupManagerId - the id of the BackupManager
     * @return a {@link V4SchedulerResponse} with the information about the given BackupManager's Scheduler.
     */
    @GetMapping(SCHEDULER_BASE_URL)
    public V4SchedulerResponse getScheduler(@PathVariable("brm-id") final String backupManagerId) {
        return new V4SchedulerResponse(getBRMScheduler(backupManagerId));
    }

    /**
     * Returns periodic events in a backup manager
     *
     * @param backupManagerId - the id of the BackupManager
     * @return V4PeriodicSchedulesResponse - the list of periodic schedules
     */
    @GetMapping(SCHEDULER_PERIODIC_URL)
    public V4PeriodicSchedulesResponse getPeriodicEvents(@PathVariable("brm-id") final String backupManagerId) {
        return new V4PeriodicSchedulesResponse(getBRMScheduler(backupManagerId).getPeriodicEvents());
    }

    /**
     * Returns a specific periodic event in a backup manager
     *
     * @param backupManagerId - the id of the BackupManager
     * @param eventId         - the id of the event to get.
     * @return - A {@link EventResponse}
     */
    @GetMapping(SCHEDULER_PERIODIC_URL + "/{eventId}")
    public PeriodicEventRequestOrResponse getPeriodicEvent(@PathVariable("brm-id") final String backupManagerId,
                                                           @PathVariable("eventId") final String eventId) {
        return getBRMScheduler(backupManagerId).getPeriodicEvent(eventId).toResponse();
    }

    /**
     * Creates a {@link com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent} for the given {@link
     * com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager}
     *
     * @param backupManagerId - the id of the BackupManager
     * @param eventRequest - a {@link PeriodicEventRequestOrResponse} instance
     * @return - A {@link CreateEventResponse}
     */
    @PostMapping(SCHEDULER_PERIODIC_URL)
    @ResponseStatus(HttpStatus.CREATED)
    public CreateEventResponse postPeriodicEvent(@PathVariable("brm-id") final String backupManagerId,
                                            @Valid @RequestBody final PeriodicEventRequestOrResponse eventRequest) {
        if (eventRequest.getHours() == null) {
            eventRequest.setHours(0);
        }
        final String eventId = eventHandler.createPeriodicEvent((getBackupManager(backupManagerId)), eventRequest, true);
        return new CreateEventResponse(eventId);
    }

    /**
     * Patches the given periodic schedule, allowing for partial updates.
     *
     * @param backupManagerId - the id of the BackupManager
     * @param scheduleId - scheduleId of existing event
     * @param eventRequest - a {@link PeriodicEventRequestOrResponse} instance
     * @return - A {@link PeriodicEventRequestOrResponse}
     */
    @PatchMapping(SCHEDULER_PERIODIC_URL + "/{schedule-id}")
    public PeriodicEventRequestOrResponse patchPeriodicEvent(@PathVariable("brm-id") final String backupManagerId,
                                                           @PathVariable("schedule-id") final String scheduleId,
                                                           @RequestBody final PeriodicEventRequestOrResponse eventRequest) {
        eventHandler.updatePeriodicEvent(getBackupManager(backupManagerId), scheduleId, eventRequest);
        return getBRMScheduler(backupManagerId).getPeriodicEvent(scheduleId).toResponse();
    }

    /**
     * Replaces a periodic event, and re-schedules it based on the new values
     * if a field is not provided in request body it will be reset to its default value.
     *
     * @param backupManagerId - the id of the BackupManager
     * @param scheduleId - scheduleId of existing event
     * @param eventRequest - a {@link PeriodicEventRequestOrResponse} instance
     * @return - A {@link PeriodicEventRequestOrResponse}
     */
    @PutMapping(SCHEDULER_PERIODIC_URL + "/{schedule-id}")
    public ResponseEntity putPeriodicEvent(@PathVariable("brm-id") final String backupManagerId,
                                   @PathVariable("schedule-id") final String scheduleId,
                                   @Valid @RequestBody final PeriodicEventRequestOrResponse eventRequest) {
        final PeriodicEvent putRequest = getBRMScheduler(backupManagerId).getPeriodicEvent(scheduleId);
        if (eventRequest.getStartTime() == null) {
            eventRequest.setStartTime(OffsetDateTime.now(ZoneId.systemDefault()).toString());
        }
        if (eventRequest.getStopTime() == null) {
            eventRequest.setStopTime("");
        }

        putRequest.mergeWith(eventRequest);
        eventHandler.updatePeriodicEvent(getBackupManager(backupManagerId), scheduleId, putRequest.toResponse());
        return ResponseEntity.ok(getBRMScheduler(backupManagerId).getPeriodicEvent(scheduleId).toResponse());
    }

    /**
     * Deletes one periodic schedule of a backupManager.
     *
     * @param backupManagerId - the id of the BackupManager
     * @param scheduleId - the id of the event to get.
     */
    @DeleteMapping(SCHEDULER_PERIODIC_URL + "/{schedule-id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePeriodicEvent(@PathVariable("brm-id") final String backupManagerId,
                                    @PathVariable("schedule-id") final String scheduleId) {
        eventHandler.deletePeriodicEvent(getBackupManager(backupManagerId), getBRMScheduler(backupManagerId).getPeriodicEvent(scheduleId), false);
    }

    /**
     * Put scheduling information for backupManager.
     * @param backupManagerId of backupManager to be updated.
     * @param request what to update.
     */
    @PutMapping(SCHEDULER_BASE_URL)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void putScheduling(
            @PathVariable("brm-id") final String backupManagerId,
            @Valid @RequestBody final SchedulerRequest request) {
        patchScheduling(backupManagerId, request);
    }

    /**
     * Patch scheduling information for backupManager.
     * @param backupManagerId of backupManager to be updated.
     * @param request what to update.
     */
    @PatchMapping(SCHEDULER_BASE_URL)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void patchScheduling(
            @PathVariable("brm-id") final String backupManagerId,
            @RequestBody final SchedulerRequest request) {
        final Scheduler target = getBackupManager(backupManagerId).getScheduler();
        request.partialUpdate(target);
        target.persist();
    }

    /**
     * A List of calendar based scheduler events
     *
     * @param backupManagerId - the id of the BackupManager
     * @return - A {@link V4CalendarEventsResponse}
     */
    @GetMapping(SCHEDULER_CALENDAR_URL)
    public V4CalendarEventsResponse getCalendarEvents(@PathVariable("brm-id") final String backupManagerId) {
        return new V4CalendarEventsResponse((getBRMScheduler(backupManagerId).getCalendarEvents()));
    }

    /**
     * Create A calendar based scheduler event for the given backup manager
     *
     * @param backupManagerId - the id of the BackupManager
     * @param request the request body
     * @return - A {@link CreateEventResponse}
     */
    @PostMapping(SCHEDULER_CALENDAR_URL)
    @ResponseStatus(value = HttpStatus.CREATED)
    public CreateEventResponse postCalenderEvents(@PathVariable("brm-id") final String backupManagerId,
                                                  @RequestBody final V4CalendarEventRequest request) {
        return new CreateEventResponse(eventHandler.createCalendarEvent((getBackupManager(backupManagerId)), request));
    }

    /**
     * Get a specific periodic event in a backup manager
     *
     * @param backupManagerId - the id of the BackupManager
     * @param scheduleId - the id of the event to get.
     * @return - A {@link CalendarEvent}
     */
    @GetMapping(SCHEDULER_CALENDAR_URL + "/{schedule-id}")
    public CalendarEvent getCalendarEvent(@PathVariable("brm-id") final String backupManagerId,
                                          @PathVariable("schedule-id") final String scheduleId) {
        return getBRMScheduler(backupManagerId).getCalendarEvent(scheduleId);
    }

    /**
     * Patches the given calendar schedule, allowing for partial updates.
     *
     * @param backupManagerId - the id of the BackupManager
     * @param scheduleId - the id of the event to get.
     * @param request - the request body to update the given scheduler
     * @return - A {@link EventResponse}
     */
    @PatchMapping(SCHEDULER_CALENDAR_URL + "/{schedule-id}")
    public CalendarEvent patchCalendarEvent(@PathVariable("brm-id") final String backupManagerId,
                                            @PathVariable("schedule-id") final String scheduleId, @RequestBody final V4CalendarEventRequest request) {

        return eventHandler.updateCalendarEvent(getBackupManager(backupManagerId), request, scheduleId);
    }

    /**
     * Replaces the given calendar scheduler.
     *
     * @param backupManagerId - the id of the BackupManager
     * @param scheduleId - the id of the event to get.
     * @param request - the request body to replace the given scheduler
     * @return - A {@link EventResponse}
     */
    @PutMapping(SCHEDULER_CALENDAR_URL + "/{schedule-id}")
    public CalendarEvent putCalendarEvent(@PathVariable("brm-id") final String backupManagerId,
                                          @PathVariable("schedule-id") final String scheduleId, @RequestBody final V4CalendarEventRequest request) {
        return eventHandler.replaceCalendarEvent(getBackupManager(backupManagerId),
                getCalendarEvent(backupManagerId, scheduleId), scheduleId, request);
    }

    /**
     * Delete a specific calendar event
     *
     * @param backupManagerId - the id of the BackupManager
     * @param scheduleId - the id of the event to get.
     */
    @DeleteMapping(SCHEDULER_CALENDAR_URL + "/{schedule-id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCalendarEvent(@PathVariable("brm-id") final String backupManagerId, @PathVariable("schedule-id") final String scheduleId) {
        eventHandler.deleteCalendarEvent(getBackupManager(backupManagerId),
                getCalendarEvent(backupManagerId, scheduleId));
    }

    /**
     * Skeleton method, not implemented.
     *
     * @param backupManagerId - the id of the BackupManager
     * @return - A {@link EventResponse}
     */
    @GetMapping(SCHEDULER_SINGLE_URL)
    public EventResponse getSingleEvents(@PathVariable("brm-id") final String backupManagerId) {
        throw new NotImplementedException();
    }

    /**
     * Skeleton method, not implemented.
     *
     * @param backupManagerId - the id of the BackupManager
     * @param eventRequest - A {@link CreateEventRequest}
     * @return - A {@link CreateEventResponse}
     */
    @PostMapping(SCHEDULER_SINGLE_URL)
    public CreateEventResponse postSingleEvent(@PathVariable("brm-id") final String backupManagerId,
                                               @RequestBody final CreateEventRequest eventRequest) {
        throw new NotImplementedException();
    }

    /**
     * Skeleton method, not implemented.
     *
     * @param backupManagerId - the id of the BackupManager
     * @param scheduleId - a schedule id
     * @return - A {@link EventResponse}
     */
    @GetMapping(SCHEDULER_SINGLE_URL + "/{schedule-id}")
    public EventResponse getSingleEvent(@PathVariable("brm-id") final String backupManagerId,
                                        @PathVariable("schedule-id") final String scheduleId) {
        throw new NotImplementedException();
    }

    /**
     * Skeleton method, not implemented.
     *
     * @param backupManagerId - the id of the BackupManager
     * @param scheduleId - a schedule id
     * @param eventRequest - A {@link CreateEventRequest}
     * @return - A {@link EventResponse}
     */
    @PutMapping(SCHEDULER_SINGLE_URL + "/{schedule-id}")
    public EventResponse putSingleEvent(@PathVariable("brm-id") final String backupManagerId,
                                        @PathVariable("schedule-id") final String scheduleId, @RequestBody final CreateEventRequest eventRequest) {
        throw new NotImplementedException();
    }

    /**
     * Skeleton method, not implemented.
     *
     * @param backupManagerId - the id of the BackupManager
     * @param scheduleId - an event id to delete
     */
    @DeleteMapping(SCHEDULER_SINGLE_URL + "/{schedule-id}")
    public void deleteSingleEvent(@PathVariable("brm-id") final String backupManagerId,
                                  @PathVariable("schedule-id") final String scheduleId) {
        throw new NotImplementedException();
    }

    /**
     * Private convenience method.
     *
     * @param backupManagerId - the id of a {@link com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager}
     * @return the {@link Scheduler} associated with the given backupManagerId
     */
    private Scheduler getBRMScheduler(final String backupManagerId) {
        return getBackupManager(backupManagerId).getScheduler();
    }
}
