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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.operation;

import java.util.Arrays;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.Housekeeping;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchOperationJson;

/**
 * Patch to update a housekeeping in the configuration.
 */
public class UpdateHousekeepingPatch extends BRMConfigurationPatch {

    private final Housekeeping housekeeping;

    /**
     * @param housekeeping to be updated
     * @param path to Housekeeping
     */
    public UpdateHousekeepingPatch(final Housekeeping housekeeping, final String path) {
        super(PatchOperation.REPLACE, path);
        this.housekeeping = housekeeping;
    }

    @Override
    protected List<PatchOperationJson> getJsonOfOperations() {
        return Arrays.asList(
                createOperationJson(path + "auto-delete", housekeeping.getAutoDelete()),
                createOperationJson(path + "max-stored-manual-backups", housekeeping.getMaxNumberBackups())
                );
    }

}
