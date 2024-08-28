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

import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerConstants.DEFAULT_SCHEDULED_BACKUP_NAME;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.calendar.CalendarEvent;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import com.ericsson.adp.mgmt.backupandrestore.exception.ScheduledEventNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.ericsson.adp.mgmt.backupandrestore.util.OrderedConcurrentMap;

/**
 * Holds scheduler information of a backupManager
 */
public class Scheduler extends SchedulerInformation {

    // eventId, ScheduledFuture
    private final Map<String, ScheduledFuture<?>> scheduledBackups = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> scheduledCancels = new ConcurrentHashMap<>();

    // eventId, event
    private final OrderedConcurrentMap<String, PeriodicEvent> periodicEvents =
        new OrderedConcurrentMap<String, PeriodicEvent>();
    private final Map<String, CalendarEvent> calendarEvents = new LinkedHashMap<>();
    private final String backupManagerId;
    private final Consumer<Scheduler> persistFunction;

    /**
     * Holds scheduler information of a backupManager
     * @param backupManagerId of the backupManager the {@link Scheduler} belongs to
     * @param persistFunction to persist scheduler
     */
    public Scheduler(final String backupManagerId, final Consumer<Scheduler> persistFunction) {
        this.backupManagerId = backupManagerId;
        this.persistFunction = persistFunction;
        adminState = AdminState.UNLOCKED;
        scheduledBackupName = DEFAULT_SCHEDULED_BACKUP_NAME.toString();
    }

    /**
     * Holds scheduler information of a backupManager
     * @param information {@link SchedulerInformation}
     * @param periodicEvents list of periodic events scheduled in the scheduler
     * @param calendarEvents list of calendar events scheduled in the scheduler
     * @param backupManagerId of the backupManager the {@link Scheduler} belongs to
     * @param persistFunction to persist scheduler
     */
    public Scheduler(final SchedulerInformation information, final List<PeriodicEvent> periodicEvents,
                     final List<CalendarEvent> calendarEvents,
                     final String backupManagerId, final Consumer<Scheduler> persistFunction) {
        adminState = information.getAdminState();
        scheduledBackupName = information.getScheduledBackupName();
        mostRecentlyCreatedAutoBackup = information.getMostRecentlyCreatedAutoBackup();
        nextScheduledTime = information.getNextScheduledTime();
        autoExport = information.autoExport;
        autoExportUri = information.autoExportUri;
        autoExportPassword = information.autoExportPassword;
        sftpServerName = information.sftpServerName;
        periodicEvents.forEach(this::addPeriodicEvent);
        calendarEvents.forEach(this::addCalendarEvent);
        this.backupManagerId = backupManagerId;
        this.persistFunction = persistFunction;
        this.setVersion(information.getVersion());
    }

    public List<PeriodicEvent> getPeriodicEvents() {
        return new ArrayList<>(periodicEvents.values());
    }

    public List<CalendarEvent> getCalendarEvents() {
        return new ArrayList<>(calendarEvents.values());
    }

    /**
     * Reload this scheduler in place, from persistence layer
     * @param handler - the scheduled event handler used to perform the reload
     * @param schedulerFileService - the file service for the scheduler
     * */
    public void reload(final ScheduledEventHandler handler, final SchedulerFileService schedulerFileService) {
        final SchedulerInformation loaded = schedulerFileService.getPersistedSchedulerInformation(backupManagerId);
        this.scheduledBackupName = loaded.scheduledBackupName;
        this.adminState = loaded.adminState;
        this.autoExport = loaded.autoExport;
        this.autoExportPassword = loaded.autoExportPassword;
        this.autoExportUri = loaded.autoExportUri;
        this.sftpServerName = loaded.sftpServerName;
        // Drop all our current events, then reload them
        final Iterator<Map.Entry<String, PeriodicEvent>> iterator = periodicEvents.entrySet().iterator();
        while (iterator.hasNext()) {
            final PeriodicEvent event = iterator.next().getValue();
            iterator.remove();
            removeScheduledBackups(event.getEventId());
            removeScheduledCancels(event.getEventId());
        }
        final Iterator<Map.Entry<String, CalendarEvent>> calendarIterator = calendarEvents.entrySet().iterator();
        while (calendarIterator.hasNext()) {
            final CalendarEvent event = calendarIterator.next().getValue();
            calendarIterator.remove();
            removeScheduledBackups(event.getEventId());
        }
        handler.getPeriodicEvents(backupManagerId).forEach(this::addPeriodicEvent);
        handler.schedulePeriodicEvents(this);
        handler.getCalendarEvents(backupManagerId).forEach(this::addCalendarEvent);
        handler.scheduleCalendarEvents(this);
    }

    /**
     * Adds periodic event to scheduler
     * @param event to be added to scheduler
     */
    public void addPeriodicEvent(final PeriodicEvent event) {
        periodicEvents.put(event.getEventId(), event);
    }

    /**
     * Adds calendar event to scheduler
     * @param event to be added to scheduler
     */
    public void addCalendarEvent(final CalendarEvent event) {
        calendarEvents.putIfAbsent(event.getEventId(), event);
    }

    /**
     * Returns specific periodic event
     * @param eventId of event
     * @return periodic event with specified id if present
     */
    public PeriodicEvent getPeriodicEvent(final String eventId) {
        if (periodicEvents.containsKey(eventId)) {
            return periodicEvents.get(eventId);
        } else {
            throw new ScheduledEventNotFoundException(eventId);
        }
    }

    /**
     * Returns specific calendar event
     * @param eventId of event
     * @return periodic event with specified id if present
     */
    public CalendarEvent getCalendarEvent(final String eventId) {
        if (calendarEvents.containsKey(eventId)) {
            return calendarEvents.get(eventId);
        } else {
            throw new ScheduledEventNotFoundException(eventId);
        }
    }

    /**
     * Returns specific periodic event
     * @param periodicEventIndex positional index of event
     * @return periodic event if present
     */
    public PeriodicEvent getPeriodicEvent(final int periodicEventIndex) {
        return Optional.of(getPeriodicEvents().get(periodicEventIndex))
                .orElseThrow(() -> new ScheduledEventNotFoundException(String.valueOf(periodicEventIndex)));
    }

    /**
     * Returns specific calendar event
     * @param calendarEventIndex positional index of event
     * @return periodic event if present
     */
    public CalendarEvent getCalendarEvent(final int calendarEventIndex) {
        return Optional.of(getCalendarEvents().get(calendarEventIndex))
                .orElseThrow(() -> new ScheduledEventNotFoundException(String.valueOf(calendarEventIndex)));
    }

    /**
     * Updates the periodic event instance in the scheduler
     * @param event to be updated in the scheduler
     */
    public void updatePeriodicEvent(final PeriodicEvent event) {
        periodicEvents.replace(event.getEventId(), event);
    }

    /**
     * Replace the periodic event instance in the scheduler
     * update one eventId in map periodicEvents
     * and keep other items in the same order in the map
     * @param eventId the id of the event to be replaced
     * @param event to be updated in the scheduler
     */
    public void replacePeriodicEvent(final String eventId, final PeriodicEvent event) {
        periodicEvents.replaceKey(eventId, event.getEventId());
    }

    /**
     * Updates the periodic event instance in the scheduler
     * @param event to be updated in the scheduler
     */
    public void updateCalendarEvent(final CalendarEvent event) {
        calendarEvents.replace(event.getEventId(), event);
    }

    public String getBackupManagerId() {
        return backupManagerId;
    }

    /**
     * @return the persistFunction
     */
    public Consumer<Scheduler> getPersistFunction() {
        return persistFunction;
    }

    /**
     * Adds scheduled future of periodic backup event
     * @param eventId of periodic event
     * @param scheduledFuture instance of scheduled backup event {@link ScheduledFuture}
     */
    public void addScheduledBackups(final String eventId, final ScheduledFuture<?> scheduledFuture) {
        scheduledBackups.putIfAbsent(eventId, scheduledFuture);
    }

    /**
     * Adds scheduled future of cancel periodic backup event
     * @param eventId of periodic event
     * @param scheduledFuture instance of scheduled cancel of periodic backup event {@link ScheduledFuture}
     */
    public void addScheduledCancels(final String eventId, final ScheduledFuture<?> scheduledFuture) {
        scheduledCancels.putIfAbsent(eventId, scheduledFuture);
    }

    /**
     * Persist scheduler
     */
    public void persist() {
        persistFunction.accept(this);
    }

    /**
     * Remove this periodic event from the schedulers list of events
     * @param event - the event to remove from the list
     * */
    public void removePeriodicEvent(final PeriodicEvent event) {
        periodicEvents.remove(event.getEventId());
        removeScheduledBackups(event.getEventId());
        removeScheduledCancels(event.getEventId());
    }

    /**
     * Remove this periodic event from the schedulers list of events
     * @param event - the event to remove from the list
     * */
    public void removeCalendarEvent(final CalendarEvent event) {
        calendarEvents.remove(event.getEventId());
        removeScheduledBackups(event.getEventId());
    }

    /**
     * Removes scheduled periodic backup
     * @param eventId of the scheduled periodic event
     */
    public void removeScheduledBackups(final String eventId) {
        if (scheduledBackups.containsKey(eventId)) {
            if (!scheduledBackups.get(eventId).isDone()) {
                scheduledBackups.get(eventId).cancel(false);
            }
            scheduledBackups.remove(eventId);
        }
    }

    /**
     * Removes scheduled cancellation of periodic backup
     * @param eventId of the scheduled periodic event
     */
    public void removeScheduledCancels(final String eventId) {
        if (scheduledCancels.containsKey(eventId)) {
            if (!scheduledCancels.get(eventId).isDone()) {
                scheduledCancels.get(eventId).cancel(false);
            }
            scheduledCancels.remove(eventId);
        }
    }

    /**
     * Updates next scheduled time
     */
    public void updateNextScheduledTime() {
        if (AdminState.UNLOCKED == getAdminState()) {
            final Optional<OffsetDateTime> nextPeriodicRun = periodicEvents
                    .values()
                    .stream()
                    .map(PeriodicEvent::getNextRun)
                    .filter(Objects::nonNull)
                    .sorted()
                    .findFirst();
            final Optional<OffsetDateTime> nextCalendarRun = calendarEvents
                    .values()
                    .stream()
                    .map(CalendarEvent::getNextRun)
                    .filter(Objects::nonNull)
                    .sorted()
                    .findFirst();
            OffsetDateTime nextRun;
            if (nextCalendarRun.isEmpty() && nextPeriodicRun.isEmpty()) {
                setNextScheduledTime(null);
                return;
            }
            if (nextCalendarRun.isPresent() && nextPeriodicRun.isPresent()) {
                nextRun = nextCalendarRun.get().isBefore(nextPeriodicRun.get()) ? nextCalendarRun.get() : nextPeriodicRun.get();
            } else {
                nextRun = nextCalendarRun.orElseGet(nextPeriodicRun::get);
            }

            setNextScheduledTime(DateTimeUtils.convertToString(nextRun));

        } else {
            setNextScheduledTime(null);
        }
    }

    /**
     * Gets index of periodic event
     * @param eventId to be checked.
     * @return index of periodic event
     */
    public int getPeriodicEventIndex(final String eventId) {
        return getPeriodicEvents()
                .stream()
                .map(PeriodicEvent::getEventId)
                .collect(Collectors.toList())
                .indexOf(eventId);
    }
}
