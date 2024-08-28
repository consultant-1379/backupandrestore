/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

/**
 * An enum to represent the metric tags
 */
public enum MetricTags {
    BACKUP_TYPE("backup_type", ""),
    BACKUP_NAME("backup_name", ""),
    BACKUP_TYPE_LIST("backup_type_list", ""),
    ACTION("action", ""),
    ACTION_ID("action_id", ""),
    AGENT("agent", ""),
    STAGE("stage", ""),
    STATUS("status", ""),
    PVC("persistentvolumeclaim", ""),
    EVENT_ID ("event_id", ""),
    CAUSE ("cause", ""),
    ADDITIONAL_INFO("additional_info", "");

    private final String identification;
    private final String value;

    /**
     * Metric Tags.
     *
     * @param stringRepresentation string representation of a Metric Tag.
     * @param value string Value of a Metric Tag.
     */
    MetricTags(final String stringRepresentation, final String value) {
        this.identification = stringRepresentation;
        this.value = value;
    }

    /**
     * Metric tag identification
     * @return return the name of the tag
     */
    public String identification() {
        return identification;
    }

    /**
     * Metric tag default value
     * @return default value
     */
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return this.identification;
    }
}
