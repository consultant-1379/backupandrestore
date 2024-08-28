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
public class V4PeriodicSchedulesResponse {

    private List<PeriodicEventRequestOrResponse> schedules = new ArrayList<>();

    /**
     * Default constructor, to be used by Jackson.
     */
    public V4PeriodicSchedulesResponse() {}

    /**
     * Creates V4PeriodicSchedulesResponse from list of PeriodicEvent.
     * @param schedules to be returned
     */
    public V4PeriodicSchedulesResponse(final List<PeriodicEvent> schedules) {
        for (final PeriodicEvent schedule : schedules) {
            this.schedules.add(schedule.toResponse());
        }
    }

    public List<PeriodicEventRequestOrResponse> getSchedules() {
        return schedules;
    }

    public void setSchedules(final List<PeriodicEventRequestOrResponse> schedules) {
        this.schedules = schedules;
    }

}
