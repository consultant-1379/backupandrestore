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
package com.ericsson.adp.mgmt.backupandrestore.util;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.CalendarEvent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.DayOfWeek;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.DayOfWeekOccurrence;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnprocessableEntityException;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.V4CalendarEventRequest;

/**
 * Validates a calendar event request
 */
@Component
public class CalendarEventValidator {
    private static final Logger log = LogManager.getLogger(CalendarEventValidator.class);

    private static final String TIME = "time";
    private static final String MONTH = "Month";
    private static final String START_TIME = "startTime";
    private static final String STOP_TIME = "stopTime";
    private static final String DAY_OF_MONTH = "dayOfMonth";
    private static final String DAY_OF_WEEK = "dayOfWeek";
    private static final String DAY_OF_WEEK_OCCURENCE = "dayOfWeekOccurence";
    private static final String MISSING_ERROR_MESSAGE = "Required field is missing : <%s>";
    private static final String INVALID_ERROR_MESSAGE = "Invalid field <%s : %s>";
    private static final String OVERLAP_ERROR_MESSAGE = "%s and %s cannot be specified at the same time";

    private final ESAPIValidator esapiValidator = new ESAPIValidator();

    /**
     * Checks if the request to create
     * a {@link com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.CalendarEvent CalendarEvent} is valid
     * @param request the {@link V4CalendarEventRequest V4CalendarEventRequest}
     */
    public void validate(final V4CalendarEventRequest request) {
        validateTime(request.getTime());
        checkConflictingFields(request);
        validateTimestamp(request.getStartTime(), START_TIME);
        validateTimestamp(request.getStopTime(), STOP_TIME);
        validateMonthAndDayOfMonth(request);
        compareStopTimeStartTime(request);
        compareStopTimeCurrentTime(request);
    }

    /**
     * Checks if the patch request is valid
     * @param request {@link V4CalendarEventRequest V4CalendarEventRequest}
     * @param event the {@link CalendarEvent event}
     */
    public void validatePatchRequest(final V4CalendarEventRequest request, final CalendarEvent event) {
        validate(updateRequestWithCurrentCalendarEvent(request, event));
    }

    /**
     * Update V4CalendarEventRequest request with current calendar event data.
     * for validation
     * @param request {@link V4CalendarEventRequest V4CalendarEventRequest}
     * @param event the {@link CalendarEvent event}
     * @return V4CalendarEventRequest {@link V4CalendarEventRequest V4CalendarEventRequest}
     * for validation
     */
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
    public V4CalendarEventRequest updateRequestWithCurrentCalendarEvent(final V4CalendarEventRequest request,
                                                                final CalendarEvent event) {
        if (request.getTime() == null) {
            request.setTime(event.getTime());
        }
        if (request.getDayOfWeek() == null) {
            request.setDayOfWeek(event.getDayOfWeek());
        }
        if (request.getDayOfWeekOccurrence() == null) {
            request.setDayOfWeekOccurrence(event.getDayOfWeekOccurrence());
        }
        if (request.getMonth() == null && event.getMonth() != 0) {
            request.setMonth(event.getMonth());
        }
        if (request.getDayOfMonth() == null && event.getDayOfMonth() != 0 ) {
            request.setDayOfMonth(event.getDayOfMonth());
        }
        if (request.getStartTime() == null) {
            request.setStartTime(event.getStartTime());
        }
        if (request.getStopTime() == null) {
            request.setStopTime(event.getStopTime());
        }
        return request;
    }

    private void checkConflictingFields(final V4CalendarEventRequest request) {
        if (request.getDayOfMonth() != null && request.getDayOfMonth() != 0) {
            if (request.getDayOfWeek() != null && request.getDayOfWeek() != DayOfWeek.ALL) {
                log.error(DAY_OF_MONTH + " and " + DAY_OF_WEEK +
                    " cannot be specified at the same time");
                throw new UnprocessableEntityException(String.format(OVERLAP_ERROR_MESSAGE, DAY_OF_MONTH, DAY_OF_WEEK));
            }
            if (request.getDayOfWeekOccurrence() != null && request.getDayOfWeekOccurrence() != DayOfWeekOccurrence.ALL) {
                log.error(DAY_OF_MONTH + " and " + DAY_OF_WEEK_OCCURENCE +
                    " cannot be specified at the same time");
                throw new UnprocessableEntityException(String.format(OVERLAP_ERROR_MESSAGE, DAY_OF_MONTH, DAY_OF_WEEK_OCCURENCE));
            }
        }
    }

    /**
     * Checks if the time parameter of the Calendar event is valid
     * <p> The validation constraints include:
     * <ul>
     * <li> The time is required
     * <li> The time format is HH:MM:SS
     * <li> The range of time allowed is [00:00:00, 23:59:59]
     * </ul>
     * @param time the time input
     * @throws HttpMessageNotReadableException if the time is missing or has an invalid syntax
     */
    public void validateTime(final String time) {
        if (time == null || time.isBlank()) {
            throw new HttpMessageNotReadableException(String.format(MISSING_ERROR_MESSAGE, TIME ));
        }
        validateTimeSyntax(time);
    }

    /**
     * Checks if the value of the time parameter
     * is in the format HH:MM:SS and is within the range [00:00:00, 23:59:59]
     * @param time the time input
     */
    public void validateTimeSyntax(final String time) {
        if (!esapiValidator.isValidCalendarTime(time)) {
            throw new HttpMessageNotReadableException(String.format(INVALID_ERROR_MESSAGE, TIME, time));
        }
    }

    /**
     * Checks if the value of the timestamp parameter
     * is in the format yyyy-mm-ddThh:mm:ss.000000000Z
     * or yyyy-mm-ddThh:mm:ss.000000000+01:00
     * @param timeStamp the timestamp input
     * @param fieldName String field name of the timestamp
     */
    public void validateTimestampSyntax(final String timeStamp, final String fieldName ) {
        if (!esapiValidator.isValidCalendarDateTime(timeStamp)) {
            throw new HttpMessageNotReadableException(String.format(INVALID_ERROR_MESSAGE, fieldName, timeStamp));
        }
    }

    /**
     * Checks if the timestamp is valid
     * @param timeStamp String timestamp
     * @param fieldName String field name of the timestamp
     */
    public void validateTimestamp(final String timeStamp, final String fieldName) {
        if (timeStamp != null && !timeStamp.isBlank()) {
            validateTimestampSyntax(timeStamp, fieldName);
            try {
                parseTimestamp(timeStamp);
            } catch (DateTimeParseException e) {
                throw new HttpMessageNotReadableException(
                        String.format(INVALID_ERROR_MESSAGE, fieldName, timeStamp));
            }
        }
    }

    /**
     * A timestamp without a timezone is allowed by BRO.
     * If the timezone is not specified, the system's default timezone will be used.
     * @param timeStamp the timestamp being validated.
     * @return the OffsetDateTime representation of the timestamp.
     */
    private OffsetDateTime parseTimestamp(final String timeStamp) {
        try {
            return OffsetDateTime.parse(timeStamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException e) {
            final LocalDateTime parsedDateTime = LocalDateTime.parse(timeStamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            final ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(parsedDateTime);
            return OffsetDateTime.of(parsedDateTime, zoneOffset);
        }
    }

    /**
     * Checks if Stop time is earlier than start time
     * @param request V4CalendarEventRequest request
     */
    public void compareStopTimeStartTime(final V4CalendarEventRequest request) {
        if (request.getStopTime() != null && request.getStartTime() != null &&
                !request.getStartTime().isBlank() &&
                !request.getStopTime().isBlank()) {
            if (parseTimestamp(request.getStopTime()).isBefore(
                    parseTimestamp(request.getStartTime()))) {
                throw new HttpMessageNotReadableException(String
                        .format("Stop time %s is earlier than start time %s",
                                request.getStopTime(), request.getStartTime()));
            }
        }
    }

    /**
     * Checks if Stop time is earlier than current time.
     * @param request V4CalendarEventRequest request
     */
    public void compareStopTimeCurrentTime(final V4CalendarEventRequest request) {
        final OffsetDateTime currentTime = OffsetDateTime.now();
        if (request.getStopTime() != null && !request.getStopTime().isBlank() &&
                parseTimestamp(request.getStopTime()).isBefore(
                        currentTime)) {
            throw new HttpMessageNotReadableException(String
                    .format("Stop time %s is earlier than current time %s",
                            request.getStopTime(), currentTime));
        }
    }

    /**
     * Checks if Month and/or DayOfMonth is valid
     * @param request V4CalendarEventRequest request
     */
    public void validateMonthAndDayOfMonth(final V4CalendarEventRequest request) {
        validateMonth(request);
        validateDayOfMonth(request);
        if (request.getMonth() != null && request.getDayOfMonth() != null) {
            OffsetDateTime base = OffsetDateTime.now();
            try {
                base =  OffsetDateTime.parse(base.withYear(0)
                        .withMonth(request.getMonth()).toString());
            } catch (DateTimeException dateTimeException) {
                throw new HttpMessageNotReadableException(
                        String.format(INVALID_ERROR_MESSAGE, MONTH, dateTimeException.getMessage()));
            }
            try {
                OffsetDateTime.parse(base.withDayOfMonth(request.getDayOfMonth()).toString());
            } catch (DateTimeException dateTimeException) {
                throw new HttpMessageNotReadableException(
                        String.format(INVALID_ERROR_MESSAGE, DAY_OF_MONTH, dateTimeException.getMessage()));
            }
        }
    }

    /**
     * Checks if Month is valid
     * @param request V4CalendarEventRequest request
     */
    public void validateMonth(final V4CalendarEventRequest request) {
        if (request.getMonth() != null && (request.getMonth() < 1 ||
                request.getMonth() > 12)) {
            throw new HttpMessageNotReadableException(
                    String.format(INVALID_ERROR_MESSAGE, MONTH, request.getMonth()));
        }
    }

    /**
     * Checks if DayOfMonth is valid
     * @param request V4CalendarEventRequest request
     */
    public void validateDayOfMonth(final V4CalendarEventRequest request) {
        if (request.getDayOfMonth() != null && (request.getDayOfMonth() < 1 ||
                request.getDayOfMonth() > 31)) {
            throw new HttpMessageNotReadableException(
                    String.format(INVALID_ERROR_MESSAGE, DAY_OF_MONTH, request.getDayOfMonth()));
        }
    }
}