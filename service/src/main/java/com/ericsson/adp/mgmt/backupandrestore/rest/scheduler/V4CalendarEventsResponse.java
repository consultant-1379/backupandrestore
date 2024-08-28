/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.scheduler;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.CalendarEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The response of GET calendar Events
 */
public class V4CalendarEventsResponse {
    private List<CalendarEvent> schedules = new ArrayList<>();

    /**
     * Default Constructor which is used when deserializing it.
     */
    public V4CalendarEventsResponse() {
    }

    /**
     * The constructor of the Events Response
     * @param schedules a list of calendar events
     */
    public V4CalendarEventsResponse(final List<CalendarEvent> schedules) {
        this.schedules = schedules;
    }

    public List<CalendarEvent> getSchedules() {
        return schedules;
    }

    public void setSchedules(final List<CalendarEvent> schedules) {
        this.schedules = schedules;
    }
}
