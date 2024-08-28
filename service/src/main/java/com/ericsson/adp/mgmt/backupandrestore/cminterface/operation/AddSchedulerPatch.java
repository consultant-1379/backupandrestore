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

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMSchedulerJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchOperationJson;

/**
 * Patch to add a scheduler to the configuration.
 */
public class AddSchedulerPatch extends BRMConfigurationPatch {

    private final Scheduler scheduler;

    /**
     * @param scheduler to be added
     * @param path to scheduler
     */
    protected AddSchedulerPatch(final Scheduler scheduler, final String path) {
        super(PatchOperation.ADD, path);
        this.scheduler = scheduler;
    }

    @Override
    protected List<PatchOperationJson> getJsonOfOperations() {
        return Arrays.asList(
                createOperationJson(path, new BRMSchedulerJson(scheduler))
                );
    }
}
