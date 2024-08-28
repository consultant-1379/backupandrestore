/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup.storage;

import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Versioned;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Metadata relating to the BR-Internal-Storage interface.
 *
 * The Versioned<> implementation here is mostly cursory - demanded by the type system, but not actually needed, since
 * we never read this file. If we ever implement a copy constructor here, the Version<T> information will need to be
 * copied as well.
 */
public class StorageMetadata implements Versioned<StorageMetadata> {

    private String version;
    private Version<StorageMetadata> persistVersion;

    /**
     * Constructor
     *
     * @param version The BR-Internal-Storage version
     */
    public StorageMetadata(final String version) {
        this.version = version;
    }

    public String getMetadataVersion() {
        return version;
    }

    public void setMetadataVersion(final String version) {
        this.version = version;
    }

    @Override
    @JsonIgnore
    public Version<StorageMetadata> getVersion() {
        return persistVersion;
    }

    @Override
    @JsonIgnore
    public void setVersion(final Version<StorageMetadata> version) {
        this.persistVersion = version;
    }
}
