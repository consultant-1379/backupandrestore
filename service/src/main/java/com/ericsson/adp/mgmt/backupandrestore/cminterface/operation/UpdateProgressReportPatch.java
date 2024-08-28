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

import com.ericsson.adp.mgmt.backupandrestore.action.Action;

/**
 * Patch to update a progressReport in the configuration.
 */
public class UpdateProgressReportPatch extends ProgressReportPatch {

    /**
     * Creates Patch.
     * @param action to be updated.
     * @param path where to add it.
     */
    protected UpdateProgressReportPatch(final Action action, final String path) {
        super(PatchOperation.REPLACE, path + "0", action);
    }
}
