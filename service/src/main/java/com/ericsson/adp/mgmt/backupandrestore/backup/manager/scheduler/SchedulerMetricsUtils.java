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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler;

import static com.ericsson.adp.mgmt.backupandrestore.util.MetricsIds.METRIC_SCHEDULED_BACKUP_MISSED;

import java.util.Optional;

import com.ericsson.adp.mgmt.backupandrestore.SpringContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Used to update the Scheduler related Metrics
 */
public class SchedulerMetricsUtils {
    private static final Logger log = LogManager.getLogger(SchedulerMetricsUtils.class);

    private SchedulerMetricsUtils() {
    }

    /**
     * Updates MissedScheduledBackupMetric
     * @param event of backup missed
     * @param backupSuccess state of backup
     * @param name name of scheduled backup
     * @param exceptionMessage exception message text
     */
    public static void updateMissedScheduledBackupMetric(final ScheduledEvent event, final boolean backupSuccess,
                                                         final String name, final String exceptionMessage) {
        final String cause = "Failed to run scheduled action: " + exceptionMessage;
        final Optional<MeterRegistry> optionalMeter = SpringContext.getBean(MeterRegistry.class);
        MeterRegistry meterRegistry;
        if (optionalMeter.isPresent()) {
            meterRegistry = optionalMeter.get();
        } else {
            log.error("Meter registry bean not present, returning");
            return;
        }

        // Clear metric
        meterRegistry.find(METRIC_SCHEDULED_BACKUP_MISSED.identification())
                .tag("backup_type", event.getBackupManagerId())
                .tag("event_id", event.getEventId())
                .counters()
                .forEach(meterRegistry::remove);
        METRIC_SCHEDULED_BACKUP_MISSED.unRegister();

        if (!backupSuccess) {
            // Create metric and increment it
            Metrics.counter(METRIC_SCHEDULED_BACKUP_MISSED.identification(),
                    "event_id", event.getEventId(),
                    "backup_type", event.getBackupManagerId(),
                    "backup_name", name,
                    "cause", cause).increment();
        } else {
            // Create metric but don't increment it
            Metrics.counter(METRIC_SCHEDULED_BACKUP_MISSED.identification(),
                    "event_id", event.getEventId(),
                    "backup_type", event.getBackupManagerId(),
                    "backup_name", name,
                    "cause", "Successfully ran scheduled action");
        }
    }
}
