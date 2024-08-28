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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.operation;

import java.util.Arrays;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMBackupManagerJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchOperationJson;

/**
 * Patch to add a backupManager to the configuration.
 */
public class AddBackupManagerPatch extends BRMConfigurationPatch {

    private final BackupManager backupManager;

    /**
     * Creates Patch to add backupManager.
     * @param backupManager who will be added.
     */
    protected AddBackupManagerPatch(final BackupManager backupManager) {
        super(PatchOperation.ADD, INDEX_TO_ADD_ELEMENT);
        this.backupManager = backupManager;
    }

    @Override
    public List<PatchOperationJson> getJsonOfOperations() {
        return Arrays.asList(
                createOperationJson(path, new BRMBackupManagerJson(backupManager))
                );
    }

}
