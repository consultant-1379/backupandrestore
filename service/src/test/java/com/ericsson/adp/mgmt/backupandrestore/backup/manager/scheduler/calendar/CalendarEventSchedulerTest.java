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

import com.ericsson.adp.mgmt.backupandrestore.test.MyTestConfiguration;
import com.ericsson.adp.mgmt.backupandrestore.test.SystemTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public class CalendarEventSchedulerTest extends SystemTest {
    @Autowired
    CalendarEventScheduler calendarEventScheduler;

    private CalendarEvent calendarEvent;
    @Before
    public void setUp() {
        calendarEvent = new CalendarEvent();
        calendarEvent.setEventId("54321");
        calendarEvent.setTime("19:00");
        calendarEvent.setBackupManagerId("DEFAULT-bro");
        calendarEvent.setStartTime("2022-05-22T08:00:00Z");
        Clock fixedClock = Clock.fixed(Instant.parse("2022-05-22T08:00:00Z"), ZoneOffset.UTC);
        calendarEvent.setClock(fixedClock);
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void scheduledTask_test_valid() {
        calendarEventScheduler.scheduledTask(calendarEvent).run();
    }
}
