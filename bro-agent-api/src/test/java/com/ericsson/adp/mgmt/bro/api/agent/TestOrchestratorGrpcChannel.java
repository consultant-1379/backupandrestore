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

import com.ericsson.adp.mgmt.control.AgentControl;
import java.util.LinkedList;
import java.util.List;

/**
 * Test class used to get fake a grpc channel to the orchestrator by multiple tests.
 * Allows for interception and testing of messages
 */
public class TestOrchestratorGrpcChannel extends OrchestratorGrpcChannel {
    private final List<AgentControl> messages = new LinkedList<>();

    public TestOrchestratorGrpcChannel() {
        super(null);
    }

    @Override
    protected void sendControlMessage(final AgentControl message) {
        this.messages.add(message);
    }

    public AgentControl getMessage() {
        if (this.messages.isEmpty()) {
            return null;
        }
        final AgentControl message = this.messages.remove(0);
        return message;
    }
}