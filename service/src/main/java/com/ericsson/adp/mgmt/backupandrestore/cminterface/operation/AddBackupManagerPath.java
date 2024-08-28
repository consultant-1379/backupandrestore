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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchOperationJson;

/**
 * Class used to create the backup-manager array in configuration
 */
public class AddBackupManagerPath extends BRMConfigurationPatch {
    /**
     * Creates patch.
     */
    public AddBackupManagerPath() {
        super(PatchOperation.ADD);
    }

    @Override
    protected List<PatchOperationJson> getJsonOfOperations() {
        return Arrays.asList(createOperationJson(path, new ArrayList<>()));
    }
}
