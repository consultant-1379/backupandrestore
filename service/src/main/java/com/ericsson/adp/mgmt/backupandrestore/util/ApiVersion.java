/*
 *  ******************************************************************************
 *  COPYRIGHT Ericsson 2020
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 *  *******************************************************************************
 *
 */
package com.ericsson.adp.mgmt.backupandrestore.util;

import com.ericsson.adp.mgmt.backupandrestore.agent.exception.InvalidAPIVersionStringException;

/**
 * API Versions of agents
 */
public enum ApiVersion {
    API_V1_0("1.0"),
    API_V2_0("2.0"),
    API_V3_0("3.0"),
    API_V4_0("4.0");
    public final String stringRepresentation;

    /**
     * API Version.
     * @param stringRepresentation string representation of Api Version.
     */
    ApiVersion(final String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    public String getStringRepresentation() {
        return stringRepresentation;
    }

    /**
     * Get api {@link ApiVersion} from {@link String}.
     * @param version - a string with a version number.
     * @return - an ApiVersion that matches the String
     */
    public static ApiVersion fromString(final String version) {
        for (final ApiVersion apiVersion : ApiVersion.values()) {
            if (apiVersion.getStringRepresentation().equals(version)) {
                return apiVersion;
            }
        }
        throw new InvalidAPIVersionStringException(version);
    }

}
