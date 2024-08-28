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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.archive.StreamingArchiveService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.ericsson.adp.mgmt.backupandrestore.util.OrchestratorDataFileService;

/**
 * Responsible for writing/reading periodic event to/from files.
 */
@Service
public class PeriodicEventFileService extends OrchestratorDataFileService<PeriodicEvent> {
    private static final String PERIODIC_EVENTS_FOLDER = "periodic-events";
    private static final Logger log = LogManager.getLogger(PeriodicEventFileService.class);

    private final List<Version<PeriodicEvent>> versions = List.of(getDefaultVersion(
        this::parseJsonStringToPersistedObject,
        p -> p.getFileName().toString().endsWith(JSON_EXTENSION)
    ));

    /**
     * Writes a periodic event to file.
     * @param periodicEvent to be written.
     */
    public void writeToFile(final PeriodicEvent periodicEvent) {
        final Path eventsFolder = getEventsFolder(periodicEvent.getBackupManagerId());
        final Path eventFile = eventsFolder.resolve(getFile(periodicEvent.getEventId()));
        if (periodicEvent.getVersion() == null) {
            periodicEvent.setVersion(getLatestVersion());
        }
        writeFile(eventsFolder, eventFile, periodicEvent.toJson().getBytes(), periodicEvent.getVersion());
    }

    /**
     * Gets persisted periodic event from a backupManager.
     * @param backupManagerId owner of periodic event.
     * @return list of periodic events.
     */
    public List<PeriodicEvent> getEvents(final String backupManagerId) {
        final Path eventsFolder = getEventsFolder(backupManagerId);
        if (exists(eventsFolder)) {
            final List<PeriodicEvent> periodicEvents = readObjectsFromFiles(eventsFolder);
            for (final PeriodicEvent e: periodicEvents) {
                e.setBackupManagerId(backupManagerId);
                e.setPersistFunction(this::writeToFile);
            }
            return periodicEvents;
        }
        return new ArrayList<>();
    }

    /**
     * Backup periodic-events in backup
     * @param backupManager of backupManager to fetch periodic events from.
     * @param storeLocation path of Backup
     * @param gzOut tar output stream
     */
    public void backupPeriodicEvents(final BackupManager backupManager, final Path storeLocation, final TarArchiveOutputStream gzOut) {
        final List<PeriodicEvent> periodicEvents = backupManager.getScheduler().getPeriodicEvents();
        periodicEvents.forEach( event -> StreamingArchiveService.passDataToTarOutputStreamLocation(storeLocation, gzOut,
                PERIODIC_EVENTS_FOLDER + File.separator + event.getEventId() + ".json", event.toJson()));
    }

    private Path getEventsFolder(final String backupManagerId) {
        return backupManagersLocation.resolve(backupManagerId).resolve(PERIODIC_EVENTS_FOLDER);
    }


    @Override
    protected List<Version<PeriodicEvent>> getVersions() {
        return versions;
    }

    /**
     * This method parses the persisted periodic events on start-up.
     * It updates the timezone in the startTime and stopTime timestamps using the timezone block.
     * If a timestamp already has a TZ, this TZ is ignored and replaced with the ones in the timezone block.
     * Otherwise, the TZ info in the timezone block is appended to the parsed timestamps.
     * @param jsonString the persisted periodic event
     * @return the Periodic Event object
     */
    private Optional<PeriodicEvent> parseJsonStringToPersistedObject(final String jsonString) {
        final Optional<PeriodicEvent> event = jsonService.parseJsonString(jsonString, PeriodicEvent.class);
        final String timeZone = getTimeZone(jsonString);
        if (event.isPresent() && (timeZone != null)) {
            final String[] timeZoneInfo = timeZone.split(",");
            final PeriodicEvent periodicEvent = event.get();
            periodicEvent.setStartTime(DateTimeUtils.offsetDateTimeFrom(periodicEvent.getStartTime(), ZoneOffset.of(timeZoneInfo[0])));
            if ((periodicEvent.getStopTime() != null) && (timeZoneInfo.length == 2)) {
                periodicEvent.setStopTime(DateTimeUtils.offsetDateTimeFrom(periodicEvent.getStopTime(), ZoneOffset.of(timeZoneInfo[1])));
            }
        }
        return event;
    }


    /**
     * Delete the event file on disk.
     * @param event - the event whose file is to be deleted
     * @return true if file deleted, false otherwise
     * */
    public boolean deleteEvent(final PeriodicEvent event) {
        final Path eventsFolder = getEventsFolder(event.getBackupManagerId());
        Path eventFile = eventsFolder.resolve(getFile(event.getEventId()));
        eventFile = event.getVersion().fromBase(eventFile);
        try {
            delete(eventFile);
        } catch (IOException e) {
            log.debug("eventFile path <{}> does not exist", eventFile);
            return false;
        }
        return true;
    }

    /**
     * Replace the event file on disk.
     * @param backupManagerId - the backupManagerId
     * @param oldEventId - the event id whose file is to be replaced
     * @param newEventId - the event id whose file is to be updated
     * @return true if file replaced, false otherwise
     * */
    public boolean replaceEvent(final String backupManagerId, final String oldEventId, final String newEventId) {
        final Path eventsFolder = getEventsFolder(backupManagerId);
        final Path eventFile = eventsFolder.resolve(getFile(oldEventId));
        try {
            Files.move(eventFile, eventFile.resolveSibling(getFile(newEventId)));
        } catch (IOException e) {
            log.debug("eventFile path <{}> does not exist", eventFile);
            return false;
        }
        return true;
    }

    /**
     * Create the periodic-events directory in a backup-manager
     * @param backupManager the backup manager
     * @return true if the directory is created, false otherwise.
     */
    public boolean createPeriodicEventsDirectory(final BackupManager backupManager) {
        final Path eventsFolder = getEventsFolder(backupManager.getBackupManagerId());
        return mkdirs(eventsFolder);
    }

}
