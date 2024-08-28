#pragma once

#include <grpc/grpc.h>
#include <grpcpp/channel.h>

#include "INT_BR_ORCH_CTRL.grpc.pb.h"

#include "Agent.hh"

namespace BackupRestoreAgent
{

/**
 * Responsible for creating agents.
 */
class AgentFactory
{
public:
    /**
     * Creates agent, establishing connection to orchestrator and registering it.
     *
     * @param host address of orchestrator.
     * @param port address of orchestrator.
     * @param agentBehavior specific agent behavior.
     * @return registered agent.
     */
    static Agent* createAgent(const std::string& host, const std::string& port, AgentBehavior* agentBehavior);

private:
    AgentFactory();
};

}
