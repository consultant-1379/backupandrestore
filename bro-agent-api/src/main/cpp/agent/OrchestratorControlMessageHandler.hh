#pragma once

#include <grpc/grpc.h>
#include <grpcpp/channel.h>

#include "INT_BR_ORCH_CTRL.grpc.pb.h"

#include "Agent.hh"

namespace BackupRestoreAgent
{

/**
 * Provides a way to handle Orchestrator Control Messages
 */
class OrchestratorControlMessageHandler
{
public:
    /**
     * Creates an Orchestrator Control Message Handler for an Agent
     *
     * @param agent
     *            The agent that will act on the received messages.
     */
    explicit OrchestratorControlMessageHandler(Agent* agent);
    ~OrchestratorControlMessageHandler();

    /**
     * Checks a message from the Orchestrator to identify what needs to be done on the agent.
     *
     * @param message
     *            A control message from the orchestrator
     *
     */
    void process(com::ericsson::adp::mgmt::control::OrchestratorControl message);
private:
    void executeBackup(const std::string& backupName);
    void executeRestore(const com::ericsson::adp::mgmt::control::Preparation& preparation);

    Agent* agent;
};

}
