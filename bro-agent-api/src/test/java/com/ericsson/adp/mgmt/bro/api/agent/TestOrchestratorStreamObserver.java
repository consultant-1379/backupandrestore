/*
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 * ****************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.bro.api.agent;

/**
 * Used by multiple Tests to fake a stub for grpc allows for testing behaviors.
 */
public class TestOrchestratorStreamObserver extends OrchestratorStreamObserver {

    public TestOrchestratorStreamObserver(final Agent agent) {
        super(agent);
    }

    @Override
    public void onError(final Throwable throwable) {

    }
}
