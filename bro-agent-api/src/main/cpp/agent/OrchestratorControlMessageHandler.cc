#include "OrchestratorControlMessageHandler.hh"

using namespace com::ericsson::adp::mgmt::control;

namespace BackupRestoreAgent
{

OrchestratorControlMessageHandler::OrchestratorControlMessageHandler(Agent* agent)
    : agent(agent)
{
}

OrchestratorControlMessageHandler::~OrchestratorControlMessageHandler()
{
}

void OrchestratorControlMessageHandler::process(OrchestratorControl message)
{
    switch (message.action())
    {
    case Action::BACKUP:
        std::cerr << "The Orchestrator has requested that the Agent creates backup: " << message.preparation().backupname() << std::endl;
        executeBackup(message.preparation().backupname());
        break;
    case Action::RESTORE:
        std::cerr << "The Orchestrator has requested that the Agent restores backup: " << message.preparation().backupname() << std::endl;
        executeRestore(message.preparation());
        break;
    default:
        std::cerr << "The Agent received an unknown message this message will be ignored" << std::endl;
        break;
    }
}

void OrchestratorControlMessageHandler::executeBackup(const std::string& backupName)
{
    agent->setBackupName(backupName);
    agent->executeBackup();
}

void OrchestratorControlMessageHandler::executeRestore(const com::ericsson::adp::mgmt::control::Preparation& preparation)
{
    agent->setBackupName(preparation.backupname());
    agent->setSoftwareVersionInfo(preparation.softwareversioninfo());
    agent->setFragmentList(preparation.fragment());
    agent->executeRestore();
}
}
