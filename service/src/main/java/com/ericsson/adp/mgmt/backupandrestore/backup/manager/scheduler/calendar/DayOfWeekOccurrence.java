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
 * The enum class of the Day-of-week occurrence.
 */
public enum DayOfWeekOccurrence {
    ALL(0), FIRST(1), SECOND(2), THIRD(3), FOURTH(4), LAST(5);

    /**
     * To ensure a certain order instead of the default ordinal value
     */
    public final int value;

    /**
     * Constructor of Enum
     * @param value the value of each Enum
     */
    DayOfWeekOccurrence(final int value) {
        this.value = value;
    }
}
