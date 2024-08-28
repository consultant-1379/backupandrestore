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

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.periodic.PeriodicEvent;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMPeriodicEventJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchOperationJson;

/**
 * AddPeriodicEventPatch
 */
public class AddPeriodicEventPatch extends BRMConfigurationPatch {
    private final PeriodicEvent periodicEvent;

    /**
     * @param periodicEvent
     *            periodicEvent
     * @param path
     *            path to periodic event
     */
    public AddPeriodicEventPatch(final PeriodicEvent periodicEvent, final String path) {
        super(PatchOperation.ADD, path + INDEX_TO_ADD_ELEMENT);
        this.periodicEvent = periodicEvent;
    }

    @Override
    protected List<PatchOperationJson> getJsonOfOperations() {
        return Arrays.asList(createOperationJson(path, new BRMPeriodicEventJson(periodicEvent)));
    }

}
