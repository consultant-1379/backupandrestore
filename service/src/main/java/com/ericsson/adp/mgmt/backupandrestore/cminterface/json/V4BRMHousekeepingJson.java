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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.HousekeepingInformation;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Housekeeping in the BRM model.
 */
public class V4BRMHousekeepingJson extends HousekeepingInformation {

    /**
     * Default constructor, to be used by Jackson.
     */
    public V4BRMHousekeepingJson() {}

    /**
     * Creates json object.
     * @param housekeeping to be represented.
     */
    public V4BRMHousekeepingJson(final Housekeeping housekeeping) {
        super(housekeeping.getAutoDelete(), housekeeping.getMaxNumberBackups());
    }

    @Override
    @JsonProperty(value = "autoDelete")
    public String getAutoDelete() {
        return autoDelete;
    }

    @Override
    @JsonProperty(value = "maxStoredBackups")
    public int getMaxNumberBackups() {
        return maxNumberBackups;
    }
}
