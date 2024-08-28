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

import static com.ericsson.adp.mgmt.bro.api.util.SetTimeouts.TIMEOUT_SECONDS;

import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.ControlInterfaceGrpc;
import com.ericsson.adp.mgmt.control.OrchestratorControl;
import io.grpc.stub.StreamObserver;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;

/**
 * Creates a Control service allowing for interception of the messages on GRPC for unit testing.
 */
public class ControlService extends ControlInterfaceGrpc.ControlInterfaceImplBase {

    private final List<AgentControl> messages = new LinkedList<>();

    @Override
    public StreamObserver<AgentControl> establishControlChannel(final StreamObserver<OrchestratorControl> responseObserver) {
        return new StreamObserver<AgentControl>() {
            @Override
            public void onNext(final AgentControl value) {
                messages.add(value);
            }

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(final Throwable t) {

            }
        };
    }

    public AgentControl getMessage() {
        Awaitility.await().atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS).until(() -> messages.size() == 1);
        final AgentControl message = messages.remove(0);
        return message;
    }

}
