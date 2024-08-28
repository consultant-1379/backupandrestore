/*
 *  ******************************************************************************
 *  COPYRIGHT Ericsson 2020
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 *  *******************************************************************************
 *
 */

package com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;

public class PeriodicEventTest {

    private PeriodicEvent periodicEvent = new PeriodicEvent("123", null);
    private final OffsetDateTime TIME_NOW = OffsetDateTime.now(ZoneId.of("Europe/Paris"));

    @Before
    public void setup() {
        periodicEvent.setStartTime(DateTimeUtils.convertToString(TIME_NOW));
        periodicEvent.setStopTime(DateTimeUtils.convertToString(TIME_NOW.plusDays(1)));
        periodicEvent.setDays(1);
        periodicEvent.setHours(2);
        periodicEvent.setMinutes(3);
        periodicEvent.setWeeks(4);
        periodicEvent.setEventId("FakeId");
    }

    @Test
    public void test_getEventId_eventIdIsFakeId() {
        Assert.assertEquals("FakeId", periodicEvent.getEventId());
    }

    @Test
    public void test_getDays_getDaysIs1() {
        Assert.assertEquals(1, periodicEvent.getDays());
    }

    @Test
    public void test_getHours_getHoursIs2() {
        Assert.assertEquals(2, periodicEvent.getHours());
    }

    @Test
    public void test_getMinutes_getMinutesIs3() {
        Assert.assertEquals(3, periodicEvent.getMinutes());
    }

    @Test
    public void test_getWeeks_getWeeksIs4() {
        Assert.assertEquals(4, periodicEvent.getWeeks());
    }

    @Test
    public void test_getStartTime_getStartTimeIsTIME_NOW() {
        Assert.assertEquals(DateTimeUtils.convertToString(TIME_NOW), periodicEvent.getStartTime());
    }

    @Test
    public void test_getStopTime_getStopTimeIsTIME_NOWplusADay() {
        Assert.assertEquals(DateTimeUtils.convertToString(TIME_NOW.plusDays(1)), periodicEvent.getStopTime());
    }

    @Test
    public void test_getBackupManagerId_backuoManagerId() {
        Assert.assertEquals("123", periodicEvent.getBackupManagerId());
    }

    @Test
    public void test_setEventId_getIdReturnsNewId() {
        periodicEvent.setEventId("NewId");
        Assert.assertEquals("NewId", periodicEvent.getEventId());
    }

    @Test
    public void test_setDays_getDaysReturns2() {
        periodicEvent.setDays(2);
        Assert.assertEquals(2, periodicEvent.getDays());
    }

    @Test
    public void test_setHours_getHoursReturns4() {
        periodicEvent.setHours(4);
        Assert.assertEquals(4, periodicEvent.getHours());
    }

    @Test
    public void test_setMinutes_getMinutesReturns5() {
        periodicEvent.setMinutes(5);
        Assert.assertEquals(5,periodicEvent.getMinutes());
    }

    @Test
    public void test_setWeeks_getWeeksReturns6() {
        periodicEvent.setWeeks(6);
        Assert.assertEquals(6, periodicEvent.getWeeks());
    }

    @Test
    public void test_setStartTime_getStartTimeReturnsNewStartTime() {
        final String dateTime = DateTimeUtils.convertToString(OffsetDateTime.now());
        periodicEvent.setStartTime(dateTime);
        Assert.assertEquals(dateTime, periodicEvent.getStartTime());
    }

    @Test
    public void test_setStopTime_getStopTimeReturnsNewStopTime() {
        final String dateTime = DateTimeUtils.convertToString(OffsetDateTime.now());
        periodicEvent.setStopTime(dateTime);
        Assert.assertEquals(dateTime, periodicEvent.getStopTime());
    }
}