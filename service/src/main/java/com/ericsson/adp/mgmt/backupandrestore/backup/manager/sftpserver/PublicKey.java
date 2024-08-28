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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PublicKey represents a locally-defined or referenced asymmetric key pair
 * used for client authentication.
 */
public class PublicKey {
    @JsonProperty("local-definition")
    private ClientLocalDefinition localDefinition;

    /**
     * Default constructor used by Jackson.
     */
    public PublicKey() {
    }

    /**
     * Creates a PublicKey instance with a LocalDefinition object
     * @param localDefinition holds an asymmetric key pair to be used for client authentication
     */
    public PublicKey(final ClientLocalDefinition localDefinition) {
        this.localDefinition = localDefinition;
    }

    public ClientLocalDefinition getLocalDefinition() {
        return localDefinition;
    }

    public void setLocalDefinition(final ClientLocalDefinition localDefinition) {
        this.localDefinition = localDefinition;
    }

    /**
     * Updates the property of the public key based on the passed yang model property
     * @param updatedProperty the YANG model property name
     * @param newValue the new value of the property
     */
    public void updateProperty(final String updatedProperty, final String newValue) {
        localDefinition.updateProperty(updatedProperty, newValue);
    }

    @Override
    public String toString() {
        return "PublicKey [localDefinition=" + localDefinition + "]";
    }
}