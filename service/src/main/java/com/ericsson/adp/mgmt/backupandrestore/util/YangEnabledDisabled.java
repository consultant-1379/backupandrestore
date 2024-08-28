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
package com.ericsson.adp.mgmt.backupandrestore.util;

/**
 * An enum to represent the Yang type enabled/disabled
 * */
public enum YangEnabledDisabled {
    ENABLED("enabled"), DISABLED("disabled");

    private final String value;

    /**
     * private constructor
     * @param value - the string representation of the enabled/disabled value
     * */
    YangEnabledDisabled(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Take a string value and match it to one of the enum constants here, in a CASE INSENSITIVE manner
     * throws IllegalArgumentException if a matching enum constant can't be found
     * @param value - value to be matched
     * @return a matched value
     * */
    public static YangEnabledDisabled caseSafeOf(final String value) {
        return YangEnabledDisabled.valueOf(value.toUpperCase());
    }
}
