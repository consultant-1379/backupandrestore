/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.scheduler;

/**
 * JSON request to schedule a backup.
 */
public class CreateEventRequest {

    private String scheduledTime;
    private String startTime;
    private String endTime;
    private Integer month;
    private String dayOfWeek;
    private Integer dayOfMonth;
    private String time;
    private String dayOfWeekOccurrence;

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(final String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(final String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(final String endTime) {
        this.endTime = endTime;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(final Integer month) {
        this.month = month;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(final String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(final Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public String getTime() {
        return time;
    }

    public void setTime(final String time) {
        this.time = time;
    }

    public String getDayOfWeekOccurrence() {
        return dayOfWeekOccurrence;
    }

    public void setDayOfWeekOccurrence(final String dayOfWeekOccurrence) {
        this.dayOfWeekOccurrence = dayOfWeekOccurrence;
    }

}
