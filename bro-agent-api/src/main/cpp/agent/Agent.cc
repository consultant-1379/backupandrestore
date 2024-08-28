#include "Agent.hh"
#include "AgentBehavior.hh"
#include "BackupExecutionActions.hh"
#include "OrchestratorControlMessageHandler.hh"
#include "RestoreExecutionActions.hh"

using namespace com::ericsson::adp::mgmt::control;

namespace BackupRestoreAgent
{

Agent::Agent(AgentBehavior* agentBehavior, OrchestratorGrpcChannel* channel)
    : agentBehavior(agentBehavior)
    , agentId(agentBehavior->getRegistrationInformation().getAgentId())
    , channel(channel)
    , softwareVersionInfo(agentBehavior->getRegistrationInformation().getSoftwareVersion())
{
}

Agent::~Agent()
{
    delete channel;
    delete agentBehavior;
    grpc_shutdown_blocking();
}

void Agent::clearBackupName()
{
    backupName = "";
}

void Agent::cleanUpAfterRestore()
{
    clearBackupName();
    getFragmentList().Clear();
    softwareVersionInfo = SoftwareVersionInfo();
}

void Agent::executeBackup()
{
    agentBehavior->executeBackup(new BackupExecutionActions(this));
}

void Agent::executeRestore()
{
    agentBehavior->executeRestore(new RestoreExecutionActions(this));
}

std::string Agent::getAgentId()
{
    return agentId;
}

std::string Agent::getBackupName()
{
    return backupName;
}

BackupDataStream Agent::getBackupStream(grpc::ClientContext* context)
{
    return std::move(channel->getBackupStream(context));
}

google::protobuf::RepeatedPtrField<Fragment> Agent::getFragmentList()
{
    return fragments;
}

grpc_connectivity_state Agent::getGrpcChannelStatus(bool tryToConnect)
{
    return grpcChannel->GetState(tryToConnect);
}

RestoreDataStream Agent::getRestoreStream(FragmentInformation fragment, grpc::ClientContext* context)
{
    return std::move(channel->getRestoreStream(agentId, backupName, fragment, context));
}

SoftwareVersionInfo Agent::getSoftwareVersionInfo()
{
    return softwareVersionInfo;
}

void Agent::registrate()
{
    std::cerr << "Agent start to registrate!" << std::endl;
    channel->establishControlChannel(new OrchestratorControlMessageHandler(this));
    channel->sendControlMessage(getRegistrationMessage());
}

void Agent::setGrpcChannel(std::shared_ptr<grpc::Channel> grpcChannel)
{
    this->grpcChannel = grpcChannel;
}

void Agent::sendStageCompleteMessage(bool success, const std::string& message, Action action)
{
    StageComplete* stageComplete = new StageComplete();
    stageComplete->set_success(success);
    stageComplete->set_agentid(agentId);
    stageComplete->set_message(message);

    AgentControl data;
    data.set_action(action);
    data.set_agentmessagetype(AgentMessageType::STAGE_COMPLETE);
    data.set_allocated_stagecomplete(stageComplete);
    channel->sendControlMessage(data);
}

void Agent::setBackupName(const std::string& backupName)
{
    this->backupName = backupName;
}

void Agent::setFragmentList(google::protobuf::RepeatedPtrField<Fragment> fragments)
{
    this->fragments = fragments;
}

void Agent::setSoftwareVersionInfo(SoftwareVersionInfo version)
{
    softwareVersionInfo = version;
}

AgentControl Agent::getRegistrationMessage()
{
    Register* reg = new Register();
    reg->set_agentid(agentId);
    reg->set_allocated_softwareversioninfo(new SoftwareVersionInfo(softwareVersionInfo));
    reg->set_apiversion(apiVersion);
    reg->set_scope(agentBehavior->getRegistrationInformation().getScope());

    AgentControl data;
    data.set_action(Action::REGISTER);
    data.set_agentmessagetype(AgentMessageType::REGISTER);
    data.set_allocated_register_(reg);

    return data;
}
}
