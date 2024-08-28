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
package com.ericsson.adp.mgmt.backupandrestore.backup.manager;

import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents housekeeping information to be persisted
 */
public class HousekeepingInformation implements Versioned<HousekeepingInformation> {

    protected int maxNumberBackups;
    protected String autoDelete;

    private Version<HousekeepingInformation> version;

    /**
     * Used by Jackson
     */
    public HousekeepingInformation() {}

    /**
     * @param autoDelete - specify if autoDelete is enabled or not
     * @param maxNumberBackups - max number of backups to be stored in a backup manager
     */
    public HousekeepingInformation(final String autoDelete, final int maxNumberBackups) {
        this.autoDelete = autoDelete;
        this.maxNumberBackups = maxNumberBackups;
    }

    /**
     * Constructor included in Jackson
     * @param maxNumberBackups represents the maxNumberofbackups
     */
    public HousekeepingInformation(final int maxNumberBackups) {
        this.maxNumberBackups = maxNumberBackups;
    }

    /**
     * Constructor included in Jackson
     * @param autoDelete auto-delete enabled or disabled
     */
    public HousekeepingInformation(final String autoDelete) {
        this.autoDelete = autoDelete;
    }

    @JsonProperty(value = "auto-delete")
    public String getAutoDelete() {
        return autoDelete;
    }

    @JsonProperty(value = "max-stored-manual-backups")
    public int getMaxNumberBackups() {
        return maxNumberBackups;
    }

    public void setAutoDelete(final String autoDelete) {
        this.autoDelete = autoDelete;
    }

    public void setMaxNumberBackups(final int maxNumberBackups) {
        this.maxNumberBackups = maxNumberBackups;
    }

    @Override
    @JsonIgnore
    public Version<HousekeepingInformation> getVersion() {
        return version;
    }

    @Override
    @JsonIgnore
    public void setVersion(final Version<HousekeepingInformation> version) {
        this.version = version;
    }
}
