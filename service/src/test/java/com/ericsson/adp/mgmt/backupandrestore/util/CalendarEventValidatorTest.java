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
package com.ericsson.adp.mgmt.backupandrestore.util;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.DayOfWeek;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.DayOfWeekOccurrence;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnprocessableEntityException;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.V4CalendarEventRequest;

public class CalendarEventValidatorTest {
    private static final OffsetDateTime NOW = OffsetDateTime.now();
    private static final String VALID_START_TIME = DateTimeUtils.convertToString(NOW);
    private static final String VALID_END_TIME = DateTimeUtils.convertToString(NOW.plusYears(5));
    private static final List<String> VALID_TIMES = List.of("04:50:59", "00:00:00", "00:00:01", "23:29:58", "23:59:59");
    private static final String VALID_TIME = "04:50:59";
    private static final int VALID_MONTH = 2;
    private static final int VALID_DAY_OF_MONTH = 28;
    private static final int INVALID_MONTH = 13;
    private static final int INVALID_DAY_OF_MONTH = 35;
    private static final String INVALID_DATE = "2022-02-29T08:00:00Z";

    private CalendarEventValidator calendarEventValidator;
    private V4CalendarEventRequest request;

    @Before
    public void setup() {
        calendarEventValidator = new CalendarEventValidator();
        request = new V4CalendarEventRequest();
    }

    @Test (expected = Test.None.class)
    public void testValidCalendarEventRequest_ValidTimes() {
        VALID_TIMES.forEach(time -> {
            request.setTime(time);
            calendarEventValidator.validate(request);
        });
    }

    @Test (expected = HttpMessageNotReadableException.class)
    public void testInValidCalendarEventRequest_TimeWithTimezone() {
        request.setTime("04:50:59Z");
        calendarEventValidator.validate(request);
    }

    @Test (expected = HttpMessageNotReadableException.class)
    public void testInValidCalendarEventRequest_TimeWithoutSECONDS() {
        request.setTime("04:50");
        calendarEventValidator.validate(request);
    }

    @Test (expected = HttpMessageNotReadableException.class)
    public void testInValidCalendarEventRequest_EmptyTime() {
        request.setTime("");
        calendarEventValidator.validate(request);
    }

    @Test (expected = HttpMessageNotReadableException.class)
    public void testInValidCalendarEventRequest_NullTime() {
        calendarEventValidator.validate(request);
    }

    @Test (expected = HttpMessageNotReadableException.class)
    public void testInValidCalendarEventRequest_TimeIsOutsideValidRange() {
        request.setTime("24:00:00");
        calendarEventValidator.validate(request);
    }

    @Test (expected = HttpMessageNotReadableException.class)
    public void testInValidCalendarEventRequest_InvalidDate() {
        request.setTime(VALID_TIME);
        request.setStartTime(INVALID_DATE);
        calendarEventValidator.validate(request);
    }

    @Test (expected = HttpMessageNotReadableException.class)
    public void testInValidCalendarEventRequest_InvalidMonthAndDay() {
        request.setTime(VALID_TIME);
        request.setMonth(INVALID_MONTH);
        request.setDayOfMonth(INVALID_DAY_OF_MONTH);
        calendarEventValidator.validate(request);
    }

    @Test (expected = UnprocessableEntityException.class)
    public void testInValidCalendarEventRequest_DayOfMonthAndDayOfWeekAreBothSet() {
        request.setTime(VALID_TIME);
        request.setDayOfMonth(18);
        request.setDayOfWeek(DayOfWeek.MONDAY);
        calendarEventValidator.validate(request);
    }

    @Test (expected = UnprocessableEntityException.class)
    public void testInValidCalendarEventRequest_DayOfMonthAndDayOfWeekOccurenceAreBothSet() {
        request.setTime(VALID_TIME);
        request.setDayOfMonth(VALID_DAY_OF_MONTH);
        request.setDayOfWeekOccurrence(DayOfWeekOccurrence.LAST);
        calendarEventValidator.validate(request);
    }

    @Test (expected = Test.None.class)
    public void testValidCalendarEventRequest() {
        request.setStartTime(VALID_START_TIME);
        request.setStopTime(VALID_END_TIME);
        request.setMonth(VALID_MONTH);
        request.setTime(VALID_TIME);
        calendarEventValidator.validate(request);
    }
}
