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

/**
 * The enum class of day of week.
 */
public enum DayOfWeek {
    ALL(0), MONDAY(1), TUESDAY(2), WEDNESDAY(3), THURSDAY(4), FRIDAY(5), SATURDAY(6), SUNDAY(7);

    /**
     * To ensure a certain order instead of the default ordinal value
     */
    public final int value;

    /**
     * Constructor of DayOfWeek
     * @param value the value of each Day of week
     */
    DayOfWeek(final int value) {
        this.value = value;
    }

}
