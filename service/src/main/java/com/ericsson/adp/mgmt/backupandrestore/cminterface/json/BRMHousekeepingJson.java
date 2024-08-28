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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.HousekeepingInformation;

/**
 * Represents a Housekeeping in the BRM model.
 */
public class BRMHousekeepingJson extends HousekeepingInformation {

    /**
     * Default constructor, to be used by Jackson.
     */
    public BRMHousekeepingJson() {}

    /**
     * Creates json object.
     * @param housekeeping to be represented.
     */
    public BRMHousekeepingJson(final Housekeeping housekeeping) {
        super(housekeeping.getAutoDelete(), housekeeping.getMaxNumberBackups());
    }

    /**
     * Creates json object.
     * @param value including change.
     */
    public BRMHousekeepingJson(final int value) {
        super(value);
    }

    /**
     * Creates json object.
     * @param value including change.
     */
    public BRMHousekeepingJson(final String value) {
        super(value);
    }

    @Override
    public String toString() {
        return "BRMHousekeepingJson [max-stored-manual-backups=" + maxNumberBackups + ", auto-delete=" + autoDelete + "]";
    }


}
