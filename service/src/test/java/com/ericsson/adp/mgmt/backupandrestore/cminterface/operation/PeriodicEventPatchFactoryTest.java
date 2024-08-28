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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.operation;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMPeriodicEventJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchRequest;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;

public class PeriodicEventPatchFactoryTest {
    private static final String PERIODIC_URL = "/ericsson-brm:brm/backup-manager/6/scheduler/periodic-event/";
    private BackupManagerPatchFactory backupManagerPatchFactory;
    private PeriodicEventPatchFactory periodicEventPatchFactory;
    private BackupManagerRepository backupManagerRepository;
    private PeriodicEventFileService fileService;

    @Before
    public void setup() {
        backupManagerPatchFactory = EasyMock.createMock(BackupManagerPatchFactory.class);
        backupManagerRepository = EasyMock.createMock(BackupManagerRepository.class);
        periodicEventPatchFactory = new PeriodicEventPatchFactory();
        EasyMock.expect(backupManagerPatchFactory.getPathToBackupManager("666")).andReturn("6");
        EasyMock.replay(backupManagerPatchFactory);
        periodicEventPatchFactory.setBackupManagerPatchFactory(backupManagerPatchFactory);
        periodicEventPatchFactory.setBackupManagerRepository(backupManagerRepository);
        fileService = new PeriodicEventFileService();
    }

    @Test
    public void getPatchToAddPeriodicEvent_periodicEvent_patchToAddIt() {
        final PeriodicEvent periodicEvent = new PeriodicEvent("666", fileService::writeToFile);
        periodicEvent.setDays(1);
        periodicEvent.setEventId("1");
        periodicEvent.setHours(1);
        periodicEvent.setMinutes(2);
        periodicEvent.setWeeks(0);

        final OffsetDateTime startTime = OffsetDateTime.now();
        final OffsetDateTime stopTime = startTime.plusMinutes(1);
        periodicEvent.setStartTime(DateTimeUtils.convertToString(startTime));
        periodicEvent.setStopTime(DateTimeUtils.convertToString(stopTime));

        final AddPeriodicEventPatch patch = periodicEventPatchFactory.getPatchToAddPeriodicEvent(periodicEvent);
        final PatchRequest json = patch.toJson();

        assertEquals(1, json.getOperations().size());
        assertEquals("add", json.getOperations().get(0).getOperation());
        assertEquals(PERIODIC_URL + "-", json.getOperations().get(0).getPath());

        final BRMPeriodicEventJson brmPeriodicEventJson = (BRMPeriodicEventJson) json.getOperations().get(0).getValue();
        assertEquals("1", brmPeriodicEventJson.getEventId());
        assertEquals(Integer.valueOf(2), brmPeriodicEventJson.getMinutes());
        assertEquals(Integer.valueOf(1), brmPeriodicEventJson.getHours());
        assertEquals(Integer.valueOf(1), brmPeriodicEventJson.getDays());
        assertEquals(Integer.valueOf(0), brmPeriodicEventJson.getWeeks());
        assertEquals(DateTimeUtils.convertToString(startTime), brmPeriodicEventJson.getStartTime());
        assertEquals(DateTimeUtils.convertToString(stopTime), brmPeriodicEventJson.getStopTime());
    }

    @Test
    public void getPatchToUpdatePeriodicEvent_periodicEvent_patchToUpdateIt() throws Exception {
        EasyMock.expect(backupManagerRepository.getBackupManager("666")).andReturn(mockBackupManager());
        EasyMock.replay(backupManagerRepository);

        final UpdatePeriodicEventPatch patch = periodicEventPatchFactory.getPatchToUpdatePeriodicEvent(mockEvent("D"));

        final PatchRequest json = patch.toJson();

        assertEquals(1, json.getOperations().size());
        assertEquals("replace", json.getOperations().get(0).getOperation());
        assertEquals(PERIODIC_URL + 3, json.getOperations().get(0).getPath());
        final BRMPeriodicEventJson brmPeriodicEventJson = (BRMPeriodicEventJson) json.getOperations().get(0).getValue();
        assertEquals(Integer.valueOf(0), brmPeriodicEventJson.getDays());
        assertEquals(Integer.valueOf(5), brmPeriodicEventJson.getHours());
        assertEquals(Integer.valueOf(0), brmPeriodicEventJson.getWeeks());
        assertEquals(Integer.valueOf(10), brmPeriodicEventJson.getMinutes());
        assertEquals("D", brmPeriodicEventJson.getEventId());
        assertEquals(Integer.valueOf(0), brmPeriodicEventJson.getDays());
        assertEquals(DateTimeUtils.convertToString(getDateTime(LocalDateTime.of(2020, 1, 2, 3, 4, 5))), brmPeriodicEventJson.getStartTime());
        assertEquals(DateTimeUtils.convertToString(getDateTime(LocalDateTime.of(2021, 1, 2, 3, 4, 5))), brmPeriodicEventJson.getStopTime());
    }

    @Test
    public void getPatchToDeletePeriodicEvent_periodicEvent_patchToDeleteIt() throws Exception {
        EasyMock.expect(backupManagerRepository.getBackupManager("666")).andReturn(mockBackupManager());
        EasyMock.replay(backupManagerRepository);

        final DeletePeriodicEventPatch patch = periodicEventPatchFactory.getPatchToDeletePeriodicEvent(mockEvent("D"));

        final PatchRequest json = patch.toJson();

        assertEquals(1, json.getOperations().size());
        assertEquals("remove", json.getOperations().get(0).getOperation());
        assertEquals(PERIODIC_URL + 3, json.getOperations().get(0).getPath());
    }

    private BackupManager mockBackupManager() {
        final BackupManager backupManager = EasyMock.createMock(BackupManager.class);
        final Scheduler scheduler = EasyMock.createMock(Scheduler.class);
        EasyMock.expect(backupManager.getScheduler()).andReturn(scheduler);
        EasyMock.expect(scheduler.getPeriodicEvents()).andReturn(Arrays.asList(mockEvent("A"), mockEvent("B"), mockEvent("C"), mockEvent("D"), mockEvent("E")));
        EasyMock.expect(scheduler.getPeriodicEventIndex("D")).andReturn(3);
        EasyMock.replay(backupManager, scheduler);
        return backupManager;
    }

    private PeriodicEvent mockEvent(final String id) {
        final PeriodicEvent event = EasyMock.createMock(PeriodicEvent.class);
        EasyMock.expect(event.getEventId()).andReturn(id).anyTimes();
        EasyMock.expect(event.getHours()).andReturn(5);
        EasyMock.expect(event.getStartTime()).andReturn(DateTimeUtils.convertToString(getDateTime(LocalDateTime.of(2020, 1, 2, 3, 4, 5)))).anyTimes();
        EasyMock.expect(event.getMinutes()).andReturn(10).anyTimes();
        EasyMock.expect(event.getWeeks()).andReturn(0).anyTimes();
        EasyMock.expect(event.getDays()).andReturn(0).anyTimes();
        EasyMock.expect(event.getBackupManagerId()).andReturn("666").anyTimes();
        EasyMock.expect(event.getStopTime()).andReturn(DateTimeUtils.convertToString(getDateTime(LocalDateTime.of(2021, 1, 2, 3, 4, 5))));
        EasyMock.replay(event);
        return event;
    }

    private OffsetDateTime getDateTime(final LocalDateTime dateTime) {
        final ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(dateTime);
        return OffsetDateTime.of(dateTime, offset);
    }

}
