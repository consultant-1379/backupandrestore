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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;

import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.PeriodicEventRequestOrResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents Periodic Event in BRM model
 */
@JsonInclude(Include.NON_NULL)
public class BRMPeriodicEventJson extends PeriodicEventRequestOrResponse {

    /**
     * Used by Jackson
     */
    public BRMPeriodicEventJson() {
    }

    /**
     * @param periodicEvent
     *            periodicEvent
     */
    public BRMPeriodicEventJson(final PeriodicEvent periodicEvent) {
        eventId = periodicEvent.getEventId();
        hours = periodicEvent.getHours();
        minutes = periodicEvent.getMinutes();
        days = periodicEvent.getDays();
        weeks = periodicEvent.getWeeks();
        startTime = periodicEvent.getStartTime();
        stopTime = periodicEvent.getStopTime();
    }

    @Override
    @JsonProperty("start-time")
    public String getStartTime() {
        return super.getStartTime();
    }

    @Override
    @JsonProperty("stop-time")
    public String getStopTime() {
        return super.getStopTime();
    }

    @Override
    public String toString() {
        return "BRMPeriodicEventJson [id :" + eventId + ", days: " + days + ", hours: " + hours + ", minutes: " + minutes
                + ", weeks: " + weeks + ", startTime: " + startTime + ", stopTime: " + stopTime + "]";
    }

}
