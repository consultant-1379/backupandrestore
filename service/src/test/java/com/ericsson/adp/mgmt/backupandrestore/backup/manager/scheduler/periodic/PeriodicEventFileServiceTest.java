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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PeriodicEventFileServiceTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path fileLocation;
    private PeriodicEventFileService fileService;
    private static final OffsetDateTime START_TIME = OffsetDateTime.now(ZoneId.of("UTC+00:00"));
    private static final OffsetDateTime STOP_TIME = OffsetDateTime.now(ZoneId.of("UTC-01:00"));

    @Before
    public void setup() throws Exception {
        fileLocation = Paths.get(folder.getRoot().getAbsolutePath());

        fileService = new PeriodicEventFileService();
        fileService.setJsonService(new JsonService());
        fileService.setBackupManagersLocation(fileLocation.toAbsolutePath().toString());
    }

    @Test
    public void writeToFile_event_persistPeriodicEvent() throws Exception {
        final PeriodicEvent event = getEvent("id1", "123");
        fileService.writeToFile(event);

        final Path file = fileLocation.resolve("123").resolve("periodic-events").resolve(event.getEventId() + ".json");
        assertTrue(file.toFile().exists());

        final String fileContents = Files.readAllLines(file).stream().collect(Collectors.joining(""));

        final PeriodicEvent persistedEvent = new ObjectMapper().readValue(fileContents, PeriodicEvent.class);
        assertEquals("id1", persistedEvent.getEventId());
        assertEquals(DateTimeUtils.getLocalDateTime(DateTimeUtils.convertToString(START_TIME)), persistedEvent.getStartTime());
        assertEquals(DateTimeUtils.getLocalDateTime(DateTimeUtils.convertToString(STOP_TIME)), persistedEvent.getStopTime());
    }

    @Test
    public void getEvents_backupManagerIdWithoutPersistedFiles_emptyList() throws Exception {
        final String backupManagerId = "qwe";

        Files.createDirectories(fileLocation.resolve(backupManagerId).resolve("periodic-events"));

        final List<PeriodicEvent> events = fileService.getEvents(backupManagerId);

        assertTrue(events.isEmpty());
    }

    @Test
    public void getEvents_backupManagerIdWithoutAnyFolder_emptyList() throws Exception {
        final String backupManagerId = "123";

        final List<PeriodicEvent> events = fileService.getEvents(backupManagerId);

        assertTrue(events.isEmpty());
    }

    @Test
    public void getEvents_validAndInvalidFiles_onlyReadsValidFiles() throws Exception {
        final String backupManagerId = "123";
        final PeriodicEvent event = getEvent("id2", backupManagerId);
        fileService.writeToFile(event);

        Files.write(fileLocation.resolve(backupManagerId).resolve("periodic-events").resolve("y.json"), "".getBytes());

        final List<PeriodicEvent> events = fileService.getEvents(backupManagerId);

        assertEquals(1, events.size());
        assertEquals("id2", events.get(0).getEventId());
        assertEquals(DateTimeUtils.convertToString(START_TIME), events.get(0).getStartTime());
        assertEquals(DateTimeUtils.convertToString(STOP_TIME), events.get(0).getStopTime());
    }

    @Test
    public void getEvents_backupManagerIdAndPersistedFiles_eventsWithInformationReadFromFile() throws Exception {
        final String backupManagerId = "qwe";
        final PeriodicEvent event = getEvent("id3", backupManagerId);
        fileService.writeToFile(event);
        fileService.writeToFile(getEvent("id4", backupManagerId));

        final List<PeriodicEvent> events = fileService.getEvents(backupManagerId);

        assertEquals(2, events.size());

        final PeriodicEvent obtainedEvent = events.stream().filter(persistedEvent -> event.getEventId().equals(persistedEvent.getEventId()))
                .findFirst().get();
        assertEquals(event.getEventId(), obtainedEvent.getEventId());
        assertEquals(DateTimeUtils.convertToString(START_TIME), obtainedEvent.getStartTime());
        assertEquals(DateTimeUtils.convertToString(STOP_TIME), obtainedEvent.getStopTime());
    }

    @Test
    public void deletePeriodicEventTest() {
        // Create events
        final PeriodicEvent event1 = getEvent("id1", "123");
        fileService.writeToFile(event1);
        final PeriodicEvent event2 = getEvent("id2", "123");
        fileService.writeToFile(event2);

        // Check they're on disk
        final Path file1 = fileLocation.resolve("123").resolve("periodic-events").resolve(event1.getEventId() + ".json");
        final Path file2 = fileLocation.resolve("123").resolve("periodic-events").resolve(event2.getEventId() + ".json");
        assertTrue(file1.toFile().exists());
        assertTrue(file2.toFile().exists());

        //Delete event 1
        fileService.deleteEvent(event1);
        //Verify event 1 is deleted, and event 2 isn't
        assertFalse(file1.toFile().exists());
        assertTrue(file2.toFile().exists());
    }

    @Test
    public void replacePeriodicEventTest() {
        // Create events
        final PeriodicEvent event1 = getEvent("id1", "123");
        fileService.writeToFile(event1);

        // Check event 1 (id1) on disk
        final Path file1 = fileLocation.resolve("123").resolve("periodic-events").resolve(event1.getEventId() + ".json");
        assertTrue(file1.toFile().exists());

        //replace event id from id1 to id2
        fileService.replaceEvent("123", "id1", "id2");
        //Verify event with id2 exist and event with id1 not
        final Path file2 = fileLocation.resolve("123").resolve("periodic-events").resolve("id2.json");
        assertFalse(file1.toFile().exists());
        assertTrue(file2.toFile().exists());
    }

    private PeriodicEvent getEvent(final String eventId, final String backupManagerId) {
        final PeriodicEvent event = new PeriodicEvent(backupManagerId, null);
        event.setEventId(eventId);
        event.setStartTime(DateTimeUtils.convertToString(START_TIME));
        event.setStopTime(DateTimeUtils.convertToString(STOP_TIME));
        return event;
    }
}
