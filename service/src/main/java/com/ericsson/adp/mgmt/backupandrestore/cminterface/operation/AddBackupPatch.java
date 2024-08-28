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

import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMBackupJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchOperationJson;

/**
 * Patch to add a backup in the configuration.
 */
public class AddBackupPatch extends BRMConfigurationPatch {

    private final Backup backup;

    /**
     * Creates Patch.
     * @param backup to be added.
     * @param pathToBackup where to add it.
     */
    protected AddBackupPatch(final Backup backup, final String pathToBackup) {
        super(PatchOperation.ADD, pathToBackup + "/" + INDEX_TO_ADD_ELEMENT);
        this.backup = backup;
    }

    @Override
    public List<PatchOperationJson> getJsonOfOperations() {
        return Arrays.asList(createOperationJson(path, new BRMBackupJson(backup)));
    }

}
