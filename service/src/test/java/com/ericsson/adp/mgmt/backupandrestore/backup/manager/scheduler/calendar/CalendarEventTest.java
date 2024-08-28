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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

import java.time.ZoneOffset;
import java.util.Optional;

public class CalendarEventTest {
    private CalendarEvent calendarEvent;
    @Before
    public void setUp() {
        calendarEvent = new CalendarEvent();
        calendarEvent.setEventId("54321");
        calendarEvent.setTime("09:00");
        calendarEvent.setStartTime("2022-05-22T08:00:00Z");
        Clock fixedClock = Clock.fixed(Instant.parse("2022-05-22T08:00:00Z"), ZoneOffset.UTC);
        calendarEvent.setClock(fixedClock);
    }

    @Test
    public void getNextValidTime_everyday_9am_valid() {
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isPresent());
        assertEquals(nextDay.get(), LocalDateTime.parse("2022-05-22T09:00"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2022-05-22T09:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime().get(), LocalDateTime.parse("2022-05-23T09:00"));
    }

    @Test
    public void getNextValidTime_everyday_7am_valid() {
        calendarEvent.setTime("07:00");
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isPresent());
        assertEquals(nextDay.get(), LocalDateTime.parse("2022-05-23T07:00"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2022-05-23T07:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime().get(), LocalDateTime.parse("2022-05-24T07:00"));
    }

    @Test
    public void getNextValidTime_everyday_9am_ends_on_sameDay_valid() {
        calendarEvent.setTime("09:00");
        calendarEvent.setStopTime("2022-05-22T10:00:00Z");
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isPresent());
        assertEquals(nextDay.get(), LocalDateTime.parse("2022-05-22T09:00"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2022-05-22T10:00:01Z"), ZoneOffset.UTC));
        assertTrue(calendarEvent.getNextValidTime().isEmpty());
    }

    @Test
    public void getNextValidTime_June() {
        calendarEvent.setMonth(6);
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isPresent());
        assertEquals(nextDay.get(), LocalDateTime.parse("2022-06-01T09:00"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2022-06-01T09:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime().get(), LocalDateTime.parse("2022-06-02T09:00"));
    }

    @Test
    public void getNextValidTime_April() {
        calendarEvent.setMonth(4);
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isPresent());
        assertEquals(nextDay.get(), LocalDateTime.parse("2023-04-01T09:00"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2023-04-01T09:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime().get(), LocalDateTime.parse("2023-04-02T09:00"));
    }

    @Test
    public void getNextValidTime_everyMonth_21st() {
        calendarEvent.setMonth(0);
        calendarEvent.setDayOfMonth(21);
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isPresent());
        assertEquals(nextDay.get(), LocalDateTime.parse("2022-06-21T09:00"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2022-06-21T09:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime().get(), LocalDateTime.parse("2022-07-21T09:00"));
    }

    @Test
    public void getNextValidTime_April_21st() {
        calendarEvent.setMonth(4);
        calendarEvent.setDayOfMonth(21);
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isPresent());
        assertEquals(nextDay.get(), LocalDateTime.parse("2023-04-21T09:00"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2023-04-21T09:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime().get(), LocalDateTime.parse("2024-04-21T09:00"));
    }

    @Test
    public void getNextValidTime_June_Monday() {
        calendarEvent.setMonth(6);
        calendarEvent.setDayOfMonth(0);
        calendarEvent.setDayOfWeek(DayOfWeek.MONDAY);
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isPresent());
        assertEquals(nextDay.get(), LocalDateTime.parse("2022-06-06T09:00"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2022-06-06T09:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime().get(), LocalDateTime.parse("2022-06-13T09:00"));
    }

    @Test
    public void getNextValidTime_FirstMonday() {
        calendarEvent.setMonth(0);
        calendarEvent.setDayOfMonth(0);
        calendarEvent.setDayOfWeek(DayOfWeek.TUESDAY);
        calendarEvent.setDayOfWeekOccurrence(DayOfWeekOccurrence.FIRST);
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isPresent());
        assertEquals(nextDay.get(), LocalDateTime.parse("2022-06-07T09:00"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2022-06-07T09:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime().get(), LocalDateTime.parse("2022-07-05T09:00"));
    }

    @Test
    public void getNextValidTime_May_FourthMonday() {
        calendarEvent.setMonth(5);
        calendarEvent.setDayOfMonth(0);
        calendarEvent.setDayOfWeek(DayOfWeek.MONDAY);
        calendarEvent.setDayOfWeekOccurrence(DayOfWeekOccurrence.FOURTH);
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isPresent());
        assertEquals(nextDay.get(), LocalDateTime.parse("2022-05-23T09:00"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2022-05-23T09:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime().get(), LocalDateTime.parse("2023-05-22T09:00"));
    }

    @Test
    public void getNextValidTime_FirstSunday_inBetween() {
        calendarEvent.setMonth(0);
        calendarEvent.setDayOfMonth(0);
        calendarEvent.setDayOfWeek(DayOfWeek.SUNDAY);
        calendarEvent.setDayOfWeekOccurrence(DayOfWeekOccurrence.FIRST);
        calendarEvent.setStartTime("2022-06-02T17:15:00Z");
        calendarEvent.setStopTime("2023-02-02T17:15:00Z");
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isPresent());
        assertEquals(nextDay.get(), LocalDateTime.parse("2022-06-05T09:00"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2022-06-05T09:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime().get(), LocalDateTime.parse("2022-07-03T09:00"));
    }

    @Test
    public void getNextValidTime_December_31st_inBetween() {
        calendarEvent.setMonth(12);
        calendarEvent.setDayOfMonth(31);
        calendarEvent.setStartTime("2022-02-02T17:15:00Z");
        calendarEvent.setStopTime("2023-02-02T17:15:00Z");
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isPresent());
        assertEquals(nextDay.get(), LocalDateTime.parse("2022-12-31T09:00"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2022-12-31T09:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime(), Optional.empty());
    }

    @Test
    public void getNextValidTime_December_LastSunday() {
        calendarEvent.setMonth(12);
        calendarEvent.setDayOfMonth(0);
        calendarEvent.setDayOfWeek(DayOfWeek.SUNDAY);
        calendarEvent.setDayOfWeekOccurrence(DayOfWeekOccurrence.LAST);
        calendarEvent.setStartTime("2022-02-02T17:15:00Z");
        calendarEvent.setStopTime("2024-02-02T17:15:00Z");
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isPresent());
        assertEquals(nextDay.get(), LocalDateTime.parse("2022-12-25T09:00"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2022-12-25T09:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime().get(), LocalDateTime.parse("2023-12-31T09:00"));
    }

    @Test
    public void getNextValidTime_December_AllSunday() {
        calendarEvent.setMonth(12);
        calendarEvent.setDayOfMonth(0);
        calendarEvent.setDayOfWeek(DayOfWeek.SUNDAY);
        calendarEvent.setDayOfWeekOccurrence(DayOfWeekOccurrence.ALL);
        calendarEvent.setStartTime("2022-02-02T17:15:00Z");
        calendarEvent.setStopTime("2023-02-02T17:15:00Z");
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isPresent());
        assertEquals(nextDay.get(), LocalDateTime.parse("2022-12-04T09:00"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2022-12-04T09:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime().get(), LocalDateTime.parse("2022-12-11T09:00"));
    }

    @Test
    public void getNextValidTime_November_LastWeek() {
        calendarEvent.setMonth(11);
        calendarEvent.setDayOfMonth(0);
        calendarEvent.setDayOfWeek(DayOfWeek.ALL);
        calendarEvent.setDayOfWeekOccurrence(DayOfWeekOccurrence.LAST);
        calendarEvent.setStartTime("2022-02-02T17:15:00Z");
        calendarEvent.setStopTime("2023-02-02T17:15:00Z");
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isPresent());
        assertEquals(nextDay.get(), LocalDateTime.parse("2022-11-24T09:00"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2022-11-24T09:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime().get(), LocalDateTime.parse("2022-11-25T09:00"));
    }

    @Test
    public void getNextValidTime_Feb_29th() {
        calendarEvent.setMonth(2);
        calendarEvent.setDayOfMonth(29);
        calendarEvent.setTime("08:20");
        calendarEvent.setDayOfWeek(DayOfWeek.ALL);
        calendarEvent.setDayOfWeekOccurrence(DayOfWeekOccurrence.ALL);
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isPresent());
        assertEquals(nextDay.get(), LocalDateTime.parse("2024-02-29T08:20"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2024-02-29T09:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime().get(), LocalDateTime.parse("2028-02-29T08:20"));
    }

    @Test
    public void getNextValidTime_June_31st() {
        calendarEvent.setMonth(6);
        calendarEvent.setDayOfMonth(31);
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertTrue(nextDay.isEmpty());
    }

    @Test
    public void getNextValidTime_everyMonth_29th() {
        calendarEvent.setMonth(0);
        calendarEvent.setDayOfMonth(29);
        calendarEvent.setClock(Clock.fixed(Instant.parse("2023-01-01T09:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime().get(), LocalDateTime.parse("2023-01-29T09:00"));
        calendarEvent.setClock(Clock.fixed(Instant.parse("2023-02-01T09:00:01Z"), ZoneOffset.UTC));
        assertEquals(calendarEvent.getNextValidTime().get(), LocalDateTime.parse("2023-03-29T09:00"));
    }

    @Test
    public void getDurationToNextTime_May_23th() {
        calendarEvent.setMonth(5);
        calendarEvent.setDayOfMonth(23);
        Optional<LocalDateTime> nextDay = calendarEvent.getNextValidTime();
        assertEquals(nextDay.get(), LocalDateTime.parse("2022-05-23T09:00"));
        assertEquals(calendarEvent.getDurationToNextTime(), Long.valueOf(25L * 3600 * 1000000000));
    }
}
