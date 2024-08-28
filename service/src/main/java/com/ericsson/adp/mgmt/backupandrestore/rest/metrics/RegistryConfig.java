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
package com.ericsson.adp.mgmt.backupandrestore.rest.metrics;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;

/**
 * Configuration class used to customize the name of BRO metrics
 */
@Configuration
@EnableAspectJAutoProxy
public class RegistryConfig {

    /**
     * Called when it's constructed, set micrometers rename filter
     * @return MeterRegistryCustomizer meterRegistry used to replace metrics id
     */
    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsConfig() {
        return registry -> registry.config().meterFilter(new CustomRenameFilter());
    }

}

/**
 * MeterFilter used to rename the metric id
 */
class CustomRenameFilter implements MeterFilter {
    private static final Map<String, String> MICROMETER_TO_PROMETHEUS_NAMES = new HashMap<>();

    static {
        MICROMETER_TO_PROMETHEUS_NAMES.put("http.server.requests", "bro_http.server.requests");
        MICROMETER_TO_PROMETHEUS_NAMES.put("http.client.requests", "bro_http.client.requests");
    }

    @Override
    public Meter.Id map(final Meter.Id metricId) {
        final String convertedName = MICROMETER_TO_PROMETHEUS_NAMES.get(metricId.getName());
        return convertedName == null ? metricId : metricId.withName(convertedName);
    }
}
