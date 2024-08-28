#include <grpcpp/create_channel.h>

#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include <grpcpp/test/mock_stream.h>

#include "agent/AgentBehavior.hh"
#include "agent/BackupExecutionActions.hh"
#include "agent/OrchestratorControlMessageHandler.hh"
#include "agent/OrchestratorGrpcChannel.hh"
#include "agent/RestoreExecutionActions.hh"

#include "INT_BR_ORCH_DATA_mock.grpc.pb.h"
#include "INT_BR_ORCH_CTRL_mock.grpc.pb.h"

using namespace com::ericsson::adp::mgmt::control;
using namespace com::ericsson::adp::mgmt::data;
using namespace BackupRestoreAgent;
using ::testing::_;
using ::testing::AtLeast;
using ::testing::Exactly;
using ::testing::Return;

class TestBehavior : public AgentBehavior
{
public:
    RegistrationInformation getRegistrationInformation() override
    {
        SoftwareVersionInfo info;
        info.set_productname("OaM-FW-backuprestoreclient");
        info.set_productnumber("0.1.0");
        info.set_revision("1");
        info.set_productiondate("2019-09-21");
        info.set_description("BackupRestore client used be traffic handlers.");
        info.set_type("Database");
        return RegistrationInformation("TestAgent", "alphascope", info);
    };
    void executeBackup(BackupExecutionActions* backupExecutionActions) override{};
    void executeRestore(RestoreExecutionActions* restoreExecutionActions) override{};
};

class MockOrchestratorControlMessageHandler : public OrchestratorControlMessageHandler
{
public:
    explicit MockOrchestratorControlMessageHandler(Agent* agent)
        : OrchestratorControlMessageHandler(agent)
    {};
    MOCK_METHOD1(process, void(OrchestratorControl));
};

class FakeOrchestratorGrpcChannel : public OrchestratorGrpcChannel
{
public:
    FakeOrchestratorGrpcChannel(std::unique_ptr<com::ericsson::adp::mgmt::control::ControlInterface::StubInterface> ctrlStub,
                                std::unique_ptr<com::ericsson::adp::mgmt::data::DataInterface::StubInterface> dataStub)
        : OrchestratorGrpcChannel(std::move(ctrlStub), std::move(dataStub)){};

    std::shared_ptr<ControlClientReaderWriter> getControlStream()
    {
        return OrchestratorGrpcChannel::getControlStream();
    };
};

class TestOrchestratorGrpcChannel : public ::testing::Test
{
public:
    TestOrchestratorGrpcChannel()
        : ctrlStub(new MockControlInterfaceStub())
        , ctrlStubRaw(ctrlStub.get())
        , dataStub(new MockDataInterfaceStub())
        , orchestratorChannel(std::move(ctrlStub), std::move(dataStub))
        , messageHandler(new MockOrchestratorControlMessageHandler(new Agent(new TestBehavior(), &orchestratorChannel)))
        , rw(new grpc::testing::MockClientReaderWriter<AgentControl, OrchestratorControl>())
    {
        //This will be freed by OrchestratorGrpcChannel class
        testing::Mock::AllowLeak(messageHandler);
    };
    ~TestOrchestratorGrpcChannel()
    {
    };

    std::unique_ptr<MockControlInterfaceStub> ctrlStub;
    MockControlInterfaceStub* ctrlStubRaw;
    std::unique_ptr<MockDataInterfaceStub> dataStub;
    FakeOrchestratorGrpcChannel orchestratorChannel;
    MockOrchestratorControlMessageHandler* messageHandler;
    grpc::testing::MockClientReaderWriter<AgentControl, OrchestratorControl>* rw;
};

TEST_F(TestOrchestratorGrpcChannel, grpc_channel_can_be_initialized)
{
    EXPECT_CALL(*rw, WritesDone()).Times(1);
    EXPECT_CALL(*rw, Finish()).Times(1);
    EXPECT_CALL(*ctrlStubRaw, establishControlChannelRaw(_)).Times(AtLeast(1)).WillOnce(Return(rw));
    EXPECT_EQ(orchestratorChannel.getControlStream(), nullptr);

    orchestratorChannel.establishControlChannel(messageHandler);

    EXPECT_NE(orchestratorChannel.getControlStream(), nullptr);
}

TEST_F(TestOrchestratorGrpcChannel, startListenOnControlChannel_will_process_messages)
{
    EXPECT_CALL(*messageHandler, process(::testing::_)).Times(Exactly(1));
    EXPECT_CALL(*rw, Write(_, _)).Times(1);
    EXPECT_CALL(*rw, WritesDone()).Times(1);
    EXPECT_CALL(*rw, Finish()).Times(1);
    EXPECT_CALL(*ctrlStubRaw, establishControlChannelRaw(_)).Times(AtLeast(1)).WillOnce(Return(rw));

    SoftwareVersionInfo* version = new SoftwareVersionInfo();
    version->set_productname("TestProduct");
    version->set_productnumber("1.0");
    version->set_revision("1.0");
    version->set_productiondate("1970-01-01");
    version->set_description("Test software");
    version->set_type("Test");

    Register* reg = new Register();
    reg->set_agentid("TestAgent");
    reg->set_allocated_softwareversioninfo(version);
    reg->set_apiversion("v0.0");
    reg->set_scope("TestScope");

    AgentControl data;
    data.set_action(Action::REGISTER);
    data.set_agentmessagetype(AgentMessageType::REGISTER);
    data.set_allocated_register_(reg);

    orchestratorChannel.establishControlChannel(messageHandler);
    orchestratorChannel.sendControlMessage(data);

    EXPECT_NE(orchestratorChannel.getControlStream(), nullptr);
}

int main(int argc, char** argv)
{
    // The following line must be executed to initialize Google Mock
    // (and Google Test) before running the tests.
    ::testing::InitGoogleMock(&argc, argv);
    return RUN_ALL_TESTS();
}
