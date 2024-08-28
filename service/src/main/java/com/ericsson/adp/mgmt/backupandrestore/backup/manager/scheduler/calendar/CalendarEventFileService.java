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

import com.ericsson.adp.mgmt.backupandrestore.archive.StreamingArchiveService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.ericsson.adp.mgmt.backupandrestore.util.OrchestratorDataFileService;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Responsible for writing/reading calendar event to/from files.
 */
@Service
public class CalendarEventFileService extends OrchestratorDataFileService<CalendarEvent> {
    private static final String CALENDAR_EVENTS_FOLDER = "calendar-events";

    private final List<Version<CalendarEvent>> versions = List.of(getDefaultVersion(
        this::parseJsonStringToCalendarEvent,
        p -> p.getFileName().toString().endsWith(JSON_EXTENSION)
    ));

    /**
    * This method parses the persisted calendar events on start-up.
    * It updates the timezone in the startTime and stopTime timestamps using the timezone block.
    * If a timestamp already has a TZ, this TZ is ignored and replaced with the ones in the timezone block.
    * Otherwise, the TZ info in the timezone block is appended to the parsed timestamps.
    * @param jsonString the persisted periodic event
    * @return the Periodic Event object
    */
    private Optional<CalendarEvent> parseJsonStringToCalendarEvent(final String jsonString) {
        final Optional<CalendarEvent> event = jsonService.parseJsonString(jsonString, CalendarEvent.class);
        final String timeZone = getTimeZone(jsonString);
        if (event.isPresent() && (timeZone != null)) {
            final String[] timeZoneInfo = timeZone.split(",");
            final CalendarEvent periodicEvent = event.get();
            periodicEvent.setStartTime(DateTimeUtils.offsetDateTimeFrom(periodicEvent.getStartTime(), ZoneOffset.of(timeZoneInfo[0])));
            if ((periodicEvent.getStopTime() != null) && (timeZoneInfo.length == 2)) {
                periodicEvent.setStopTime(DateTimeUtils.offsetDateTimeFrom(periodicEvent.getStopTime(), ZoneOffset.of(timeZoneInfo[1])));
            }
        }
        return event;
    }

    /**
     * Writes a calendar event to file.
     * @param event to be written.
     */
    public void writeToFile(final CalendarEvent event) {
        final Path eventsFolder = getEventsFolder(event.getBackupManagerId());
        final Path eventFile = eventsFolder.resolve(getFile(event.getEventId()));
        if (event.getVersion() == null) {
            event.setVersion(getLatestVersion());
        }
        writeFile(eventsFolder, eventFile, event.toJson().getBytes(), event.getVersion());
    }

    private Path getEventsFolder(final String backupManagerId) {
        return backupManagersLocation.resolve(backupManagerId).resolve(CALENDAR_EVENTS_FOLDER);
    }


    /**
     * Gets persisted periodic event from a backupManager.
     * @param backupManagerId owner of periodic event.
     * @return list of periodic events.
     */
    public List<CalendarEvent> getEvents(final String backupManagerId) {
        final Path eventsFolder = getEventsFolder(backupManagerId);
        if (exists(eventsFolder)) {
            final List<CalendarEvent> calendarEvents = readObjectsFromFiles(eventsFolder);
            for (final CalendarEvent e : calendarEvents) {
                e.setBackupManagerId(backupManagerId);
                e.setPersistFunction(this::writeToFile);
            }
            return calendarEvents;
        }
        return new ArrayList<>();
    }

    @Override
    protected List<Version<CalendarEvent>> getVersions() {
        return versions;
    }

    /**
     * Delete the event file on disk
     * @param event  the event whose file is to be deleted
     * @return true if file deleted, false otherwise
     */
    public boolean deleteEvent(final CalendarEvent event) {
        final Path eventsFolder = getEventsFolder(event.getBackupManagerId());
        Path eventFile = eventsFolder.resolve(getFile(event.getEventId()));
        eventFile = event.getVersion().fromBase(eventFile);
        try {
            delete(eventFile);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Create the calendar-based events directory in a backup-manager
     * @param backupManager the backup manager
     * @return true if the directory is created, false otherwise.
     */
    public boolean createCalendarEventsDirectory(final BackupManager backupManager) {
        final Path eventsFolder = getEventsFolder(backupManager.getBackupManagerId());
        return mkdirs(eventsFolder);
    }

    /**
     * Backup calendar-based events in backup
     * @param backupManager the backupManager to fetch periodic events from.
     * @param pathInBackup path of Backup
     * @param gzOut tar output stream
     */
    public void backupCalendarEvents(final BackupManager backupManager, final Path pathInBackup, final TarArchiveOutputStream gzOut) {
        final List<CalendarEvent> calendarEvents = backupManager.getScheduler().getCalendarEvents();
        calendarEvents.forEach( event -> StreamingArchiveService.passDataToTarOutputStreamLocation(pathInBackup, gzOut,
                CALENDAR_EVENTS_FOLDER + File.separator + event.getEventId() + ".json", event.toJson()));
    }
}
