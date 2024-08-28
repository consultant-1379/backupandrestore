/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.persist.version;

/**
 * An object can be versioned
 * @param <T> Persisted Class
 */
public interface Versioned<T> {
    /**
     * return object persisted version
     * @return persisted version
     */
    Version<T> getVersion();

    /**
     * Set the version to object persisted
     * @param version version to be assigned
     */
    void setVersion(final Version<T> version);
}
