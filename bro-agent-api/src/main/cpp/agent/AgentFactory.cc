#include <grpcpp/create_channel.h>

#include "AgentFactory.hh"
#include "OrchestratorGrpcChannel.hh"

using namespace com::ericsson::adp::mgmt::control;
using namespace com::ericsson::adp::mgmt::data;

namespace BackupRestoreAgent
{

Agent* AgentFactory::createAgent(const std::string& host, const std::string& port, AgentBehavior* agentBehavior)
{
    std::string url = host + ":" + port;
    std::shared_ptr<grpc::Channel> grpcChannel(grpc::CreateChannel(url, grpc::InsecureChannelCredentials()));

    OrchestratorGrpcChannel* channel = new OrchestratorGrpcChannel(ControlInterface::NewStub(grpcChannel), DataInterface::NewStub(grpcChannel));
    Agent* newAgent = new Agent(agentBehavior, channel);
    newAgent->registrate();
    newAgent->setGrpcChannel(grpcChannel);
    return newAgent;
}
}
