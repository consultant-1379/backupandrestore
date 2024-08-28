#pragma once

#include <grpc/grpc.h>
#include <grpcpp/channel.h>

#include "INT_BR_ORCH_CTRL.grpc.pb.h"
#include "INT_BR_ORCH_DATA.grpc.pb.h"

#include "fragment/BackupFragmentInformation.hh"

namespace BackupRestoreAgent
{

using ControlClientReaderWriter = grpc::ClientReaderWriterInterface<com::ericsson::adp::mgmt::control::AgentControl, com::ericsson::adp::mgmt::control::OrchestratorControl>;
using BackupDataClientWriter = grpc::ClientWriterInterface<com::ericsson::adp::mgmt::data::BackupData>;
using BackupDataStream = std::unique_ptr<BackupDataClientWriter>;
using RestoreDataClientReader = grpc::ClientReaderInterface<com::ericsson::adp::mgmt::data::RestoreData>;
using RestoreDataStream = std::unique_ptr<RestoreDataClientReader>;

class OrchestratorControlMessageHandler;

/**
 * Responsible for managing GRPC connection to orchestrator.
 */
class OrchestratorGrpcChannel
{
public:
    explicit OrchestratorGrpcChannel(std::unique_ptr<com::ericsson::adp::mgmt::control::ControlInterface::StubInterface> ctrlStub,
                                     std::unique_ptr<com::ericsson::adp::mgmt::data::DataInterface::StubInterface> dataStub);
    virtual ~OrchestratorGrpcChannel();

    /**
     * Establishes control channel to orchestrator.
     * @param streamObserver to handle messages from orchestrator.
     */
    void establishControlChannel(OrchestratorControlMessageHandler* messageHandler);
    /**
     * Gets stream to transmit backup data on.
     * @return stream.
     */
    BackupDataStream getBackupStream(grpc::ClientContext* context);
    /**
     * Gets stream to receive backup data on.
     * @return stream.
     */
    RestoreDataStream getRestoreStream(const std::string& agentId, const std::string& backupName, FragmentInformation fragment, grpc::ClientContext* context);
    /**
     * Sends control message to orchestrator.
     * @param message to be sent.
     */
    void sendControlMessage(com::ericsson::adp::mgmt::control::AgentControl message);
    /**
     * Starts a new thread where we will listen massages from BRO on control interface
     */
    void startListenOnControlChannel();

protected:
    std::shared_ptr<ControlClientReaderWriter> getControlStream();
    void registerAgain();
    bool shouldTryToRegisterAgain();

private:
    std::shared_ptr<ControlClientReaderWriter> controlStream{nullptr};
    std::unique_ptr<com::ericsson::adp::mgmt::control::ControlInterface::StubInterface> ctrlStub;
    std::unique_ptr<com::ericsson::adp::mgmt::data::DataInterface::StubInterface> dataStub;
    OrchestratorControlMessageHandler* messageHandler{nullptr};
    bool readyToListenMessages{false};
    grpc::ClientContext* context{nullptr};
    com::ericsson::adp::mgmt::control::AgentControl registerMessage;
    const int MAXIMUM_NUMBER_OF_ATTEMPTS_TO_REGISTER_AGAIN = 20;
    int attemptsToRegisterAgain{0};
};

}
