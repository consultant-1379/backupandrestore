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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ServerLocalDefinition holds a list of SSH host keys
 */
public class ServerLocalDefinition{
    @JsonProperty("host-key")
    private List<String> hostKeys =  new CopyOnWriteArrayList<>();

    public List<String> getHostKeys() {
        return hostKeys;
    }

    public void setHostKeys(final List<String> hostKeys) {
        this.hostKeys = hostKeys;
    }

    /**
     * Adds a key to the list of host keys
     * @param key the key to add
     */
    public synchronized void addKey(final String key) {
        if (!hostKeys.contains(key)) {
            hostKeys.add(key);
        }
    }

    /**
     * Removes a key at the specified index
     * @param index the index of the host key to be remved
     */
    public synchronized void removeKey(final int index) {
        hostKeys.remove(index);
    }

    /**
     * Replaces the key at a given index
     * @param index the index of the host key to be replaced
     * @param newKey the new host key value
     */
    public synchronized void replaceKey(final int index, final String newKey) {
        hostKeys.set(index, newKey);
    }

    @Override
    public String toString() {
        return "LocalDefinition [hostKeys=" + hostKeys + "]";
    }
}