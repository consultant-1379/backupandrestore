#include <grpcpp/create_channel.h>

#include "agent/AgentBehavior.hh"
#include "agent/OrchestratorControlMessageHandler.hh"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace com::ericsson::adp::mgmt::control;
using namespace com::ericsson::adp::mgmt::data;
using namespace BackupRestoreAgent;
using ::testing::Exactly;

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

class MockAgent : public Agent
{
public:
    explicit MockAgent(AgentBehavior* agentBehavior, OrchestratorGrpcChannel* channel)
        : Agent(agentBehavior, channel){};
    MOCK_METHOD0(executeBackup, void());
    MOCK_METHOD0(executeRestore, void());
};

class TestOrchestratorControlMessageHandler : public ::testing::Test
{
public:
    TestOrchestratorControlMessageHandler()
        : agent(new MockAgent(new TestBehavior(),
              new OrchestratorGrpcChannel(std::move(ControlInterface::NewStub(grpc::CreateChannel("localhost:5432", grpc::InsecureChannelCredentials()))),
                  std::move(DataInterface::NewStub(grpc::CreateChannel("localhost:5432", grpc::InsecureChannelCredentials()))))))
        , handler(new OrchestratorControlMessageHandler(agent)){};
    ~TestOrchestratorControlMessageHandler()
    {
        delete agent;
        delete handler;
    };

    MockAgent* agent;
    OrchestratorControl message;
    OrchestratorControlMessageHandler* handler;
};

TEST_F(TestOrchestratorControlMessageHandler, process_will_call_agents_executeBackup)
{
    message.set_action(Action::BACKUP);

    EXPECT_CALL(*agent, executeBackup()).Times(Exactly(1));

    handler->process(message);
}

TEST_F(TestOrchestratorControlMessageHandler, process_will_call_agents_executeRestore)
{
    message.set_action(Action::RESTORE);

    EXPECT_CALL(*agent, executeRestore()).Times(Exactly(1));

    handler->process(message);
}

int main(int argc, char** argv)
{
    // The following line must be executed to initialize Google Mock
    // (and Google Test) before running the tests.
    ::testing::InitGoogleMock(&argc, argv);
    return RUN_ALL_TESTS();
}
