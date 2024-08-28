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

/**
 * Represents a Patch to the BRM configuration.
 */
public abstract class BRMConfigurationPatch extends ConfigurationPatch {

    private static final String CONFIG_PREFIX = "/ericsson-brm:brm/";
    private static final String BACKUP_MANAGER_PREFIX = CONFIG_PREFIX + "backup-manager";
    private static final String CONTEXT_PREFIX = BACKUP_MANAGER_PREFIX + "/";

    /**
     * Creates patch.
     * @param operation to be performed.
     * @param path where to perform it.
     */
    protected BRMConfigurationPatch(final PatchOperation operation, final String path) {
        super(operation, CONTEXT_PREFIX + path);
    }

    /**
     * Returns the base of backup-manager used when configuration is created
     * @param operation to be performed.
     */
    protected BRMConfigurationPatch(final PatchOperation operation) {
        super(operation, BACKUP_MANAGER_PREFIX);
    }

}
