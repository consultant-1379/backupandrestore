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

import static com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils.convertToISOOffsetDateTime;
import static com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils.convertToString;
import static com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils.parseToOffsetDateTime;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.CalendarEvent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.CalendarEventFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.CalendarEventScheduler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.DayOfWeek;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.DayOfWeekOccurrence;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEventScheduler;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidActionException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidIdException;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnprocessableEntityException;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.PeriodicEventRequestOrResponse;
import com.ericsson.adp.mgmt.backupandrestore.rest.scheduler.V4CalendarEventRequest;
import com.ericsson.adp.mgmt.backupandrestore.util.CalendarEventValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.ESAPIValidator;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;


/**
 * Handles creation of Scheduled Events
 */
@Service
public class ScheduledEventHandler {

    private static final Integer MAX_NUM = 65535;
    private static final Logger log = LogManager.getLogger(ScheduledEventHandler.class);

    private final Random randomNumberGenerator = new Random();

    private PeriodicEventFileService periodicEventFileService;
    private CalendarEventFileService calendarEventFileService;
    private CMMediatorService cmMediatorService;
    private PeriodicEventScheduler periodicEventScheduler;
    private CalendarEventScheduler calendarEventScheduler;
    private CalendarEventValidator calendarEventValidator;
    private ESAPIValidator esapiValidator;
    private IdValidator idValidator;

    /**
     * Creates a periodic event and adds it to specified backup manager's scheduler
     * @param backupManager to schedule an event
     * @param request {@link PeriodicEventRequestOrResponse}
     * @param isRESTConfig indicates if the periodic event is configured via REST API
     * @return the periodic event id
     */
    public String createPeriodicEvent(final BackupManager backupManager, final PeriodicEventRequestOrResponse request,
                                      final boolean isRESTConfig) {
        final PeriodicEvent event = new PeriodicEvent(backupManager.getBackupManagerId(), this::persistPeriodicEvent);
        event.setEventId(getEventId(backupManager, request));
        // Assert hours is set. This is done here, rather than in Event::merge, as that is used in the update code, and
        // it is perfectly reasonable to, e.g., to update an event without setting the hours field, so we only assert
        // that the hours field is mandatory when creating events. The same logic applies to the null protection on
        // start time below
        if (request.getHours() == null) {
            log.error("Missing mandatory field: hours");
            throw new UnprocessableEntityException("Missing mandatory field: hours");
        }
        // Default to now() if start time isn't specified. This means that events with no specified start time will first
        // occur roughly 1 period from now(), since when they're scheduled (later in this code), now() will be a tiny
        // amount of time in the past, so the initial delay calculation will will be roughly 1 period
        if (request.getStartTime() == null) {
            request.setStartTime(convertToString(OffsetDateTime.now(ZoneId.systemDefault())));
        } else {
            request.setStartTime(convertToISOOffsetDateTime(request.getStartTime()));
        }
        if (request.getStopTime() != null) {
            request.setStopTime(convertToISOOffsetDateTime(request.getStopTime()));
        }
        event.mergeWith(request);
        try {
            validateEndTime(event.getStartTime(), event.getStopTime());
        } catch (UnprocessableEntityException e) {
            cmMediatorService.deletePeriodicEvent(event);
            throw e;
        }
        persistNewPeriodicEvent(backupManager, event, isRESTConfig);

        checkPeriodInSecondsAndSchedule(event);
        return event.getEventId();
    }

    /**
     * Create a calendar based event and add it to the backup manager's scheduler
     * @param backupManager to schedule an event
     * @param request request body
     * @return unique id of the event
     */
    public String createCalendarEvent(final BackupManager backupManager, final V4CalendarEventRequest request) {
        calendarEventValidator.validate(request);
        final CalendarEvent event = createCalendarEvent(backupManager, request, null);
        return event.getEventId();
    }

    /**
     * Create a calendar based event with a eventId and add it to the backup manager's scheduler
     * @param backupManager to schedule an event
     * @param request request body
     * @param eventId id of the event
     * @return the new event
     */
    public CalendarEvent createCalendarEvent(final BackupManager backupManager, final V4CalendarEventRequest request, final String eventId) {
        final CalendarEvent event = new CalendarEvent(backupManager.getBackupManagerId(), request, this::persistCalendarEvent);
        if (eventId == null) {
            event.setEventId(generateCalendarEventId(backupManager));
        } else {
            event.setEventId(eventId);
        }
        backupManager.getScheduler().addCalendarEvent(event);
        calendarEventScheduler.scheduleEvent(event);
        calendarEventFileService.writeToFile(event);
        return event;
    }

    /**
     * Update a calendar based event with a eventId and add it to the backup manager's scheduler
     * @param backupManager to schedule an event
     * @param request request body
     * @param eventId id of the event
     * @return the new event
     */
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
    public CalendarEvent updateCalendarEvent(final BackupManager backupManager, final V4CalendarEventRequest request, final String eventId) {
        final CalendarEvent event = backupManager.getScheduler().getCalendarEvent(eventId);
        calendarEventValidator.validatePatchRequest(request, event);
        final String startTime = request.getStartTime();
        if (startTime != null && !startTime.isBlank()) {
            log.info("Update the calendar based scheduler {} with startTime {}", eventId, startTime);
            event.setStartTime(convertToISOOffsetDateTime(startTime));
        }
        final String stopTime = request.getStopTime();
        if (stopTime != null && !stopTime.isBlank()) {
            log.info("Update the calendar based scheduler {} with stopTime {}", eventId, stopTime);
            event.setStopTime(convertToISOOffsetDateTime(stopTime));
        }
        final String time = request.getTime();
        if (time != null && !time.isBlank()) {
            log.info("Update the calendar based scheduler {} with time {}", eventId, time);
            event.setTime(time);
        }
        final DayOfWeekOccurrence dayOfWeekOccurrence = request.getDayOfWeekOccurrence();
        if (dayOfWeekOccurrence != null) {
            log.info("Update the calendar based scheduler {} with dayOfWeekOccurrence {}", eventId, dayOfWeekOccurrence);
            event.setDayOfWeekOccurrence(dayOfWeekOccurrence);
        }
        final DayOfWeek dayOfWeek = request.getDayOfWeek();
        if (dayOfWeek != null) {
            log.info("Update the calendar based scheduler {} with dayOfWeek {}", eventId, dayOfWeek);
            event.setDayOfWeek(dayOfWeek);
        }
        final Integer dayOfMonth = request.getDayOfMonth();
        if (dayOfMonth != null) {
            log.info("Update the calendar based scheduler {} with dayOfMonth {}", eventId, dayOfMonth);
            event.setDayOfMonth(dayOfMonth);
        }
        final Integer month = request.getMonth();
        if (month != null) {
            log.info("Update the calendar based scheduler {} with month {}", eventId, month);
            event.setMonth(month);
        }
        calendarEventScheduler.scheduleEvent(event);
        calendarEventFileService.writeToFile(event);
        return event;
    }

    /**
     * Schedule persisted periodic events on startup
     * @param scheduler instance to schedule persisted periodic events
     */
    public void schedulePeriodicEvents(final Scheduler scheduler) {
        getPeriodicEvents(scheduler.getBackupManagerId()).stream()
        .filter(events -> events.getStopTime() == null ||
                parseToOffsetDateTime(events.getStopTime()).isAfter(OffsetDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS)))
        .filter(event -> event.periodInSeconds() > 0)
        .forEach(event -> periodicEventScheduler.scheduleEvent(event));
        scheduler.updateNextScheduledTime();
        scheduler.persist();
    }

    /**
     * Schedule calendar events on startup
     * @param scheduler instance to schedule persisted periodic events
     */
    public void scheduleCalendarEvents(final Scheduler scheduler) {
        getCalendarEvents(scheduler.getBackupManagerId()).stream()
                .filter(event -> event.getDurationToNextTime() > 0)
                .forEach(event -> calendarEventScheduler.scheduleEvent(event));
        scheduler.updateNextScheduledTime();
        scheduler.persist();
    }

    /**
     * Update the EVENT_ID for periodic event.
     * @param manager - the backup manager this event belongs to
     * @param event - the periodic event which needs to be updated
     * @param newEventId - the new id of the event to be update
     * */
    public void updatePeriodicEventId(final BackupManager manager, final PeriodicEvent event, final String newEventId) {
        final String oldEventId = event.getEventId();
        event.setEventId(newEventId);
        periodicEventFileService.replaceEvent(manager.getBackupManagerId(), oldEventId, newEventId);
        event.persist();
        manager.getScheduler().replacePeriodicEvent(oldEventId, event);
        manager.getScheduler().removeScheduledBackups(oldEventId);
        manager.getScheduler().removeScheduledCancels(oldEventId);
        manager.getScheduler().persist();
    }

    /**
     * Update the periodic event with EVENT_ID id. All non-null values in update will be set as the equivalent values
     * in the event. Null values in update are treated as "don't touch", and have no effect on the event.
     * @param manager - the backup manager this event belongs to
     * @param eventId - the id of the event to be update
     * @param update - a request carrying the values to be merged into the
     * */
    public void updatePeriodicEvent(final BackupManager manager, final String eventId, final PeriodicEventRequestOrResponse update) {
        final PeriodicEvent event = manager.getScheduler().getPeriodicEvent(eventId);

        if (update.getStartTime() == null || update.getStartTime().isEmpty()) {
            update.setStartTime(convertToISOOffsetDateTime(event.getStartTime()));
        } else {
            update.setStartTime(convertToISOOffsetDateTime(update.getStartTime()));
        }

        final String endTime;
        if (update.getStopTime() == null) {
            endTime = event.getStopTime();
        } else if (update.getStopTime().isEmpty()) {
            endTime = null;
        } else {
            endTime = update.getStopTime();
            update.setStopTime(convertToISOOffsetDateTime(update.getStopTime()));
        }
        try {
            validateEndTime(update.getStartTime(), endTime);
            event.mergeWith(update, false);
        } catch (UnprocessableEntityException e) {
            // Push original values to CMM for consistency between REST and CMYP.
            cmMediatorService.updatePeriodicEvent(event);
            throw e;
        }
        checkPeriodInSecondsAndSchedule(event);
        event.persist();
    }

    private void checkPeriodInSecondsAndSchedule(final PeriodicEvent event) {
        if (event.periodInSeconds() > 0) {
            periodicEventScheduler.scheduleEvent(event);
            log.info("Periodic schedule <{}> persisted and scheduled.", event.getEventId());
        } else {
            log.info("Periodic schedule <{}> updated but not scheduled as defined period is 0 seconds.", event.getEventId());
        }
    }

    private String getEventId(final BackupManager backupManager, final PeriodicEventRequestOrResponse request) {
        if (backupManager.getScheduler().getPeriodicEvents().size() >= MAX_NUM) {
            throw new InvalidActionException("Maximum number of periodic events exceeded");
        }

        final Set<String> existingEventIds = backupManager.getScheduler().getPeriodicEvents()
                .stream().map(PeriodicEvent::getEventId).collect(Collectors.toSet());

        String newId;
        if (request.getEventId() == null) {
            newId = String.valueOf(randomNumberGenerator.nextInt(MAX_NUM + 1));
            while (existingEventIds.contains(newId)) {
                newId = String.valueOf(randomNumberGenerator.nextInt(MAX_NUM + 1));
            }
        } else {
            newId = request.getEventId();
            // Throws exception for duplicate id
            if (existingEventIds.contains(newId)) {
                throw new InvalidIdException("Event Id " + newId + " already exists");
            }

            idValidator.validateId(newId);
            esapiValidator.validateEventId(newId);
        }
        return newId;

    }

    private String generateCalendarEventId(final BackupManager backupManager) {
        if (backupManager.getScheduler().getPeriodicEvents().size() >= MAX_NUM) {
            throw new InvalidActionException("Maximum number of periodic events exceeded");
        }
        final Set<String> existingEventIds = backupManager.getScheduler().getPeriodicEvents()
                .stream().map(PeriodicEvent::getEventId).collect(Collectors.toSet());

        String newId;

        newId = String.valueOf(randomNumberGenerator.nextInt(MAX_NUM + 1));
        while (existingEventIds.contains(newId)) {
            newId = String.valueOf(randomNumberGenerator.nextInt(MAX_NUM + 1));
        }

        return newId;
    }

    /**
     * Returns list of persisted periodic events in a backup manager
     * @param backupManagerId owner of periodic events
     * @return list of periodic events scheduled in a backup manager
     */
    public List<PeriodicEvent> getPeriodicEvents(final String backupManagerId) {
        return periodicEventFileService.getEvents(backupManagerId);
    }

    /**
     * Returns list of persisted periodic events in a backup manager
     * @param backupManagerId owner of periodic events
     * @return list of periodic events scheduled in a backup manager
     */
    public List<CalendarEvent> getCalendarEvents(final String backupManagerId) {
        return calendarEventFileService.getEvents(backupManagerId);
    }

    private void validateEndTime(final String startTime, final String endTime) {
        final OffsetDateTime startOffsetDateTime = parseToOffsetDateTime(startTime);
        final OffsetDateTime endOffsetDateTime;
        if (endTime == null) {
            return;
        } else {
            endOffsetDateTime = parseToOffsetDateTime(endTime);
        }

        if (endOffsetDateTime.isBefore(startOffsetDateTime)) {
            log.error(endTime + " is before " + startTime);
            throw new UnprocessableEntityException("Enter StopTime later than StartTime");
        }
    }

    @Autowired
    public void setPeriodicEventFileService(final PeriodicEventFileService fileService) {
        this.periodicEventFileService = fileService;
    }

    /**
     * Delete a periodic event from the schedulers list, and delete its data file
     * @param backupManager - the manager who's scheduler needs updating
     * @param event - the event to delete
     * @param changedByCMYP - specifies if delete request is from cmyp or rest endpoint
     * */
    public void deletePeriodicEvent(final BackupManager backupManager, final PeriodicEvent event, final boolean changedByCMYP) {
        if (!changedByCMYP) {
            // do not delete the event from cmMediatorService
            // if the request is NOT from cmyp
            // cmMediatorService.deletePeriodicEvent(event);
            log.debug("the delete action not raised from CMYP");
        }
        periodicEventFileService.deleteEvent(event);
        backupManager.getScheduler().removePeriodicEvent(event);
        backupManager.getScheduler().updateNextScheduledTime();
        backupManager.getScheduler().persist();
    }

    /**
     * Delete a periodic event from the schedulers list, and delete its data file
     * @param backupManager - the manager who's scheduler needs updating
     * @param event - the event to delete
     * */
    public void deleteCalendarEvent(final BackupManager backupManager, final CalendarEvent event) {
        calendarEventFileService.deleteEvent(event);
        backupManager.getScheduler().removeCalendarEvent(event);
        // The next scheduler may be deleted, so it need to update again.
        backupManager.getScheduler().updateNextScheduledTime();
        //Update the next scheduled time and extra
        backupManager.getScheduler().persist();
    }

    /**
     * Replace a calendar based event with new event
     * @param backupManager to schedule an event
     * @param calendarEvent to delete this event
     * @param scheduleId to reuse the event id
     * @param request request body
     * @return the new event
     */
    public CalendarEvent replaceCalendarEvent(final BackupManager backupManager, final CalendarEvent calendarEvent,
                                              final String scheduleId,
                                              final V4CalendarEventRequest request) {
        calendarEventValidator.validate(request);
        deleteCalendarEvent(backupManager,
                calendarEvent);
        return createCalendarEvent(backupManager, request, scheduleId);
    }

    private void persistCalendarEvent(final CalendarEvent event) {
        calendarEventFileService.writeToFile(event);
    }

    private void persistPeriodicEvent(final PeriodicEvent event) {
        periodicEventFileService.writeToFile(event);
        cmMediatorService.updatePeriodicEvent(event);
    }

    private void persistNewPeriodicEvent(final BackupManager backupManager, final PeriodicEvent event, final boolean isRESTConfig) {
        backupManager.getScheduler().addPeriodicEvent(event);
        periodicEventFileService.writeToFile(event);
        if (isRESTConfig) {
            cmMediatorService.addPeriodicEvent(event);
        } else {
            cmMediatorService.updatePeriodicEvent(event);
        }
    }

    @Autowired
    public void setPeriodicEventScheduler(final PeriodicEventScheduler eventHandler) {
        periodicEventScheduler = eventHandler;
    }

    @Autowired
    public void setCalendarEventFileService(final CalendarEventFileService calendarEventFileService) {
        this.calendarEventFileService = calendarEventFileService;
    }

    @Autowired
    public void setCmMediatorService(final CMMediatorService cmMediatorService) {
        this.cmMediatorService = cmMediatorService;
    }

    @Autowired
    public void setEsapiValidator(final ESAPIValidator esapiValidator) {
        this.esapiValidator = esapiValidator;
    }

    @Autowired
    public void setIdValidator(final IdValidator idValidator) {
        this.idValidator = idValidator;
    }

    @Autowired
    public void setCalendarEventScheduler(final CalendarEventScheduler calendarEventScheduler) {
        this.calendarEventScheduler = calendarEventScheduler;
    }

    @Autowired
    public void setCalendarEventValidator(final CalendarEventValidator calendarEventValidator) {
        this.calendarEventValidator = calendarEventValidator;
    }
}
