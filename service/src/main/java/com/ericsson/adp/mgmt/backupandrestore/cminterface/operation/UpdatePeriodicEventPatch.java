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

import static com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation.REPLACE;

import java.util.Arrays;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMPeriodicEventJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchOperationJson;

/**
 * Patch to update a periodic event in the configuration.
 */
public class UpdatePeriodicEventPatch extends BRMConfigurationPatch {

    private final PeriodicEvent event;
    /**
     * Creates Patch.
     * @param event periodic event to be updated.
     * @param path where to update it.
     */
    protected UpdatePeriodicEventPatch(final PeriodicEvent event, final String path) {
        super(REPLACE, path);
        this.event = event;
    }

    @Override
    protected List<PatchOperationJson> getJsonOfOperations() {
        return Arrays.asList(createOperationJson(path, new BRMPeriodicEventJson(event)));
    }
}
