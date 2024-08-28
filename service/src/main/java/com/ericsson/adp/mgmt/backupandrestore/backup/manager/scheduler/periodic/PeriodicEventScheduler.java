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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.AdminState;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.SchedulerMetricsUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionService;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.action.payload.BackupNamePayload;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import static com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils.parseToOffsetDateTime;
/**
 * Schedules periodic events
 */
@Service
public class PeriodicEventScheduler {
    private static final Logger log = LogManager.getLogger(PeriodicEventScheduler.class);

    private static final String SEPARATOR = "-";

    private final ScheduledThreadPoolExecutor executorService;
    private ActionService actionService;
    private BackupManagerRepository backupManagerRepository;

    /**
     * Schedules periodic events
     */
    public PeriodicEventScheduler() {
        executorService = new ScheduledThreadPoolExecutor(1);
        executorService.setRemoveOnCancelPolicy(true);
    }

    /**
     * Schedules periodic event
     * @param event to be scheduled
     */
    public void scheduleEvent(final PeriodicEvent event) {
        final Scheduler scheduler = getScheduler(event.getBackupManagerId());

        // Reset the schedulers "model" of this event - the scheduled backup and cancel it has
        // stored relating to this event, if it has any
        scheduler.removeScheduledCancels(event.getEventId());
        scheduler.removeScheduledBackups(event.getEventId());
        if (event.periodInSeconds() <= 0) {
            log.info("Event <{}> not scheduled as defined period is 0", event.getEventId());
        } else {

            // Generate a scheduled backup for this event, and add it to the scheduler
            final long initialDelay = initialDelayInSeconds(event);
            final ScheduledFuture<?> scheduledBackup = executorService
                    .scheduleAtFixedRate(scheduledTask(event), initialDelay, event.periodInSeconds(), TimeUnit.SECONDS);
            scheduler.addScheduledBackups(event.getEventId(), scheduledBackup);

            // Generate a scheduled cancel for this event (if it needs one), and add it to the scheduler
            endEvent(event, scheduler);

            // Update the events next run information
            final OffsetDateTime firstRun = firstRunTime(event);
            event.setNextRun(firstRun);
        }

        // Swap this event in the place of the existing event in the scheduler, if one exists
        scheduler.updatePeriodicEvent(event);

        // Update and persist the schedulers model of itself
        scheduler.updateNextScheduledTime();
        scheduler.persist();
    }

    private Runnable scheduledTask(final PeriodicEvent event) {
        return () -> {
            String scheduledBackupName = "";
            final Scheduler scheduler = getScheduler(event.getBackupManagerId());
            try {
                if (AdminState.UNLOCKED == scheduler.getAdminState()) {
                    final OffsetDateTime creationTime = getCurrentTime();
                    final String backupName = scheduler.getScheduledBackupName() + SEPARATOR
                            + DateTimeUtils.convertToString(creationTime);

                    scheduledBackupName = backupName;

                    actionService.handleActionRequest(event.getBackupManagerId(),
                            createRequest(backupName, creationTime));
                    scheduler.setMostRecentlyCreatedAutoBackup(backupName);
                    // Update metric with successful scheduled backup
                    SchedulerMetricsUtils.updateMissedScheduledBackupMetric(event, true, scheduledBackupName, "");
                } else {
                    log.info("Scheduled event <{}> did not execute as Scheduler is LOCKED", event.getEventId());
                }
            } catch (Exception e) {
                // Update metric with failed scheduled backup
                SchedulerMetricsUtils.updateMissedScheduledBackupMetric(event, false, scheduledBackupName, e.getMessage());
                log.error("Failed to run action for event <{}>", event.getEventId(), e);
            }
            event.setNextRun(nextRunTime(event));
            scheduler.updatePeriodicEvent(event);
            scheduler.updateNextScheduledTime();
            try {
                scheduler.persist();
            } catch (Exception e) {
                log.error("Failed to persist update to event <{}>", event.getEventId(), e);
            }
        };
    }


    private void endEvent(final PeriodicEvent event, final Scheduler scheduler) {

        if (event.getStopTime() != null) {
            final Duration duration = Duration.between(getCurrentTime(), parseToOffsetDateTime(event.getStopTime()));
            final ScheduledFuture<?> scheduledCancel = executorService.schedule(scheduledCancel(event), duration.getSeconds(), TimeUnit.SECONDS);
            scheduler.addScheduledCancels(event.getEventId(), scheduledCancel);
        }
    }

    private Runnable scheduledCancel(final PeriodicEvent event) {
        return () -> {
            final Scheduler schedulerRunTime = getScheduler(event.getBackupManagerId());
            schedulerRunTime.removeScheduledBackups(event.getEventId());
            event.setNextRun(null);
            schedulerRunTime.updatePeriodicEvent(event);
            schedulerRunTime.updateNextScheduledTime();
            schedulerRunTime.persist();
        };
    }

    private long initialDelayInSeconds(final PeriodicEvent event) {
        final long currentTime = OffsetDateTime.now(ZoneId.systemDefault()).toEpochSecond();
        final long startTime = parseToOffsetDateTime(event.getStartTime()).toEpochSecond();
        final long period = event.periodInSeconds();
        final long deltaTime = currentTime - startTime;
        return period -
                ((deltaTime + period) % period) - // the "within a period" delay component
                (period * Long.min(0, (deltaTime / period) + 1)); // the "outside a period" delay component
    }

    private OffsetDateTime firstRunTime(final PeriodicEvent event) {
        // OffsetDateTime does not take into account of the DST changes hence using ZonedDateTime
        final ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.systemDefault()).plusSeconds(initialDelayInSeconds(event));
        return dateTime.withFixedOffsetZone().toOffsetDateTime();
    }

    private OffsetDateTime nextRunTime(final PeriodicEvent event) {
        final ZonedDateTime dateTime = event.getNextRun().toZonedDateTime().plusSeconds(event.periodInSeconds());
        return dateTime.withFixedOffsetZone().toOffsetDateTime();
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

    private OffsetDateTime getCurrentTime() {
        return OffsetDateTime.now(ZoneId.systemDefault());
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
