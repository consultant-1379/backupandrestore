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
package com.ericsson.adp.mgmt.backupandrestore.rest.scheduler;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.DayOfWeek;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.DayOfWeekOccurrence;


/**
 * The Request body to create a calendar scheduler.
 */
public class V4CalendarEventRequest {

    private Integer dayOfMonth;
    private DayOfWeek dayOfWeek;
    private DayOfWeekOccurrence dayOfWeekOccurrence;
    private Integer month;
    private String startTime;
    private String stopTime;
    private String time;

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public DayOfWeekOccurrence getDayOfWeekOccurrence() {
        return dayOfWeekOccurrence;
    }

    public Integer getMonth() {
        return month;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getStopTime() {
        return stopTime;
    }

    public String getTime() {
        return time;
    }

    public void setDayOfMonth(final Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public void setDayOfWeek(final DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public void setDayOfWeekOccurrence(final DayOfWeekOccurrence dayOfWeekOccurrence) {
        this.dayOfWeekOccurrence = dayOfWeekOccurrence;
    }

    public void setMonth(final Integer month) {
        this.month = month;
    }

    public void setStartTime(final String startTime) {
        this.startTime = startTime;
    }

    public void setStopTime(final String stopTime) {
        this.stopTime = stopTime;
    }

    public void setTime(final String time) {
        this.time = time;
    }
}
