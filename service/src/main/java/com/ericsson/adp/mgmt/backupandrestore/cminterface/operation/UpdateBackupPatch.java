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
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchOperationJson;

/**
 * Patch to update a backup in the configuration.
 */
public class UpdateBackupPatch extends BRMConfigurationPatch {

    private final Backup backup;

    /**
     * Creates Patch.
     * @param backup to be updated.
     * @param pathToBackup where to update it.
     */
    protected UpdateBackupPatch(final Backup backup, final String pathToBackup) {
        super(PatchOperation.REPLACE, pathToBackup);
        this.backup = backup;
    }

    @Override
    public List<PatchOperationJson> getJsonOfOperations() {
        return Arrays.asList(createOperationJson(path + "/status", backup.getStatus().getCmRepresentation()));
    }

}
