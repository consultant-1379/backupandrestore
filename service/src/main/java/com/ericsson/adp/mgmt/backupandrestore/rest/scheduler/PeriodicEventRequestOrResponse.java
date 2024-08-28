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

import java.time.Duration;

import com.ericsson.adp.mgmt.backupandrestore.rest.v4.FirstOrder;
import com.ericsson.adp.mgmt.backupandrestore.rest.v4.SecondOrder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.hibernate.validator.constraints.ScriptAssert;
import jakarta.validation.GroupSequence;

/**
 * JSON request to create a scheduled periodic event
 */
@JsonInclude(Include.NON_NULL)
@GroupSequence({PeriodicEventRequestOrResponse.class, FirstOrder.class, SecondOrder.class})
@ScriptAssert.List({
    @ScriptAssert(lang = "javascript", script = "!_this.isEmptyBody()",
                  message = "Empty Body is not a valid request", groups = FirstOrder.class),
    @ScriptAssert(lang = "javascript", script = "!_this.isPeriodZero()",
                  message = "Specifying a schedule with a period of 0 is not a valid request", groups = SecondOrder.class)})
public class PeriodicEventRequestOrResponse {

    @JsonProperty("id")
    protected String eventId;
    protected String startTime;
    protected String stopTime;
    protected Integer minutes;
    protected Integer hours;
    protected Integer days;
    protected Integer weeks;

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

    public Integer getMinutes() {
        return minutes;
    }

    public void setMinutes(final Integer minutes) {
        this.minutes = minutes;
    }

    public Integer getHours() {
        return hours;
    }

    public void setHours(final Integer hours) {
        this.hours = hours;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(final Integer days) {
        this.days = days;
    }

    public Integer getWeeks() {
        return weeks;
    }

    public void setWeeks(final Integer weeks) {
        this.weeks = weeks;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    @JsonIgnore
    public boolean isEmptyBody() {
        return isPeriodNull() && startTime == null && stopTime == null;
    }

    private boolean isPeriodNull() {
        return hours == null && weeks == null && minutes == null && days == null;
    }

    @JsonIgnore
    public boolean isPeriodZero() {
        return isPeriodNull() || Duration.ofDays(convertToZeroIfNull(weeks) * 7L)
                .plus(Duration.ofDays(convertToZeroIfNull(days)))
                .plus(Duration.ofHours(convertToZeroIfNull(hours)))
                .plus(Duration.ofMinutes(convertToZeroIfNull(minutes))).getSeconds() == 0;
    }

    private int convertToZeroIfNull(final Integer period) {
        return period == null ? 0 : period;
    }

}
