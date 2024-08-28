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

import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.CLIENT_LOCAL_PRIVATE_KEY;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.CLIENT_LOCAL_PUBLIC_KEY;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ClientLocalDefinition holds the client's local key definition.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientLocalDefinition {
    @JsonProperty("public-key")
    private String publicKey;

    @JsonProperty("private-key")
    private String privateKey;

    /**
     * Default constructor, to be used by Jackson.
     */
    public ClientLocalDefinition() {
    }

    /**
     * LocalDefinition wraps the local key definition
     * @param publicKey the binary value of the public key. The interpretation of the value is defined by 'public-key-format' field.
     * @param privateKey the value of the binary key. The key's value is interpreted by the 'private-key-format' field.
     */
    public ClientLocalDefinition(final String publicKey, final String privateKey) {
        super();
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(final String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(final String privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * Updates the property of the local definition based on the passed yang model property
     * @param updatedProperty the YANG model property name
     * @param newValue the new value of the property
     */
    public void updateProperty(final String updatedProperty, final String newValue) {
        if (updatedProperty.equalsIgnoreCase(CLIENT_LOCAL_PRIVATE_KEY.toString())) {
            setPrivateKey(newValue);
        } else if (updatedProperty.equalsIgnoreCase(CLIENT_LOCAL_PUBLIC_KEY.toString())) {
            setPublicKey(newValue);
        }
    }

    @Override
    public String toString() {
        return "LocalDefinition [publicKey=" + publicKey + ", privateKey=" + privateKey + "]";
    }

}