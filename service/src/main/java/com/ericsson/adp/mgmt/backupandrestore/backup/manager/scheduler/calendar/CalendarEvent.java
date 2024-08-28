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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar;


import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.ScheduledEvent;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.V4CalendarEventRequest;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Represents a Calendar Event
 */
@JsonInclude(Include.NON_NULL)
public class CalendarEvent extends ScheduledEvent implements Versioned<CalendarEvent> {

    private String time;

    /**
     * Default is 0, means every month
     */
    private int month;
    /**
     * Default is 0, means every day in a month
     */
    private int dayOfMonth;
    /**
     * Default is ALL, means every day in a week
     */
    private DayOfWeek dayOfWeek = DayOfWeek.ALL;
    /**
     * Default is ALL, means every occurrence
     */
    private DayOfWeekOccurrence dayOfWeekOccurrence = DayOfWeekOccurrence.ALL;

    @JsonIgnore
    private Consumer<CalendarEvent> persistFunction;

    @JsonIgnore
    private Version<CalendarEvent> version;
    @JsonIgnore
    private Clock clock = Clock.systemDefaultZone();


    /**
     * Constructor, needed when read json file to object
     */
    public CalendarEvent() {
    }

    /**
     * Constructor of the calendar event
     * @param backupManagerId the specific backup manager
     * @param request the request body
     * @param persistFunction the persist function
     */
    public CalendarEvent(final String backupManagerId, final V4CalendarEventRequest request, final Consumer<CalendarEvent> persistFunction) {
        setBackupManagerId(backupManagerId);
        this.persistFunction = persistFunction;
        dayOfMonth = request.getDayOfMonth() == null ? 0 : request.getDayOfMonth();
        dayOfWeek = request.getDayOfWeek() == null ? DayOfWeek.ALL : request.getDayOfWeek();
        dayOfWeekOccurrence = request.getDayOfWeekOccurrence() == null ? DayOfWeekOccurrence.ALL : request.getDayOfWeekOccurrence();
        handleDayFields();
        month = request.getMonth() == null ? 0 : request.getMonth();
        time = request.getTime();

        startTime = DateTimeUtils.convertToString(request.getStartTime() != null && !request.getStartTime().isBlank() ?
                DateTimeUtils.parseToOffsetDateTime(request.getStartTime()) : OffsetDateTime.now(clock));
        stopTime = request.getStopTime() != null && !request.getStopTime().isBlank() ?
                DateTimeUtils.convertToISOOffsetDateTime(request.getStopTime()) : null;
    }

    private void handleDayFields() {
        if (dayOfMonth != 0) {
            dayOfWeek = null;
            dayOfWeekOccurrence = null;
        }
    }

    /**
     * Base on current event get the time of next valid candidate.
     * @return The optional of next valid time
     */
    @JsonIgnore
    public Optional<LocalDateTime> getNextValidTime() {
        /**
         * Try to get the right Start date and time
         */
        LocalDate startDate;
        OffsetDateTime offsetStartDateTime = DateTimeUtils.parseToOffsetDateTime(this.startTime);
        offsetStartDateTime = offsetStartDateTime.isAfter(OffsetDateTime.now(clock)) ? offsetStartDateTime : OffsetDateTime.now(clock);
        // start time with timezone is converted to local date and local time here.
        // time is used as local time without timezone or offset.
        if (offsetStartDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalTime().isAfter(LocalTime.parse(time))) {
            startDate = offsetStartDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate().plusDays(1);
        } else {
            startDate = offsetStartDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
        }

        /** Try to get the right End date, the valid date windows is [startDate, endDate).
         * if the localStopTime is before specific time.
         * The End date should minus 1 day.
         */
        LocalDate endDate;
        if (this.stopTime != null) {
            final OffsetDateTime offsetStopDateTime = DateTimeUtils.parseToOffsetDateTime(this.stopTime);
            if (offsetStopDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalTime().isBefore(LocalTime.parse(time))) {
                endDate = offsetStopDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
            } else {
                endDate = offsetStopDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate().plusDays(1);
            }
        } else {
            /**
             * If there is not stop time, the scheduler should never end.
             * 29th of Feb will occur every four years.
             * so the candidate time range should be at least 5 years.
             */
            endDate = startDate.plusYears(5);
        }

        //Get the valid LocalDate stream
        final Stream<LocalDate> localDateStream = startDate.datesUntil(endDate);

        final Optional<LocalDate> bestCandidate = localDateStream
            .filter(this::shouldKeepTheDay)
            .findFirst();

        if (bestCandidate.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(LocalDateTime.of(bestCandidate.get(), LocalTime.parse(time)));
        }
    }

    private boolean shouldKeepTheDay(final LocalDate date) {
        boolean needKeep = true;
        if (this.dayOfMonth != 0) {
            needKeep = date.getDayOfMonth() == dayOfMonth;
        }
        if (this.month != 0) {
            needKeep = date.getMonth().getValue() == month && needKeep;
        }
        if (this.dayOfWeek != DayOfWeek.ALL && this.dayOfWeek != null) {
            needKeep = date.getDayOfWeek().name().equals(this.dayOfWeek.name()) && needKeep;
        }
        if (this.dayOfWeekOccurrence != DayOfWeekOccurrence.ALL && this.dayOfWeekOccurrence != null) {
            needKeep = needKeep && this.isTheRightDayOfWeekOccurrence(date);
        }
        return needKeep;
    }

    /**
     * Get the duration to next valid execution time
     * @return the duration
     */
    @JsonIgnore
    public Long getDurationToNextTime() {
        // Calculate the duration from Now
        final Optional<LocalDateTime> nextTime = getNextValidTime();
        return nextTime.map(localDateTime -> LocalDateTime.now(clock).until(localDateTime,
                ChronoUnit.NANOS)).orElse(0L);
    }

    /**
     * Given a day, check if it has the right day of week occurrence
     * it is used to filter the right day of execution
     * it is only checked when DayOfWeekOccurrence is not ALL
     * @param day the day to check
     * @return if it is at the right day of week occurrence
     */
    public boolean isTheRightDayOfWeekOccurrence(final LocalDate day) {
        final Calendar calendar = Calendar.getInstance();
        // because of different enum order
        calendar.set(day.getYear(), day.getMonthValue() - 1, day.getDayOfMonth());
        if (this.getDayOfWeekOccurrence() != DayOfWeekOccurrence.LAST) {
            return calendar.get(Calendar.DAY_OF_WEEK_IN_MONTH) == this.getDayOfWeekOccurrence().value;
        } else {
            // It is the last occurrence when only maximum 6 days left.
            final int remainDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH) - calendar.get(Calendar.DAY_OF_MONTH);
            return remainDays < 7;
        }
    }

    /**
     * Converts the CalendarEvent object to string
     * This methods removes the timezone info in the startTime and stopTime JSON fields
     * The timezone information is stored in a separate block object.
     * This block is then appended to the CalendarEvent's JSON object.
     * @return the JSON string to be stored in its json file
     */
    @JsonIgnore
    @Override
    public String toJson() {
        final Optional<CalendarEvent> deepCopy = jsonService.parseJsonString(JsonService.toJsonString(this), CalendarEvent.class);
        if (deepCopy.isPresent()) {
            return toJson(deepCopy.get());
        }
        return "{}";
    }

    public void setVersion(final Version<CalendarEvent> version) {
        this.version = version;
    }

    /**
     * return object persisted version
     *
     * @return persisted version
     */
    public Version<CalendarEvent> getVersion() {
        return this.version;
    }


    public Consumer<CalendarEvent> getPersistFunction() {
        return persistFunction;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(final int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(final DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public DayOfWeekOccurrence getDayOfWeekOccurrence() {
        return dayOfWeekOccurrence;
    }

    public void setDayOfWeekOccurrence(final DayOfWeekOccurrence dayOfWeekOccurrence) {
        this.dayOfWeekOccurrence = dayOfWeekOccurrence;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(final int month) {
        this.month = month;
    }

    public String getTime() {
        return time;
    }

    public void setTime(final String time) {
        this.time = time;
    }


    public void setPersistFunction(final Consumer<CalendarEvent> persistFunction) {
        this.persistFunction = persistFunction;
    }

    public void setClock(final Clock clock) {
        this.clock = clock;
    }

    public Clock getClock() {
        return clock;
    }
}
