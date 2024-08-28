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

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.BRMProgressReportJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchOperationJson;

/**
 * A progress report patch in the configuration.
 */
public abstract class ProgressReportPatch extends BRMConfigurationPatch{
    private final Action action;

    /**
     * Creates a progress report patch.
     * @param operation the patch operation
     * @param path the path for which the patch will be applied to.
     * @param action the action corresponding to the progress report.
     */
    public ProgressReportPatch(final PatchOperation operation, final String path, final Action action) {
        super(operation, path);
        this.action = action;
    }

    @Override
    protected List<PatchOperationJson> getJsonOfOperations() {
        return Arrays.asList(createOperationJson(path, new BRMProgressReportJson(action)));
    }

    public Action getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "ProgressReportPatch [action=" + action + ", operation=" + operation + ", path=" + path + "]";
    }
}
