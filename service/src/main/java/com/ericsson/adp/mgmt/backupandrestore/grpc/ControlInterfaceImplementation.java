/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.grpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentInputStream;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentOutputStream;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentRepository;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;
import com.ericsson.adp.mgmt.control.AgentControl;
import com.ericsson.adp.mgmt.control.ControlInterfaceGrpc.ControlInterfaceImplBase;
import com.ericsson.adp.mgmt.control.OrchestratorControl;

import io.grpc.stub.StreamObserver;

/**
 *Implements the control interface, INT_BR_ORCH_CTRL.
 */
@Service
public class ControlInterfaceImplementation extends ControlInterfaceImplBase {

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private IdValidator idValidator;

    @Override
    public StreamObserver<AgentControl> establishControlChannel(final StreamObserver<OrchestratorControl> orchestratorControlStream) {
        return createAgentOutputStream(createAgent(orchestratorControlStream));
    }

    private Agent createAgent(final StreamObserver<OrchestratorControl> orchestratorControlStream) {
        return new Agent(new AgentInputStream(orchestratorControlStream), agentRepository, idValidator);
    }

    private AgentOutputStream createAgentOutputStream(final Agent agent) {
        return new AgentOutputStream(agent);
    }

}
