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

import com.ericsson.adp.mgmt.backupandrestore.action.ActionService;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.AdminState;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerMetricsUtils;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Schedule calendar events
 */
@Service
public class CalendarEventScheduler {
    private static final Logger log = LogManager.getLogger(CalendarEventScheduler.class);
    private static final String SEPARATOR = "-";
    private final ScheduledThreadPoolExecutor executorService;
    private ActionService actionService;
    private BackupManagerRepository backupManagerRepository;

    /**
     * Constructor
     */
    public CalendarEventScheduler() {
        this.executorService = new ScheduledThreadPoolExecutor(1);
        executorService.setRemoveOnCancelPolicy(true);
    }

    /**
     * Schedules a calendar event
     * @param event to be scheduled
     */
    public void scheduleEvent(final CalendarEvent event) {
        final Scheduler scheduler = getScheduler(event.getBackupManagerId());
        // Reset the schedulers "model" of this event - the scheduled backup it has
        // stored relating to this event, if it has any
        scheduler.removeScheduledBackups(event.getEventId());

        // Calculate the duration to next trigger
        final Optional<LocalDateTime> nextValidTime = event.getNextValidTime();
        if (nextValidTime.isPresent()) {
            final ScheduledFuture<?> scheduledBackup = executorService.schedule(
                    scheduledTask(event), event.getDurationToNextTime(), TimeUnit.NANOSECONDS);
            scheduler.addScheduledBackups(event.getEventId(), scheduledBackup);

            // Update the events next run information
            final OffsetDateTime firstRun = nextValidTime.get().atZone(ZoneId.systemDefault()).toOffsetDateTime();
            event.setNextRun(firstRun);
        }
        scheduler.updateCalendarEvent(event);
        scheduler.updateNextScheduledTime();
        scheduler.persist();
    }

    /**
     * Return the scheduled task
     * @param event the calendar based scheduler
     * @return the runnable which will be scheduled
     */
    protected Runnable scheduledTask(final CalendarEvent event) {
        return () -> {
            String scheduledBackupName = "";
            final Scheduler scheduler = getScheduler(event.getBackupManagerId());
            try {
                if (AdminState.UNLOCKED == scheduler.getAdminState()) {
                    final OffsetDateTime creationTime = OffsetDateTime.now(event.getClock());
                    final String backupName = scheduler.getScheduledBackupName() + SEPARATOR
                            + DateTimeUtils.convertToString(creationTime);
                    scheduledBackupName = backupName;
                    actionService.handleActionRequest(event.getBackupManagerId(),
                            createRequest(backupName, creationTime));
                    scheduler.setMostRecentlyCreatedAutoBackup(backupName);

                    // Update metric with successful scheduled backup
                    SchedulerMetricsUtils.updateMissedScheduledBackupMetric(event, true, scheduledBackupName, "");
                    //Schedule the next event
                    scheduleEvent(event);

                } else {
                    log.info("Scheduled event <{}> did not execute as Scheduler is LOCKED", event.getEventId());
                }
            } catch (Exception e) {
                // Update metric with failed scheduled backup
                SchedulerMetricsUtils.updateMissedScheduledBackupMetric(event, false, scheduledBackupName, e.getMessage());
                log.error("Failed to run action for event <{}>", event.getEventId(), e);
            }
            if (event.getNextValidTime().isPresent()) {
                event.setNextRun(event.getNextValidTime().get().atZone(ZoneId.systemDefault()).toOffsetDateTime());
            } else {
                event.setNextRun(null);
            }
            scheduler.updateCalendarEvent(event);
            scheduler.updateNextScheduledTime();
            try {
                scheduler.persist();
            } catch (Exception e) {
                log.error("Failed to persist update to event <{}>", event.getEventId(), e);
            }
        };
    }

    private CreateActionRequest createRequest(final String scheduledBackupName, final OffsetDateTime creationTime) {
        final CreateActionRequest request = new CreateActionRequest();
        request.setAction(ActionType.CREATE_BACKUP);
        request.setScheduledEvent(true);
        request.setPayload(new BackupNamePayload(scheduledBackupName, Optional.of(creationTime)));
        return request;
    }

    private Scheduler getScheduler(final String backupManagerId) {
        return backupManagerRepository.getBackupManager(backupManagerId).getScheduler();
    }


    @Autowired
    public void setActionService(final ActionService actionService) {
        this.actionService = actionService;
    }
    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }
}
