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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager;

/**
 * Holds Housekeeping Constants for general utility.
 */
public enum HouseKeepingConstants {

    AUTO_DELETE ("auto-delete"),
    HOUSE_KEEPING ("housekeeping");

    private final String stringRepresentation;

    /**
     * @param stringRepresentation string constants.
     */
    HouseKeepingConstants(final String stringRepresentation) {
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
