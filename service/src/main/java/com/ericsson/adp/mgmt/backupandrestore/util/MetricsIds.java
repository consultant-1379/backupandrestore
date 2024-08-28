/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.ACTION;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.ACTION_ID;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.ADDITIONAL_INFO;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.AGENT;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.BACKUP_NAME;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.BACKUP_TYPE;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.BACKUP_TYPE_LIST;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.CAUSE;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.EVENT_ID;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.PVC;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.STAGE;
import static com.ericsson.adp.mgmt.backupandrestore.util.MetricTags.STATUS;
import static io.micrometer.core.instrument.Meter.Type.COUNTER;
import static io.micrometer.core.instrument.Meter.Type.GAUGE;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.ericsson.adp.mgmt.backupandrestore.SpringContext;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Gauge.Builder;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.search.Search;

/**
 * Metrics ENUM including the description, type and default tags
 *
 */
public enum MetricsIds {
    METRIC_OPERATION_ENDTIME ("bro.operation.end.time", GAUGE, "End time of last operation",
            BACKUP_TYPE, ACTION, ACTION_ID, STATUS),
    METRIC_SCHEDULED_OPERATION_ERROR_INFO ("bro.scheduled.operation.error", GAUGE, "Error status of the last scheduled operation",
            BACKUP_TYPE, ACTION),
    METRIC_BRO_GRANULAR_STAGE_DURATION_SECONDS ("bro.granular.stage.duration.seconds", GAUGE, "Gauge that contains duration of stage",
            BACKUP_TYPE, STAGE, ACTION, ACTION_ID, AGENT),
    METRIC_BRO_GRANULAR_ENDTIME ("bro.granular.end.time", GAUGE, "Gauge that contains end time of a stage per agent",
            BACKUP_TYPE, STAGE, ACTION, ACTION_ID, STATUS, AGENT),
    METRIC_BRO_GRANULAR_STAGE_INFO ("bro.granular.stage.info", GAUGE, "Gauge that contains additional info of a stage",
            BACKUP_TYPE, ACTION, ACTION_ID, STATUS, AGENT, STAGE),
    METRIC_BRO_REGISTERED_AGENTS ("bro.registered.agents", GAUGE, "Number of agents registered with BRO",
            AGENT, BACKUP_TYPE_LIST),
    METRIC_BRO_OPERATION_INFO ("bro.operation.info", GAUGE, "Outcome of last operation",
            ACTION, BACKUP_TYPE, BACKUP_NAME, ACTION_ID, STATUS, ADDITIONAL_INFO),
    METRIC_BRO_OPERATION_TRANSFERRED_BYTES ("bro.operation.transferred.bytes", GAUGE, "Number of bytes transferred by backup operation",
            ACTION, BACKUP_TYPE, BACKUP_NAME, ACTION_ID, AGENT),

    METRIC_BRO_DISK_USAGE_BYTES ("bro.disk.usage.bytes", GAUGE, "Total size of the backup files on disk",
            BACKUP_TYPE),
    METRIC_BRO_STORED_BACKUPS ("bro.stored.backups", GAUGE, "Number of backups stored in BRO",
            BACKUP_TYPE),
    METRIC_BRO_OPERATION_STAGE_DURATION_SECONDS ("bro.operation.stage.duration.seconds", GAUGE, "Stage duration in seconds",
            ACTION_ID, BACKUP_TYPE, ACTION, STAGE, STATUS),
    METRIC_BRO_VOLUME_STATS_CAPACITY_BYTES ("bro.volume.stats.capacity.bytes", GAUGE, "Total size of the PVC capacity",
            PVC),
    METRIC_BRO_VOLUME_STATS_USED_BYTES ("bro.volume.stats.used.bytes", GAUGE, "Total size of the used space in PVC",
            PVC),
    METRIC_BRO_VOLUME_STATS_AVAILABLE_BYTES ("bro.volume.stats.available.bytes", GAUGE, "The amount of free space in PVC",
            PVC),
    METRIC_SCHEDULED_BACKUP_MISSED ("bro.scheduled.backup.missed", COUNTER, "BRO Scheduled backup missed",
            BACKUP_TYPE, EVENT_ID, BACKUP_NAME, CAUSE),
    METRIC_BRO_OPERATIONS_TOTAL ("bro.operations.total", COUNTER, "Counter for number of executed operations",
            ACTION, STATUS, BACKUP_TYPE),
    METRIC_BRO_GRANULAR_OPERATIONS_TOTAL ("bro.granular.operations.total", COUNTER, "Counter for number of granular executed operations",
            AGENT, ACTION, STATUS, BACKUP_TYPE);

    private final String metricId;
    private final String description;
    private final ConcurrentMap<String, Tag> tags;
    private final Meter.Type type;
    private final AtomicBoolean registered = new AtomicBoolean();

    /**
     * Constructor Metric definition
     * @param metricId Enum metric id
     * @param type Meter type
     * @param description Meter Id
     * @param metricTags List of Metrictags
     */
    MetricsIds(final String metricId, final Meter.Type type, final String description, final MetricTags... metricTags ) {
        this.metricId = metricId;
        this.description = description;
        tags = new ConcurrentHashMap<String, Tag>();
        for (final MetricTags mtags: metricTags) {
            tags.putIfAbsent(mtags.identification(), Tag.of (mtags.identification(), mtags.value()));
        }
        this.type = type;
    }

    /**
     * Stream of ENUM values
     * @return Stream of metric values
     */
    public static Stream<MetricsIds> stream() {
        return Stream.of(MetricsIds.values());
    }

    /**
     * Look for a specific Metric ID
     * @param metricID Metric Id
     * @return Metric defined in the ENUM
     */
    public static MetricsIds findById(final String metricID) {
        return Arrays.stream(values()).filter(value -> value.identification().equals(metricID)).findFirst().orElse(null);
    }

    /**
     * Get the enum id
     * @return Metric identifier
     */
    public String identification() {
        return metricId;
    }

    /**
     * Return Meter type
     * @return meter type
     */
    public Meter.Type type() {
        return type;
    }

    /**
     * Get an stream of tags
     * @return Array of tags
     */
    public Stream<Entry<String, Tag>> tags() {
        return tags.entrySet().stream();
    }

    /**
     * Get tag Metric description
     * @return metric description
     */
    public String description() {
        return description;
    }

    /**
     * Register an empty Metric
     */
    public void register() {
        SpringContext.getBean(MeterRegistry.class)
            .ifPresent(this::register);
    }

    private void register(final MeterRegistry registry) {
        if (registered.compareAndSet(false, true)) {
            initMetric (registry);
        }
    }

    private void initMetric ( final MeterRegistry registry) {
        if (type() == GAUGE) {
            final Builder<Supplier<Number>> metric = Gauge.builder(identification(), () -> 0);
            tags().forEach(entryTag -> {
                metric.tag(entryTag.getValue().getKey(), entryTag.getValue().getValue());
            });
            metric.description(description());
            metric.register(registry);
        } else if (type() == COUNTER) {
            final Counter.Builder counter = Counter.builder(identification());
            tags().forEach(entryTag -> {
                counter.tag(entryTag.getValue().getKey(), entryTag.getValue().getValue());
            });
            counter.description(identification());
            counter.register(registry);
        }
    }

    /**
     * Unregister the empty metric
     */
    public void unRegister() {
        SpringContext.getBean(MeterRegistry.class)
            .ifPresent(this::unRegister);
    }

    private void unRegister(final MeterRegistry registry) {
        if (registered.compareAndSet(true, false)) {
            removeMetric (registry);
        }
    }

    private void removeMetric ( final MeterRegistry registry) {
        final Search search = getSearchMetric (registry, metricId, tags);
        if (type == GAUGE) {
            search.gauges()
                .forEach(registry::remove);
        } else if (type == COUNTER) {
            search.counters().forEach(registry::remove);
        }

    }

    private Search getSearchMetric(final MeterRegistry registry, final String metricName, final ConcurrentMap<String, Tag> tags) {
        final Search search = getSearchMetric(registry, metricName);
        for (final Entry<String, Tag> entry: tags.entrySet()) {
            search.tag(entry.getValue().getKey(), "");
        }
        return search;
    }

    private static Search getSearchMetric(final MeterRegistry registry, final String metricName) {
        return registry.find(metricName);
    }
}
