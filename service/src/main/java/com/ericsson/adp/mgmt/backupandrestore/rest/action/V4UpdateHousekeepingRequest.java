/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.action;

import jakarta.validation.constraints.NotNull;


/**
 * Request to update housekeeping using the v4 REST endpoint.
 */
public class V4UpdateHousekeepingRequest extends CreateActionRequest{

    @NotNull(message = "Missing required field autoDelete")
    private String autoDelete;

    @NotNull(message = "Missing required field maxStoredBackups")
    private Integer maxStoredBackups;

    /**
     * Getter of autoDelete
     * @return autoDelete
     */
    @Override
    public String getAutoDelete() {
        return this.autoDelete;
    }

    /**
     * Setter of autoDelete
     * @param autoDelete the configuration or autoDelete
     */
    @Override
    public void setAutoDelete(final String autoDelete) {
        this.autoDelete = autoDelete;
    }

    /**
     * getter of maxStoredBackups
     * @return maxStoredBackups
     */
    public Integer getMaxStoredBackups() {
        return getMaximumManualBackupsNumberStored();
    }

    /**
     * Setter of maxStoredBackups
     * @param maxStoredBackups the limit of maxStoredBackups
     */
    public void setMaxStoredBackups(final Integer maxStoredBackups) {
        this.maxStoredBackups = maxStoredBackups;
        setMaximumManualBackupsNumberStored(this.maxStoredBackups);
    }
}
