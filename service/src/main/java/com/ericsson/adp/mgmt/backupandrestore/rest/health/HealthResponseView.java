/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */
package com.ericsson.adp.mgmt.backupandrestore.rest.health;

/**
 * Class used for filtering the fields that
 * are returned in the HealthResponse JSON object
 */
public class HealthResponseView {

    private HealthResponseView() {}

    /**
     * Sets the JSON View for V1 - V3 HealthResponse object
     */
    public interface V1 {}

    /**
     * Sets the JSON View for V4 HealthResponse object
     */
    public interface V4 {}
}
