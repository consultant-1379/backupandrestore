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

package com.ericsson.adp.mgmt.backupandrestore.agent.exception;

import java.text.MessageFormat;

/**
 * Attempt to use invalid API Version String.
 */
public class InvalidAPIVersionStringException extends RuntimeException {
    private static final long serialVersionUID = 8827497976424283347L;

    /**
     * Create {@link Exception} with apiVersion.
     * @param apiVersion - a String representing an invalid API version.
     */
    public InvalidAPIVersionStringException(final String apiVersion) {
        super(MessageFormat.format("{0} is not a valid API version string.", apiVersion));
    }
}
