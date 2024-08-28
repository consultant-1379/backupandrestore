#pragma once

#include <grpc/grpc.h>
#include <grpcpp/channel.h>

#include "INT_BR_ORCH_CTRL.grpc.pb.h"

#include "OrchestratorGrpcChannel.hh"

namespace BackupRestoreAgent
{

class AgentBehavior;

/**
 * Represents agent, that listens to messages from orchestrator and replies back.
 */
class Agent
{
public:
    /**
     * Creates agent with specific behavior.
     * @param agentBehavior specific agent behavior.
     * @param channel to orchestrator.
     */
    explicit Agent(AgentBehavior* agentBehavior, OrchestratorGrpcChannel* channel);
    virtual ~Agent();

    /**
     * Send registration message to orchestrator.
     */
    virtual void registrate();
    /**
     * Provides a way to clear the backup name upon completion of a backup action.
     */
    void clearBackupName();
    /**
     * Provides a way to clear the backup name, fragmentList and SoftwareVersionInfo upon completion of a restore action.
     */
    void cleanUpAfterRestore();
    /**
     * execute a backup
     */
    virtual void executeBackup();
    /**
     * Executes a Restore
     */
    virtual void executeRestore();
    std::string getAgentId();
    std::string getBackupName();
    /**
     * Provides stream to send backup data.
     * @return backup stream.
     */
    BackupDataStream getBackupStream(grpc::ClientContext* context);
    google::protobuf::RepeatedPtrField<Fragment> getFragmentList();
    /**
     * Use this method if you would like to check the gRPC channel status
     * See: https://grpc.github.io/grpc/cpp/md_doc_connectivity-semantics-and-api.html
     */
    grpc_connectivity_state getGrpcChannelStatus(bool tryToConnect);
    /**
     * Provides stream to get restore data.
     * @param fragment info for the restore
     * @return stream which provides the restore data.
     */
    RestoreDataStream getRestoreStream(FragmentInformation fragment, grpc::ClientContext* context);
    SoftwareVersionInfo getSoftwareVersionInfo();
    /**
     * Set grpcChannel object which holds state informations
     * @param the object of the grpc channel
     */
    void setGrpcChannel(std::shared_ptr<grpc::Channel> grpcChannel);
    /**
     * Sends a stage complete message with the supplied information
     * @param success to indicate if the action was successful
     * @param message to be logged
     * @param action type
     */
    void sendStageCompleteMessage(bool success, const std::string& message, Action action);
    void setBackupName(const std::string& backupName);
    void setFragmentList(google::protobuf::RepeatedPtrField<Fragment> fragments);
    void setSoftwareVersionInfo(SoftwareVersionInfo version);

private:
    com::ericsson::adp::mgmt::control::AgentControl getRegistrationMessage();

    std::string apiVersion{"1.0"};
    AgentBehavior* agentBehavior;
    std::string agentId;
    std::string backupName;
    OrchestratorGrpcChannel* channel;
    std::shared_ptr<grpc::Channel> grpcChannel{nullptr};
    google::protobuf::RepeatedPtrField<Fragment> fragments;
    SoftwareVersionInfo softwareVersionInfo;
};

}
