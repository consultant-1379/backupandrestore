/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
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
 * Patch to add a the initial backup in the configuration.
 */
public class AddInitialBackupPatch extends BRMConfigurationPatch {

    private final Backup backup;

    /**
     * Creates initial Patch, including the array in value.
     * @param backup to be added.
     * @param pathToBackup where to add it.
     */
    public AddInitialBackupPatch(final Backup backup, final String pathToBackup) {
        super(PatchOperation.ADD, pathToBackup);
        this.backup = backup;
    }

    @Override
    public List<PatchOperationJson> getJsonOfOperations() {
        final BRMBackupJson brmBackupJson = new BRMBackupJson(backup);
        final BRMBackupJson[] arrBRMBackupJson = new BRMBackupJson[]{brmBackupJson};
        return Arrays.asList(createOperationJson(path, arrBRMBackupJson));
    }
}
