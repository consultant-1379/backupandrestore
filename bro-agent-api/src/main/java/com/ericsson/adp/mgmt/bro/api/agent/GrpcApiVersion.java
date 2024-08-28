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
package com.ericsson.adp.mgmt.bro.api.agent;

/**
 * An enum used to represent the agent's
 * GRPC API version
 */
public enum GrpcApiVersion {
    V2("2.0"), V3("3.0"), V4("4.0");

    private final String version;

    /**
     * An enum used to represent the agent's
     * GRPC API version
     * @param version the GRPC API version
     */
    GrpcApiVersion(final String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return this.version;
    }
}
