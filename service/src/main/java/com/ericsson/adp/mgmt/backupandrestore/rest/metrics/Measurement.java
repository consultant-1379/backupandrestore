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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Measurement returned as part of metric by Prometheus endpoint
 */
public class Measurement {

    private String statistic;
    private Double value;

    @JsonProperty("statistic")
    public String getStatistic() {
        return statistic;
    }

    @JsonProperty("statistic")
    public void setStatistic(final String statistic) {
        this.statistic = statistic;
    }

    @JsonProperty("value")
    public Double getValue() {
        return value;
    }

    @JsonProperty("value")
    public void setValue(final Double value) {
        this.value = value;
    }

}
