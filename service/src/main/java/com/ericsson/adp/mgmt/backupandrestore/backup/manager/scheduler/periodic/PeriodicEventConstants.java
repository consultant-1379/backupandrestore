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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic;

/**
 * Holds SchedulerConstants for general utility.
 */
public enum PeriodicEventConstants {

    HOURS("hours"),
    MINUTES("minutes"),
    WEEKS("weeks"),
    DAYS("days"),
    EVENT_ID("id"),
    START_TIME("start-time"),
    STOP_TIME("stop-time"),
    PERIODIC_EVENT("periodic-event");

    private final String stringRepresentation;

    /**
     * @param stringRepresentation string constants.
     */
    PeriodicEventConstants(final String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    /**
     * Get String representation of enum
     * @return String representation of enum
     */
    @Override
    public String toString() {
        return stringRepresentation;
    }
}
