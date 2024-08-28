#include "agent/AgentBehavior.hh"
#include "agent/AgentFactory.hh"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace com::ericsson::adp::mgmt::control;
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

class TestAgentFactory : public ::testing::Test
{
public:
    TestAgentFactory(){};
    ~TestAgentFactory(){};
};

TEST_F(TestAgentFactory, factory_can_produce_valid_agent)
{
    TestBehavior behavior;
    Agent* agent = AgentFactory::createAgent("localhost", "3000", &behavior);
    agent->setBackupName("test");
    EXPECT_EQ(agent->getAgentId(), "TestAgent");
}

int main(int argc, char** argv)
{
    // The following line must be executed to initialize Google Mock
    // (and Google Test) before running the tests.
    ::testing::InitGoogleMock(&argc, argv);
    return RUN_ALL_TESTS();
}
