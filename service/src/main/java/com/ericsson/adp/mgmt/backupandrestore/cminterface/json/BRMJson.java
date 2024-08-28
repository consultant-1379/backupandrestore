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

import java.util.ArrayList;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;


/**
 * BRM model.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BRMJson {

    private List<BRMBackupManagerJson> backupManagers = new ArrayList<>();

    /**
     * Default constructor, to be used by Jackson.
     */
    public BRMJson() {
    }

    /**
     * Creates BRM.
     * @param backupManagers belonging to BRM.
     */
    public BRMJson(final List<BackupManager> backupManagers) {
        backupManagers
            .stream()
            .map(BRMBackupManagerJson::new)
            .forEach(this.backupManagers::add);
    }

    @JsonProperty("backup-manager")
    public List<BRMBackupManagerJson> getBackupManagers() {
        return backupManagers.isEmpty() ? null : backupManagers;
    }

    public void setBackupManagers(final List<BRMBackupManagerJson> backupManagers) {
        this.backupManagers = backupManagers;
    }

    @Override
    public String toString() {
        return "BRMJson [backupManagers=" + backupManagers + "]";
    }
}
