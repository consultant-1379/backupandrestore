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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.CalendarEvent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import org.junit.Before;
import org.junit.Test;

public class SchedulerTest {

    private Scheduler scheduler;
    private ScheduledEventHandler handler;
    private SchedulerFileService schedulerFileService;

    @Before
    public void setUp() {
        scheduler = new Scheduler("123", null);
        handler = createMock(ScheduledEventHandler.class);
        schedulerFileService = createMock(SchedulerFileService.class);
        scheduler.addPeriodicEvent(getPeriodicEvent("1", OffsetDateTime.parse("2020-10-30T11:23:22.265+02:00")));
        scheduler.addPeriodicEvent(getPeriodicEvent("2", OffsetDateTime.parse("2020-09-30T11:23:22.265+02:00")));
        scheduler.addPeriodicEvent(getPeriodicEvent("3", OffsetDateTime.parse("2020-09-30T11:23:23.265Z")));
        scheduler.addPeriodicEvent(getPeriodicEvent("4", OffsetDateTime.parse("2020-09-30T11:23:23.265-02:00")));
        scheduler.addPeriodicEvent(getPeriodicEvent("5", null));
    }

    @Test
    public void test_reload_of_scheduler_events() {
        assertEquals(5, scheduler.getPeriodicEvents().size());
        expect(schedulerFileService.getPersistedSchedulerInformation("123")).andReturn(new SchedulerInformation());

        List<PeriodicEvent> events = new ArrayList<>();
        PeriodicEvent event = getPeriodicEvent("6", OffsetDateTime.parse("2020-10-30T11:23:23.265-02:00"));
        events.add(event);
        List<CalendarEvent> calendarEvents = new ArrayList<>();
        CalendarEvent calendarEvent = getCalendarEvent("456", OffsetDateTime.parse("2020-10-30T11:23:23.265-02:00"));
        calendarEvents.add(calendarEvent);
        expect(handler.getPeriodicEvents("123")).andReturn(events);
        expect(handler.getCalendarEvents("123")).andReturn(calendarEvents);

        handler.schedulePeriodicEvents(scheduler);
        expectLastCall();
        handler.scheduleCalendarEvents(scheduler);
        expectLastCall();
        replay(handler, schedulerFileService);
        scheduler.reload(handler, schedulerFileService);

        assertEquals(event, scheduler.getPeriodicEvent("6"));
        assertEquals(calendarEvent, scheduler.getCalendarEvent("456"));
        assertEquals(1, scheduler.getPeriodicEvents().size());
    }

    @Test
    public void updateNextScheduledTime_adminStateLocked_updateNextScheduledTimeToNull() {
        scheduler.setAdminState(AdminState.LOCKED);
        scheduler.updateNextScheduledTime();
        assertNull(scheduler.getNextScheduledTime());
    }

    @Test
    public void updateNextScheduledTime_adminStateUnlocked_updateNextScheduledTimeToNearestTime() {
        scheduler.updateNextScheduledTime();
        assertEquals(scheduler.getPeriodicEvent("2").getNextRun().toString(),
                scheduler.getNextScheduledTime());
    }

    @Test
    public void getPeriodicEventIndex_eventId_returnsIndex() {
        scheduler.addPeriodicEvent(getPeriodicEvent("0", null));
        scheduler.addPeriodicEvent(getPeriodicEvent("id1", null));
        assertEquals(0, scheduler.getPeriodicEventIndex("1"));
    }

    @Test
    public void getPeriodicEvent_index_returnsEvent() {
        scheduler.addPeriodicEvent(getPeriodicEvent("newId", null));
        scheduler.addPeriodicEvent(getPeriodicEvent("id5", null));
        assertEquals(scheduler.getPeriodicEvent("newId"), scheduler.getPeriodicEvent(5));
    }

    @Test
    public void updatePeriodicEvent_eventID_returnsEvent() {
        OffsetDateTime time = OffsetDateTime.parse("2024-01-01T11:23:23.265-02:00");
        scheduler.addPeriodicEvent(getPeriodicEvent("id1", time));
        scheduler.addPeriodicEvent(getPeriodicEvent("id2", null));
        PeriodicEvent newEvent = getPeriodicEvent("newId", time);
        scheduler.replacePeriodicEvent("id1", newEvent);
        assertEquals(null, scheduler.getPeriodicEvent("id2").getNextRun());
        assertEquals(time, scheduler.getPeriodicEvent("newId").getNextRun());
    }

    private PeriodicEvent getPeriodicEvent(final String id, final OffsetDateTime nextRun) {
        final PeriodicEvent event = new PeriodicEvent("123", null);
        event.setEventId(id);
        event.setNextRun(nextRun);
        return event;
    }

    private CalendarEvent getCalendarEvent(final String id, final OffsetDateTime nextRun) {
        final CalendarEvent event = new CalendarEvent();
        event.setEventId(id);
        event.setNextRun(nextRun);
        return event;
    }
}
