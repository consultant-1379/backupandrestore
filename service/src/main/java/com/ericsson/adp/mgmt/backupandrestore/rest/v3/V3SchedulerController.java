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

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.ScheduledEventHandler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.exception.NotImplementedException;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.CreateEventRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.CreateEventResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.EventResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.PeriodicEventRequestOrResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.PeriodicEventsResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.SchedulerRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.SchedulerResponse;

/**
 * Responsible for V3 scheduler endpoints.
 */
@RestController
public class V3SchedulerController extends V3Controller {

    private static final String SCHEDULER_BASE_URL = "/backup-managers/{brm-id}/scheduler";
    private static final String SCHEDULER_PERIODIC_EVENTS_URL = SCHEDULER_BASE_URL + "/periodic-events";
    private static final String SCHEDULER_CALENDAR_EVENTS_URL = SCHEDULER_BASE_URL + "/calendar-events";
    private static final String SCHEDULER_SINGLE_EVENTS_URL = SCHEDULER_BASE_URL + "/single-events";

    @Autowired
    private ScheduledEventHandler eventHandler;

    /**
     * Private convenience method.
     *
     * @param backupManagerId - the id of a {@link com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager}
     * @return the {@link Scheduler} associated with the given backupManagerId
     */
    private Scheduler scheduler(final String backupManagerId) {
        return getBackupManager(backupManagerId).getScheduler();
    }

    /**
     * Gets the {@link Scheduler} for the given {@link com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager}
     *
     * @param backupManagerId - the id of the BackupManager
     * @return a {@link SchedulerResponse} with the information about the given BackupManager's Scheduler.
     */
    @GetMapping(SCHEDULER_BASE_URL)
    public SchedulerResponse getScheduler(@PathVariable("brm-id") final String backupManagerId) {
        return new SchedulerResponse(scheduler(backupManagerId));
    }

    /**
     * PUT scheduler configuration
     *
     * @param backupManagerId - the id of the BackupManager
     * @param update - partial or complete set of updated scheduler configuration values
     * @return - A {@link SchedulerResponse}
     */
    @PutMapping(SCHEDULER_BASE_URL + "/configuration")
    public SchedulerResponse putConfiguration(
                                              @PathVariable("brm-id") final String backupManagerId,
                                              @RequestBody final SchedulerRequest update) {
        final Scheduler target = getBackupManager(backupManagerId).getScheduler();
        update.partialUpdate(target);
        target.persist();
        return new SchedulerResponse(target);
    }

    /**
     * GET scheduler configuration
     *
     * @param backupManagerId - the id of the BackupManager
     * @return - A {@link SchedulerResponse}
     */
    @GetMapping(SCHEDULER_BASE_URL + "/configuration")
    public SchedulerResponse getConfiguration(@PathVariable("brm-id") final String backupManagerId) {
        return getScheduler(backupManagerId);
    }

    /**
     * Creates a {@link PeriodicEvent} for the given {@link
     * com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager}
     *
     * @param backupManagerId - the id of the BackupManager
     * @param eventRequest - a {@link PeriodicEventRequestOrResponse} instance
     * @return - A {@link CreateEventResponse}
     */
    @PostMapping(SCHEDULER_PERIODIC_EVENTS_URL)
    public CreateEventResponse postPeriodicEvent(@PathVariable("brm-id") final String backupManagerId,
                                                 @RequestBody final PeriodicEventRequestOrResponse eventRequest) {

        return new CreateEventResponse(eventHandler.createPeriodicEvent((getBackupManager(backupManagerId)), eventRequest, true));
    }

    /**
     * Returns periodic events in a backup manager
     *
     * @param backupManagerId - the id of the BackupManager
     * @return - {@link EventResponse}
     */
    @GetMapping(SCHEDULER_PERIODIC_EVENTS_URL)
    public PeriodicEventsResponse getPeriodicEvents(@PathVariable("brm-id") final String backupManagerId) {
        return new PeriodicEventsResponse(scheduler(backupManagerId).getPeriodicEvents());
    }

    /**
     * Returns a specific periodic event in a backup manager
     *
     * @param backupManagerId - the id of the BackupManager
     * @param eventId - the id of the event to get.
     * @return - A {@link EventResponse}
     */
    @GetMapping(SCHEDULER_PERIODIC_EVENTS_URL + "/{eventId}")
    public PeriodicEventRequestOrResponse getPeriodicEvent(@PathVariable("brm-id") final String backupManagerId,
                                                           @PathVariable("eventId") final String eventId) {
        return scheduler(backupManagerId).getPeriodicEvent(eventId).toResponse();
    }

    /**
     * DELETE periodic event
     *
     * @param backupManagerId - the id of the BackupManager
     * @param eventId - the id of the event to get.
     */
    @DeleteMapping(SCHEDULER_PERIODIC_EVENTS_URL + "/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePeriodicEvent(@PathVariable("brm-id") final String backupManagerId,
                                    @PathVariable("eventId") final String eventId) {
        eventHandler.deletePeriodicEvent(getBackupManager(backupManagerId),
                getBackupManager(backupManagerId).getScheduler().getPeriodicEvent(eventId), false);
    }

    /**
     * Updates a periodic event, and re-schedules it based on the new values
     *
     * @param backupManagerId - the EVENT_ID of the backup manager the periodic event belongs to
     * @param eventId - the EVENT_ID of the event itself
     * @param update - the values to be written into the event
     * @return the event post-update
     * */
    @PutMapping(SCHEDULER_PERIODIC_EVENTS_URL + "/{eventId}")
    public PeriodicEventRequestOrResponse updatePeriodicEvent(@PathVariable("brm-id") final String backupManagerId,
                                                              @PathVariable("eventId") final String eventId,
                                                              @RequestBody final PeriodicEventRequestOrResponse update) {
        eventHandler.updatePeriodicEvent(getBackupManager(backupManagerId), eventId, update);
        return scheduler(backupManagerId).getPeriodicEvent(eventId).toResponse();
    }

    /**
     * Skeleton method, not implemented.
     *
     * @param backupManagerId - the id of the BackupManager
     * @return - A {@link EventResponse}
     */
    @GetMapping(SCHEDULER_CALENDAR_EVENTS_URL)
    public EventResponse getCalendarEvents(@PathVariable("brm-id") final String backupManagerId) {
        throw new NotImplementedException();
    }

    /**
     * Skeleton method, not implemented.
     *
     * @param backupManagerId - the id of the BackupManager
     * @return - A {@link CreateEventResponse}
     */
    @PostMapping(SCHEDULER_CALENDAR_EVENTS_URL)
    public CreateEventResponse postCalenderEvents(@PathVariable("brm-id") final String backupManagerId) {
        throw new NotImplementedException();
    }

    /**
     * Skeleton method, not implemented.
     *
     * @param backupManagerId - the id of the BackupManager
     * @param eventId - the id of the event to get.
     * @return - A {@link EventResponse}
     */
    @GetMapping(SCHEDULER_CALENDAR_EVENTS_URL + "/{eventId}")
    public EventResponse getCalendarEvent(@PathVariable("brm-id") final String backupManagerId, @PathVariable("eventId") final String eventId) {
        throw new NotImplementedException();
    }

    /**
     * Skeleton method, not implemented.
     *
     * @param backupManagerId - the id of the BackupManager
     * @param eventId - an event id
     */
    @DeleteMapping(SCHEDULER_CALENDAR_EVENTS_URL + "/{eventId}")
    public void deleteCalendarEvent(@PathVariable("brm-id") final String backupManagerId, @PathVariable("eventId") final String eventId) {
        throw new NotImplementedException();
    }

    /**
     * Skeleton method, not implemented.
     *
     * @param backupManagerId - the id of the BackupManager
     * @return - A {@link EventResponse}
     */
    @GetMapping(SCHEDULER_SINGLE_EVENTS_URL)
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
    @PostMapping(SCHEDULER_SINGLE_EVENTS_URL)
    public CreateEventResponse postSingleEvent(@PathVariable("brm-id") final String backupManagerId,
                                               @RequestBody final CreateEventRequest eventRequest) {
        throw new NotImplementedException();
    }

    /**
     * Skeleton method, not implemented.
     *
     * @param backupManagerId - the id of the BackupManager
     * @param eventId - an event id
     * @return - A {@link EventResponse}
     */
    @GetMapping(SCHEDULER_SINGLE_EVENTS_URL + "/{eventId}")
    public EventResponse getSingleEvent(@PathVariable("brm-id") final String backupManagerId, @PathVariable("eventId") final String eventId) {
        throw new NotImplementedException();
    }

    /**
     * Skeleton method, not implemented.
     *
     * @param backupManagerId - the id of the BackupManager
     * @param eventId - an event id to delete
     */
    @DeleteMapping(SCHEDULER_SINGLE_EVENTS_URL + "/{eventId}")
    public void deleteSingleEvent(@PathVariable("brm-id") final String backupManagerId, @PathVariable("eventId") final String eventId) {
        throw new NotImplementedException();
    }
}
