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
 * Patch to add a progressReport in the configuration.
 */
public class AddProgressReportInitialPatch extends ProgressReportPatch {
    final Action action;

    /**
     * Creates initial Patch for progressReport.
     * @param action to be added.
     * @param path where to add it.
     */
    public AddProgressReportInitialPatch(final Action action, final String path) {
        super(PatchOperation.ADD, path.substring(0, path.length() - 1), action);
        this.action =  action;
    }

    @Override
    protected List<PatchOperationJson> getJsonOfOperations() {
        final BRMProgressReportJson brmProgressReportJson = new BRMProgressReportJson(action);
        final BRMProgressReportJson[] arrBRMProgresReportJson = new BRMProgressReportJson[]{brmProgressReportJson};
        return Arrays.asList(createOperationJson(path, arrBRMProgresReportJson));
    }

}
