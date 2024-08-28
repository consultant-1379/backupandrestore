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

import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.CLIENT_USERNAME;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ClientIdentity holds the client credentials
 * to authenticate to the SSH server
 */
public class ClientIdentity {

    private String username;

    @JsonProperty("public-key")
    private PublicKey publicKey;

    /**
     * Default constructor needed by Jackson
     */
    public ClientIdentity() {
    }

    /**
     * Creates a client identity
     * @param username the username of the user
     * @param publicKey the object holding the asymmetric keys
     */
    public ClientIdentity(final String username, final PublicKey publicKey) {
        this.username = username;
        this.publicKey = publicKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(final PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    /**
     * Updates the property of the client identity based on the passed yang model property
     * @param updatedProperty the YANG model property name
     * @param newValue the new value of the property
     */
    public void updateProperty(final String updatedProperty, final String newValue) {
        if (updatedProperty.equalsIgnoreCase(CLIENT_USERNAME.toString())) {
            setUsername(newValue);
        } else {
            publicKey.updateProperty(updatedProperty, newValue);
        }
    }

    @Override
    public String toString() {
        return "ClientIdentity [username=" + username + ", publicKey=" + publicKey + "]";
    }

}