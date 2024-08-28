#include <future>

#include "OrchestratorGrpcChannel.hh"
#include "OrchestratorControlMessageHandler.hh"

using namespace com::ericsson::adp::mgmt::control;
using namespace com::ericsson::adp::mgmt::data;

namespace BackupRestoreAgent
{

OrchestratorGrpcChannel::OrchestratorGrpcChannel(
    std::unique_ptr<com::ericsson::adp::mgmt::control::ControlInterface::StubInterface> ctrlStub,
    std::unique_ptr<com::ericsson::adp::mgmt::data::DataInterface::StubInterface> dataStub)
    : ctrlStub(std::move(ctrlStub))
    , dataStub(std::move(dataStub))
{
}

OrchestratorGrpcChannel::~OrchestratorGrpcChannel()
{
    readyToListenMessages = false;
    if (controlStream != nullptr)
    {
        controlStream->WritesDone();
        controlStream->Finish();
    }
    delete messageHandler;
    delete context;
}

void OrchestratorGrpcChannel::establishControlChannel(OrchestratorControlMessageHandler* messageHandler)
{
    std::cerr << "Establishing connection to orchestrator!" << std::endl;
    context = new grpc::ClientContext();
    this->messageHandler = messageHandler;
    controlStream = ctrlStub->establishControlChannel(context);
    std::thread listenThread([&]() {
        startListenOnControlChannel();
    });
    listenThread.detach();
}

BackupDataStream OrchestratorGrpcChannel::getBackupStream(grpc::ClientContext* context)
{
    google::protobuf::Empty empty;
    return BackupDataStream(std::move(dataStub->backup(context, &empty)));
}

RestoreDataStream OrchestratorGrpcChannel::getRestoreStream(const std::string& agentId, const std::string& backupName, FragmentInformation fragmentInformation, grpc::ClientContext* context)
{
    Fragment* fragment = new Fragment();
    fragment->set_fragmentid(fragmentInformation.getFragmentId());
    fragment->set_version(fragmentInformation.getVersion());
    fragment->set_sizeinbytes(fragmentInformation.getSizeInBytes());

    Metadata metaData;
    metaData.set_agentid(agentId);
    metaData.set_backupname(backupName);
    metaData.set_allocated_fragment(fragment);
    return std::move(RestoreDataStream(std::move(dataStub->restore(context, metaData))));
}

void OrchestratorGrpcChannel::sendControlMessage(AgentControl message)
{
    std::cerr << "Control message was sent to orchestrator: " << message.agentmessagetype() << std::endl;
    controlStream->Write(message);
    if (message.agentmessagetype() == AgentMessageType::REGISTER)
    {
        readyToListenMessages = true;
        registerMessage = message;
    }
}

std::shared_ptr<ControlClientReaderWriter> OrchestratorGrpcChannel::getControlStream()
{
    return controlStream;
}

void OrchestratorGrpcChannel::registerAgain()
{
    std::cerr << "Trying to register again!" << std::endl;
    controlStream->Write(registerMessage);
}

bool OrchestratorGrpcChannel::shouldTryToRegisterAgain()
{
    return attemptsToRegisterAgain < MAXIMUM_NUMBER_OF_ATTEMPTS_TO_REGISTER_AGAIN;
}

void OrchestratorGrpcChannel::startListenOnControlChannel()
{
    while (!readyToListenMessages)
    {
        std::cerr << "Waiting to be ready for listening!\n";
        std::this_thread::sleep_for(std::chrono::milliseconds(50));
    }

    std::cerr << "Start to listen to orchestrator CTRL messages!" << std::endl;
    OrchestratorControl server_data;
    while (readyToListenMessages)
    {
        while (controlStream->Read(&server_data))
        {
            messageHandler->process(server_data);
        }
        if (shouldTryToRegisterAgain()) {
            attemptsToRegisterAgain++;
            registerAgain();
        }
    }
}
}
