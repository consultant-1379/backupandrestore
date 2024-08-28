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
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMHousekeepingJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchOperationJson;

/**
 * Patch to add a housekeeping to the configuration.
 */
public class AddHousekeepingPatch extends BRMConfigurationPatch {

    private final Housekeeping housekeeping;
    /**
     * @param housekeeping to be added
     * @param path to housekeeping
     */
    protected AddHousekeepingPatch(final Housekeeping housekeeping, final String path) {
        super(PatchOperation.ADD, path);
        this.housekeeping = housekeeping;
    }

    @Override
    protected List<PatchOperationJson> getJsonOfOperations() {
        return Arrays.asList(
                createOperationJson(path, new BRMHousekeepingJson(housekeeping))
                );
    }

}
