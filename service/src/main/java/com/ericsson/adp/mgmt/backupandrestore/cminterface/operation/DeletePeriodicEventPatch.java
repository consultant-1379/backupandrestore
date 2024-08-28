/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
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

import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchOperationJson;

/**
 * Patch to delete a periodic event in the configuration.
 */
public class DeletePeriodicEventPatch extends BRMConfigurationPatch {

    /**
     * Creates Patch.
     * @param pathToEvent where to remove.
     */
    protected DeletePeriodicEventPatch(final String pathToEvent) {
        super(PatchOperation.REMOVE, pathToEvent);
    }

    @Override
    public List<PatchOperationJson> getJsonOfOperations() {
        return Arrays.asList(createOperationJson(path, ""));
    }
}
