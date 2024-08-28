#include "service/BackupService.hh"

#include "Agent.hh"
#include "BackupExecutionActions.hh"

namespace BackupRestoreAgent
{

BackupExecutionActions::BackupExecutionActions(Agent* agent)
    : agent(agent)
{
}

void BackupExecutionActions::sendBackup(BackupFragmentInformation fragmentInformation)
{
    std::cerr << "BackupExecutionActions::sendBackup" << std::endl;
    grpc::ClientContext context;
    BackupService backupService(std::move(agent->getBackupStream(&context)), new FileChunkServiceUtil(fragmentInformation.getBackupFilePath()));
    backupService.backup(fragmentInformation, agent->getAgentId(), agent->getBackupName());
}

void BackupExecutionActions::backupComplete(bool success, const std::string& message)
{
    std::cerr << "BackupExecutionActions::backupComplete" << std::endl;
    agent->sendStageCompleteMessage(success, message, Action::BACKUP);
    agent->clearBackupName();
}

std::string BackupExecutionActions::getBackupName()
{
    return agent->getBackupName();
}
}
