/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents BRM configuration in CM.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BRMConfiguration {

    private BRMJson brm = new BRMJson();

    /**
     * Default constructor, to be used by Jackson.
     */
    public BRMConfiguration() {}

    /**
     * Creates configuration.
     * @param backupManagers existing backupManagers.
     */
    public BRMConfiguration(final List<BackupManager> backupManagers) {
        this.brm = new BRMJson(backupManagers);
    }

    @JsonProperty("ericsson-brm:brm")
    public BRMJson getBrm() {
        return brm;
    }

    public void setBrm(final BRMJson brm) {
        this.brm = brm;
    }

    @Override
    public String toString() {
        return "BRMConfiguration [brm=" + brm + "]";
    }

}
