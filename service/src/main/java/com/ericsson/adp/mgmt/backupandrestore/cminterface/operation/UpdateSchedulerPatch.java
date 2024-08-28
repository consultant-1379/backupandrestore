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

import static com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation.REPLACE;

import java.util.Arrays;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.scheduler.Scheduler;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMSchedulerJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchOperationJson;

/**
 * Patch to update a housekeeping in the configuration.
 */
public class UpdateSchedulerPatch extends BRMConfigurationPatch {

    private final Scheduler scheduler;
    private final List<Action> actions;

    /**
     * @param scheduler to be updated
     * @param actions list of actions in the brm
     * @param path to scheduler
     */
    protected UpdateSchedulerPatch(final Scheduler scheduler, final List<Action> actions, final String path) {
        super(REPLACE, path);
        this.scheduler = scheduler;
        this.actions = actions;
    }

    @Override
    protected List<PatchOperationJson> getJsonOfOperations() {
        return Arrays.asList(
                createOperationJson(path, new BRMSchedulerJson(scheduler, actions))
                );
    }

}
