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

import java.util.Arrays;


/**
 * Holds the list of {@link Endpoint endpoints}
 */
public class Endpoints {

    private static final int MAX_ENDPOINT = 1;
    /**
     * The list of endpoints will only have one item max
     */
    private Endpoint[] endpoint = new Endpoint[MAX_ENDPOINT];

    public Endpoint[] getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(final Endpoint[] endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Adds an endpoint to the list of endpoints.
     * As BRO only supports one endpoint, calling this method
     * will replace the existing endpoint in the list.
     * @param newEndpoint the new endpoint to be added
     */
    public void addEndpoint(final Endpoint newEndpoint) {
        endpoint[0] = newEndpoint;
    }

    @Override
    public String toString() {
        return "Endpoints [endpoint=" + Arrays.toString(endpoint) + "]";
    }
}