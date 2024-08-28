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

import com.ericsson.adp.mgmt.backupandrestore.exception.NotImplementedException;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.CreateEventRequest;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.CreateEventResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.EventResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.EventsResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.SchedulerRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responsible for scheduler endpoints.
 */
@RequestMapping(value = {"v1", "v3"})
@RestController
public class SchedulerController extends BaseController {

    /**
     * Responsible for updating the scheduler.
     * @param backupManagerId which backupManager is affected.
     * @param request what to update.
     */
    @PostMapping("backup-manager/{backupManagerId}/scheduler")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void updateScheduler(@PathVariable("backupManagerId") final String backupManagerId,
                                @RequestBody final SchedulerRequest request) {
        throw new NotImplementedException();
    }

    /**
     * Gets all scheduled events for a backupManager
     * @param backupManagerId which backupManager to look for.
     * @return backupManager's scheduled events.
     */
    @GetMapping("backup-manager/{backupManagerId}/scheduler/event")
    public EventsResponse getEvents(@PathVariable("backupManagerId") final String backupManagerId) {
        throw new NotImplementedException();
    }

    /**
     * Creates a scheduled event.
     * @param backupManagerId which backupManager to have a backup scheduled.
     * @param request event's information.
     * @return id of new event.
     */
    @PostMapping("backup-manager/{backupManagerId}/scheduler/event")
    public CreateEventResponse createEvent(@PathVariable("backupManagerId") final String backupManagerId,
                                           @RequestBody final CreateEventRequest request) {
        throw new NotImplementedException();
    }

    /**
     * Gets specific scheduled event.
     * @param backupManagerId which backupManager to look for.
     * @param eventId which event to look for.
     * @return event information.
     */
    @GetMapping("backup-manager/{backupManagerId}/scheduler/event/{eventId}")
    public EventResponse getEvent(@PathVariable("backupManagerId") final String backupManagerId,
                                  @PathVariable("eventId") final String eventId) {
        throw new NotImplementedException();
    }

    /**
     * Removes a scheduled event.
     * @param backupManagerId which backupManager to look for.
     * @param eventId which event to delete.
     */
    @DeleteMapping("backup-manager/{backupManagerId}/scheduler/event/{eventId}")
    public void deleteEvent(@PathVariable("backupManagerId") final String backupManagerId,
                            @PathVariable("eventId") final String eventId) {
        throw new NotImplementedException();
    }

}
