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
package com.ericsson.adp.mgmt.backupandrestore.rest.scheduler;

import java.util.ArrayList;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * JSON response with all scheduled periodic events of a backupManager.
 */
@JsonInclude(Include.NON_NULL)
public class PeriodicEventsResponse {

    private List<PeriodicEventRequestOrResponse> events = new ArrayList<>();

    /**
     * Default constructor, to be used by Jackson.
     */
    public PeriodicEventsResponse() {}

    /**
     * Creates PeriodicEventResponse from list of PeriodicEvent.
     * @param events to be returned
     */
    public PeriodicEventsResponse(final List<PeriodicEvent> events) {
        for (final PeriodicEvent event : events) {
            this.events.add(event.toResponse());
        }
    }

    public List<PeriodicEventRequestOrResponse> getEvents() {
        return events;
    }

    public void setEvents(final List<PeriodicEventRequestOrResponse> events) {
        this.events = events;
    }

}
