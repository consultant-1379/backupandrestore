#pragma once

#include "agent/OrchestratorGrpcChannel.hh"
#include "fragment/BackupFragmentInformation.hh"
#include "util/FileChunkServiceUtil.hh"

namespace BackupRestoreAgent
{

/**
 * Backup Service streams data to backup in chunks
 */
class BackupService
{
public:
    /**
     * Creates BackupService.
     * @param backupStream
     *            The stream to use to send the backup data to the orchestrator
     */
    explicit BackupService(BackupDataStream backupStream, IChunkServiceUtil* chunkService);
    ~BackupService();
    /**
     * Sends chunk of files and checksum
     *
     * @param agentId
     *            The ID of this agent
     * @param backupName
     *            The name given on request of the backup
     */
    void backup(BackupFragmentInformation fragmentInformation, const std::string& agentId, const std::string& backupName);


private:
    com::ericsson::adp::mgmt::data::BackupData buildBackupData(const std::string& fileName, const std::string& content, const std::string& checksum);
    com::ericsson::adp::mgmt::data::BackupData buildBackupCustomData(const std::string& fileName, const std::string& content, const std::string& checksum);
    void sendBackupMetadata(const std::string& agentId, BackupFragmentInformation fragmentInformation, const std::string& backupName);
    void sendCustomFile(const std::string& path);
    void sendFile(const std::string& path);

    BackupDataStream backupStream;
    IChunkServiceUtil* chunkService;
};

}
