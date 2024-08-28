/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.metrics;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metric details returned by Prometheus endpoint.
 */
public class PrometheusMetric {

    private String name;
    private String description;
    private Object baseUnit;
    private List<Measurement> measurements;
    private List<AvailableTag> availableTags;

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(final String name) {
        this.name = name;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(final String description) {
        this.description = description;
    }

    @JsonProperty("baseUnit")
    public Object getBaseUnit() {
        return baseUnit;
    }

    @JsonProperty("baseUnit")
    public void setBaseUnit(final Object baseUnit) {
        this.baseUnit = baseUnit;
    }

    @JsonProperty("measurements")
    public List<Measurement> getMeasurements() {
        return measurements;
    }

    @JsonProperty("measurements")
    public void setMeasurements(final List<Measurement> measurements) {
        this.measurements = measurements;
    }

    @JsonProperty("availableTags")
    public List<AvailableTag> getAvailableTags() {
        return availableTags;
    }

    @JsonProperty("availableTags")
    public void setAvailableTags(final List<AvailableTag> availableTags) {
        this.availableTags = availableTags;
    }

}
