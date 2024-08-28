/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler;

import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
/**
 * Represent a base class of Scheduled Event.
 * Currently it's a base class of CalendarEvent and PeriodicEvent
 */
public abstract class ScheduledEvent {

    protected String startTime;
    protected String stopTime;

    @JsonIgnore
    protected OffsetDateTime nextRun;

    @JsonIgnore
    protected final JsonService jsonService = new JsonService();

    @JsonProperty("id")
    private String eventId;

    @JsonIgnore
    private String backupManagerId;

    public String getBackupManagerId() {
        return backupManagerId;
    }

    public void setBackupManagerId(final String backupManagerId) {
        this.backupManagerId = backupManagerId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(final String startTime) {
        this.startTime = startTime;
    }

    public String getStopTime() {
        return stopTime;
    }

    public void setStopTime(final String stopTime) {
        this.stopTime = stopTime;
    }

    /**
     * Converts the Scheduled Event object to string
     * This methods removes the timezone info in the startTime and stopTime JSON fields
     * The timezone information is stored in a separate block object.
     * This block is then appended to the Scheduled Event's JSON object.
     * @return the JSON string to be stored in its json file
     */
    @JsonIgnore
    public abstract String toJson();

    /**
     * Convert the PeriodicEvent object to a JSON String
     * This methods removes the timezone info in the startTime and stopTime JSON fields
     * The timezone information is stored in a separate JSON block,
     * This block is then appended to the Periodic Event's JSON object.
     * @param eventCopy the deep copy of the scheduled event
     * @return the JSON string representation of the ScheduledEvent object
     */
    protected String toJson(final ScheduledEvent eventCopy) {
        final String startOffsetDateTime = eventCopy.getStartTime();
        String offset = DateTimeUtils.parseToOffsetDateTime(startOffsetDateTime).getOffset().toString();
        eventCopy.setStartTime(DateTimeUtils.getLocalDateTime(startOffsetDateTime));

        final String stopOffsetDateTime = eventCopy.getStopTime();
        if (stopOffsetDateTime != null) {
            offset = offset + "," + DateTimeUtils.parseToOffsetDateTime(stopOffsetDateTime).getOffset().toString();
            eventCopy.setStopTime(DateTimeUtils.getLocalDateTime(stopOffsetDateTime));
        }

        final String timezone = String.format("{\"timezone\":\"%s\"}", offset);
        return JsonService.toJsonString(eventCopy).concat(timezone);
    }

    public OffsetDateTime getNextRun() {
        return nextRun;
    }

    public void setNextRun(final OffsetDateTime nextRun) {
        this.nextRun = nextRun;
    }
}
