/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver;

import static com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation.ADD;
import static com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation.REMOVE;
import static com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation.REPLACE;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *  The class which holds the list of SSH host keys
 *  used by the SSH client to authenticate SSH server host keys.
 */
public class SSHHostKeys{

    @JsonProperty("local-definition")
    private ServerLocalDefinition localDefinition;

    /**
     * Default constructor used by Jackson.
     */
    public SSHHostKeys() {
    }

    /**
     * Creates a SSHHostKeys with a local definition
     * @param localDefinition the local definition
     */
    public SSHHostKeys(final ServerLocalDefinition localDefinition) {
        this.localDefinition = localDefinition;
    }

    public ServerLocalDefinition getLocalDefinition() {
        return localDefinition;
    }

    public void setLocalDefinition(final ServerLocalDefinition localDefinition) {
        this.localDefinition = localDefinition;
    }

    @Override
    public String toString() {
        return "SSHHostKeys [localDefinition=" + localDefinition + "]";
    }

    /**
     * Patches the list of host keys
     * @param newHostKey the new host key
     * @param operation the patch operation
     * @param index the host key index
     */
    public void patchHostKey(final String newHostKey, final String operation, final int index) {
        if (operation.equals(ADD.getStringRepresentation())) {
            localDefinition.addKey(newHostKey);
        } else if (operation.equals(REPLACE.getStringRepresentation())) {
            localDefinition.replaceKey(index, newHostKey);
        } else if (operation.equals(REMOVE.getStringRepresentation())) {
            localDefinition.removeKey(index);
        }
    }
}