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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventScheduler;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidIdException;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnprocessableEntityException;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.PeriodicEventRequestOrResponse;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.ericsson.adp.mgmt.backupandrestore.util.ESAPIValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ScheduledEventHandlerTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path fileLocation;
    private ScheduledEventHandler eventHandler;
    private BackupManager backupManager;
    private PeriodicEventFileService fileService;
    private PeriodicEventScheduler periodicEventScheduler;
    private CMMediatorService cmMediatorService;
    private IdValidator validator;
    private ESAPIValidator esapiValidator;

    @Before
    public void setUp() {
        fileLocation = Paths.get(folder.getRoot().getAbsolutePath());
        eventHandler = new ScheduledEventHandler();
        periodicEventScheduler = createMock(PeriodicEventScheduler.class);
        cmMediatorService = createMock(CMMediatorService.class);
        backupManager = mockBackupManager("123");
        fileService = new PeriodicEventFileService();
        validator = createMock(IdValidator.class);
        esapiValidator = createMock(ESAPIValidator.class);
        fileService.setJsonService(new JsonService());
        fileService.setBackupManagersLocation(fileLocation.toAbsolutePath().toString());
        eventHandler.setPeriodicEventFileService(fileService);
        eventHandler.setPeriodicEventScheduler(periodicEventScheduler);
        eventHandler.setCmMediatorService(cmMediatorService);
        eventHandler.setEsapiValidator(esapiValidator);
        eventHandler.setIdValidator(validator);
    }

    @Test
    public void createPeriodicEvent_validRequest_persistPeriodicEvent() throws Exception {
        final String endTime = "9020-03-01T01:12:45.456";
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        final OffsetDateTime startTime = OffsetDateTime.now(ZoneId.systemDefault());
        request.setHours(2);
        request.setMinutes(3);
        request.setDays(0);
        request.setWeeks(0);
        request.setStartTime(DateTimeUtils.convertToString(startTime));
        request.setStopTime(endTime);

        cmMediatorService.addPeriodicEvent(anyObject());
        expectLastCall();
        periodicEventScheduler.scheduleEvent(anyObject());
        expectLastCall();
        replay(periodicEventScheduler, cmMediatorService);

        final String eventId = eventHandler.createPeriodicEvent(backupManager, request, true);
        assertNotNull(eventId);
        final Path file = fileLocation.resolve("123").resolve("periodic-events").resolve(eventId + ".json");
        assertTrue(file.toFile().exists());
        final String fileContents = Files.readAllLines(file).stream().collect(Collectors.joining(""));

        final PeriodicEvent persistedEvent = new ObjectMapper().readValue(fileContents, PeriodicEvent.class);
        assertEquals(eventId, persistedEvent.getEventId());
        assertEquals(request.getHours().intValue(), persistedEvent.getHours());
        assertEquals(request.getMinutes().intValue(), persistedEvent.getMinutes());
        // Verify the time stamps in the persisted event do not have a timezone component
        assertTimeStampsInLocalDateTimeFormat(persistedEvent);
        assertEquals(DateTimeUtils.convertToString(startTime.toLocalDateTime()), persistedEvent.getStartTime());
        assertEquals(DateTimeUtils.getLocalDateTime(endTime), persistedEvent.getStopTime());
        verify(periodicEventScheduler);
    }

    @Test
    public void createPeriodicEvent_noStartDateSpecified_startTimeUpdated() throws Exception {
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setHours(1);
        request.setEventId("Id1");

        cmMediatorService.addPeriodicEvent(anyObject());
        expectLastCall();
        periodicEventScheduler.scheduleEvent(anyObject());
        expectLastCall();
        replay(periodicEventScheduler, cmMediatorService);

        final String eventId = eventHandler.createPeriodicEvent(backupManager, request, true);
        assertNotNull(eventId);
        final Path file = fileLocation.resolve("123").resolve("periodic-events").resolve(eventId + ".json");
        assertTrue(file.toFile().exists());
        final String fileContents = Files.readAllLines(file).stream().collect(Collectors.joining(""));

        final PeriodicEvent persistedEvent = new ObjectMapper().readValue(fileContents, PeriodicEvent.class);
        assertEquals(eventId, persistedEvent.getEventId());
        assertEquals(request.getHours().intValue(), persistedEvent.getHours());
        assertEquals("Id1", persistedEvent.getEventId());
        // Verify the time stamps in the persisted event do not have a timezone component
        assertTimeStampsInLocalDateTimeFormat(persistedEvent);
        assertNotNull(persistedEvent.getStartTime());
        verify(periodicEventScheduler);
    }

    @Test
    public void schedulePeriodicEvents_noPersistedEvent_noEventsScheduled() throws Exception {
        replay(periodicEventScheduler, cmMediatorService);
        eventHandler.schedulePeriodicEvents(new Scheduler("123", this::persist));
        verify(periodicEventScheduler);
    }

    @Test
    public void schedulePeriodicEvents_hasPersistedEventWithoutStopTime_schedulesEvents() throws Exception {
        final PeriodicEvent event = new PeriodicEvent("123", null);
        event.setEventId("event1");
        event.setStartTime(DateTimeUtils.convertToString(OffsetDateTime.now()));
        event.setMinutes(10);

        fileService.writeToFile(event);
        cmMediatorService.addPeriodicEvent(anyObject());
        expectLastCall();
        periodicEventScheduler.scheduleEvent(anyObject());
        expectLastCall();
        replay(periodicEventScheduler, cmMediatorService);
        eventHandler.schedulePeriodicEvents(new Scheduler("123", this::persist));
        assertTimeStampsInOffsetDateTimeFormat(event);
        verify(periodicEventScheduler);
    }

    @Test
    public void schedulePeriodicEvents_hasPersistedEventWithStopTimeElapsed_noEventsScheduled() throws Exception {
        final PeriodicEvent event = new PeriodicEvent("123", null);
        event.setEventId("event1");
        event.setStartTime(DateTimeUtils.convertToString(OffsetDateTime.now().minusMinutes(1)));
        event.setStopTime(DateTimeUtils.convertToString(OffsetDateTime.now().minusSeconds(30)));

        fileService.writeToFile(event);
        cmMediatorService.addPeriodicEvent(anyObject());
        expectLastCall();
        replay(periodicEventScheduler, cmMediatorService);
        eventHandler.schedulePeriodicEvents(new Scheduler("123", this::persist));
        assertTimeStampsInOffsetDateTimeFormat(event);
        verify(periodicEventScheduler);
    }

    @Test
    public void schedulePeriodicEvents_hasPersistedEventWithStopTime_scheduledEvent() throws Exception {
        final PeriodicEvent event = new PeriodicEvent("123", null);
        event.setEventId("event1");
        event.setStartTime(DateTimeUtils.convertToString(OffsetDateTime.now().plusMinutes(1)));
        event.setStopTime(DateTimeUtils.convertToString(OffsetDateTime.now().plusMinutes(30)));
        event.setMinutes(10);

        fileService.writeToFile(event);
        cmMediatorService.addPeriodicEvent(anyObject());
        expectLastCall();
        periodicEventScheduler.scheduleEvent(anyObject());
        expectLastCall();
        replay(periodicEventScheduler, cmMediatorService);
        eventHandler.schedulePeriodicEvents(new Scheduler("123", this::persist));
        assertTimeStampsInOffsetDateTimeFormat(event);
        verify(periodicEventScheduler);
    }

    @Test(expected=UnprocessableEntityException.class)
    public void createPeriodicEvent_outOfRangeUInt16_throwsException() {
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setHours(2);
        request.setMinutes(65536);

        assertNotNull(eventHandler.createPeriodicEvent(backupManager, request, true));
    }

    @Test(expected=UnprocessableEntityException.class)
    public void createPeriodicEvent_invalidUInt16Value_throwsException() {
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setHours(2);
        request.setMinutes(3);
        request.setWeeks(-1);

        assertNotNull(eventHandler.createPeriodicEvent(backupManager, request, false));
    }

    @Test(expected=UnprocessableEntityException.class)
    public void createPeriodicEvent_missingHours_throwsException() {
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setMinutes(3);
        request.setDays(4);
        request.setWeeks(6);

        assertNotNull(eventHandler.createPeriodicEvent(backupManager, request, true));
    }

    @Test(expected=DateTimeParseException.class)
    public void createPeriodicEvent_invalidDate_throwsException() {
        //9021 is not a leap year
        final String startTime = "9021-02-40T15:02:34.123Z";
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setHours(2);
        request.setDays(4);
        request.setStartTime(startTime);

        assertNotNull(eventHandler.createPeriodicEvent(backupManager, request, true));
    }

    @Test
    public void createPeriodicEvent_validDate_periodicEvent() {
        //9020 is a leap year
        final OffsetDateTime startTime = getDateTime(9020, 02, 29, 11, 45, 03);
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setHours(2);
        request.setDays(4);
        request.setStartTime(DateTimeUtils.convertToString(startTime));

        cmMediatorService.addPeriodicEvent(anyObject());
        expectLastCall();
        periodicEventScheduler.scheduleEvent(anyObject());
        expectLastCall();
        replay(periodicEventScheduler, cmMediatorService);
        assertNotNull(eventHandler.createPeriodicEvent(backupManager, request, true));

        verify(periodicEventScheduler);
    }


    @Test
    public void updatePeriodicEvent_validDate_periodicEvent() {
        final OffsetDateTime startTime = getDateTime(9020, 02, 29, 11, 45, 03);
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        final Scheduler scheduler = createMock(Scheduler.class);
        final BackupManager backupManager = createMock(BackupManager.class);
        createPeriodicEvent(backupManager, request, scheduler, startTime, Optional.of(startTime.plusHours(2)));
        String eventId=eventHandler.createPeriodicEvent(backupManager, request, true);
        request.setHours(3);
        request.setDays(5);
        eventHandler.updatePeriodicEvent(backupManager, eventId, request);
    }

    @Test
    public void updatePeriodicEvent_eventId_periodicEvent() {
        final BackupManager backupManager = createMock(BackupManager.class);
        final Scheduler scheduler = createMock(Scheduler.class);
        final PeriodicEvent mockedEvent = createMock(PeriodicEvent.class);
        final String newEventId = "newEventId";
        expect(mockedEvent.getEventId()).andReturn("eventId").once();
        mockedEvent.setEventId(newEventId);
        expectLastCall().once();
        expect(backupManager.getBackupManagerId()).andReturn("123").once();
        expect(backupManager.getScheduler()).andReturn(scheduler).times(4);
        mockedEvent.persist();
        expectLastCall().once();

        replay(mockedEvent, backupManager);
        eventHandler.updatePeriodicEventId(backupManager, mockedEvent, newEventId);

        verify(mockedEvent, backupManager);
    }

    @Test
    public void updatePeriodicEvent_emptyStopTime_periodicEvent() {
        final OffsetDateTime startTime = getDateTime(9020, 02, 29, 11, 45, 03);
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        final Scheduler scheduler = createMock(Scheduler.class);
        final BackupManager backupManager = createMock(BackupManager.class);

        createPeriodicEvent(backupManager, request, scheduler, startTime, Optional.empty());
        String eventId=eventHandler.createPeriodicEvent(backupManager, request, false);
        request.setHours(3);
        request.setDays(5);
        request.setStopTime("");
        eventHandler.updatePeriodicEvent(backupManager, eventId, request);
    }

    @Test
    public void updatePeriodicEvent_validStopTime_periodicEvent() {
        final OffsetDateTime startTime = getDateTime(2021, 02, 20, 11, 45, 03);
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        final Scheduler scheduler = createMock(Scheduler.class);
        final BackupManager backupManager = createMock(BackupManager.class);
        createPeriodicEvent(backupManager, request, scheduler, startTime, Optional.empty());
        String eventId=eventHandler.createPeriodicEvent(backupManager, request, true);
        request.setHours(3);
        request.setDays(5);
        request.setStopTime("2021-02-21T10:52:59Z");
        eventHandler.updatePeriodicEvent(backupManager, eventId, request);
    }

    @Test(expected=UnprocessableEntityException.class)
    public void updatePeriodicEvent_invalidStopTime_periodicEvent() {
        final OffsetDateTime startTime = getDateTime(2021, 03, 20, 11, 45, 03);
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        final Scheduler scheduler = createMock(Scheduler.class);
        final BackupManager backupManager = createMock(BackupManager.class);

        createPeriodicEvent(backupManager, request, scheduler, startTime, Optional.empty());
        
        String eventId=eventHandler.createPeriodicEvent(backupManager, request, false);
        request.setHours(3);
        request.setDays(5);
        // Stop time if set before the start time
        request.setStopTime("2021-02-21T10:52:59Z");
        eventHandler.updatePeriodicEvent(backupManager, eventId, request);
    }

    @Test(expected=UnprocessableEntityException.class)
    public void updatePeriodicEvent_emptyStartTime_periodicEvent() {
        final OffsetDateTime startTime = getDateTime(2021, 03, 20, 11, 45, 03);
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        final Scheduler scheduler = createMock(Scheduler.class);
        final BackupManager backupManager = createMock(BackupManager.class);

        createPeriodicEvent(backupManager, request, scheduler, startTime, Optional.empty());

        String eventId=eventHandler.createPeriodicEvent(backupManager, request, true);
        request.setHours(3);
        request.setDays(5);
        request.setStopTime("2021-02-21T10:52:59Z");
        request.setStartTime("");
        eventHandler.updatePeriodicEvent(backupManager, eventId, request);
    }

    @Test(expected= DateTimeParseException.class)
    public void createPeriodicEvent_invalidTime_throwsException() {
        //26th hour
        final String startTime = "9020-03-01T26:12:45.456Z";
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setHours(2);
        request.setDays(4);
        request.setStartTime(startTime);

        assertNotNull(eventHandler.createPeriodicEvent(backupManager, request, false));
    }

    @Test(expected=UnprocessableEntityException.class)
    public void createPeriodicEvent_invalidStopTime_throwsException() {
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setHours(2);
        request.setDays(4);
        request.setStopTime(DateTimeUtils.convertToString(OffsetDateTime.now(ZoneId.systemDefault()).minusHours(1)));

        assertNotNull(eventHandler.createPeriodicEvent(backupManager, request, true));
    }

    @Test(expected = InvalidIdException.class)
    public void createPeriodicEvent_invalidId_throwsException() {

        validator.validateId("");
        expectLastCall().andThrow(new InvalidIdException(""));
        replay(validator);

        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setEventId("");
        request.setHours(2);
        request.setDays(4);

        assertNotNull(eventHandler.createPeriodicEvent(backupManager, request, false));
    }

    @Test(expected = InvalidIdException.class)
    public void createPeriodicEvent_invalidEventId_throwsException() {

        esapiValidator.validateEventId("event&Id");
        expectLastCall().andThrow(new InvalidIdException("event&Id"));
        replay(esapiValidator);

        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setEventId("event&Id");
        request.setHours(2);
        request.setDays(4);

        assertNotNull(eventHandler.createPeriodicEvent(backupManager, request, true));
    }

    @Test(expected = InvalidIdException.class)
    public void createPeriodicEvent_existingEventId_throwsException() {

        final BackupManager backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn("123").anyTimes();

        final PeriodicEvent event = createMock(PeriodicEvent.class);
        expect(event.getEventId()).andReturn("eventId");
        final List<PeriodicEvent> eventList = new ArrayList<>();
        eventList.add(event);

        final Scheduler scheduler = createMock(Scheduler.class);
        expect(scheduler.getPeriodicEvents()).andReturn(eventList).anyTimes();
        expect(backupManager.getScheduler()).andReturn(scheduler).anyTimes();
        replay(backupManager, scheduler, event);

        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setEventId("eventId");
        request.setHours(3);
        request.setDays(4);
        eventHandler.createPeriodicEvent(backupManager, request, false);
    }

    @Test
    public void createPeriodicEvent_periodOfZero_storedButNotScheduled() {
        final PeriodicEventRequestOrResponse request = new PeriodicEventRequestOrResponse();
        request.setHours(0);
        cmMediatorService.addPeriodicEvent(anyObject());
        expectLastCall();
        // Don't expect() a call to ::scheduleEvent(), since a periodic event with period=0 shouldn't be scheduled but should be stored
        replay(periodicEventScheduler, cmMediatorService);
        final String eventId = eventHandler.createPeriodicEvent(backupManager, request, true);
        // Assert the event it persisted despite not being scheduled
        final List<PeriodicEvent> persistedEvents = fileService.getEvents(backupManager.getBackupManagerId());
        persistedEvents.forEach(this::assertTimeStampsInOffsetDateTimeFormat);
        assertTrue(persistedEvents.stream().anyMatch(e -> e.getEventId().equals(eventId)));
    }

    private void assertTimeStampsInOffsetDateTimeFormat(final PeriodicEvent persistedEvent) {
        OffsetDateTime.parse(persistedEvent.getStartTime(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        if(persistedEvent.getStopTime() != null && !persistedEvent.getStopTime().isEmpty()) {
            OffsetDateTime.parse(persistedEvent.getStopTime(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }

    private void assertTimeStampsInLocalDateTimeFormat(final PeriodicEvent persistedEvent) {
        LocalDateTime.parse(persistedEvent.getStartTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        if(persistedEvent.getStopTime() != null && !persistedEvent.getStopTime().isEmpty()) {
            LocalDateTime.parse(persistedEvent.getStopTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    private void createPeriodicEvent(final BackupManager backupManager, PeriodicEventRequestOrResponse request,
                                     final Scheduler scheduler,
                                     OffsetDateTime startTime,
                                     Optional<OffsetDateTime> stopTime) {
        final PeriodicEvent event = createMock(PeriodicEvent.class);
        final List<PeriodicEvent> eventList = new ArrayList<>();
        expect(backupManager.getBackupManagerId()).andReturn("123").anyTimes();
        eventList.add(event);

        expect(event.getEventId()).andReturn("eventId").anyTimes();
        expect(event.getStopTime()).andReturn(stopTime.map(DateTimeUtils::convertToString).orElse(null));
        expect(event.periodInSeconds()).andReturn(352800L);
        expect(event.getStartTime()).andReturn(DateTimeUtils.convertToString(startTime)).anyTimes();

        expect(scheduler.getPeriodicEvent(anyString())).andReturn(event).anyTimes();
        scheduler.addPeriodicEvent(anyObject());
        expectLastCall();
        expect(scheduler.getPeriodicEvents()).andReturn(eventList).anyTimes();
        expect(backupManager.getScheduler()).andReturn(scheduler).anyTimes();

        request.setHours(2);
        request.setDays(4);
        request.setStartTime(DateTimeUtils.convertToString(startTime));
        cmMediatorService.addPeriodicEvent(anyObject());
        expectLastCall();
        cmMediatorService.updatePeriodicEvent(anyObject());
        expectLastCall().anyTimes();

        periodicEventScheduler.scheduleEvent(anyObject());
        expectLastCall().anyTimes();

        event.persist();
        expectLastCall();

        event.mergeWith(request,false);
        expectLastCall();

        replay(periodicEventScheduler, cmMediatorService, backupManager, scheduler, event);
    }

    private BackupManager mockBackupManager(final String backupManagerId) {
        final BackupManager backupManager = createMock(BackupManager.class);
        expect(backupManager.getBackupManagerId()).andReturn(backupManagerId).anyTimes();

        final Scheduler scheduler = createMock(Scheduler.class);
        expect(scheduler.getAdminState()).andReturn(AdminState.UNLOCKED).anyTimes();
        expect(scheduler.getPeriodicEvents()).andReturn(new ArrayList<PeriodicEvent>()).anyTimes();
        expect(backupManager.getScheduler()).andReturn(scheduler).anyTimes();
        scheduler.addPeriodicEvent(anyObject(PeriodicEvent.class));
        expectLastCall();
        replay(backupManager, scheduler);
        return backupManager;
    }

    private OffsetDateTime getDateTime(final int year, final int month, final int dayOfMonth, final int hour, final int minute, final int second) {
        final LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second);
        final ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(dateTime);
        return OffsetDateTime.of(dateTime, offset);
    }

    private void persist(final Scheduler scheduler) {
        //do nothing
    }
}
