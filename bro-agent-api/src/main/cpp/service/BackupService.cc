#include <fstream>
#include <streambuf>

#include <boost/filesystem.hpp>

#include "exception/FailedToTransferBackupException.hh"
#include "util/ChecksumCalculator.hh"

#include "BackupService.hh"

using namespace com::ericsson::adp::mgmt::data;

namespace BackupRestoreAgent
{

BackupService::BackupService(BackupDataStream backupStream, IChunkServiceUtil* chunkService)
    : backupStream(std::move(backupStream))
    , chunkService(chunkService)
{
}

BackupService::~BackupService()
{
    delete chunkService;
}

void BackupService::backup(BackupFragmentInformation fragmentInformation, const std::string& agentId, const std::string& backupName)
{
    std::cerr << "BackupService::backup" << std::endl;
    try
    {
        sendBackupMetadata(agentId, fragmentInformation, backupName);
        sendFile(fragmentInformation.getBackupFilePath());
        if (!fragmentInformation.getCustomMetadataFilePath().empty())
        {
            sendCustomFile(fragmentInformation.getCustomMetadataFilePath());
        }
        backupStream->WritesDone();
        backupStream->Finish();
    }
    catch (...)
    {
        throw new FailedToTransferBackupException("There was an error while trying to transfer: " + fragmentInformation.getBackupFilePath());
    }
}

BackupData BackupService::buildBackupData(const std::string& fileName, const std::string& content, const std::string& checksum)
{
    BackupData data;
    data.set_datamessagetype(DataMessageType::BACKUP_FILE);
    BackupFileChunk* backupFileChunk = new BackupFileChunk();
    backupFileChunk->set_filename(fileName);
    backupFileChunk->set_content(content.c_str());
    backupFileChunk->set_checksum(checksum);

    data.set_allocated_backupfilechunk(backupFileChunk);
    return data;
}

BackupData BackupService::buildBackupCustomData(const std::string& fileName, const std::string& content, const std::string& checksum)
{
    BackupData data;
    data.set_datamessagetype(DataMessageType::CUSTOM_METADATA_FILE);
    CustomMetadataFileChunk* metadataFileChunk = new CustomMetadataFileChunk();
    metadataFileChunk->set_filename(fileName);
    metadataFileChunk->set_content(content.c_str());
    metadataFileChunk->set_checksum(checksum);

    data.set_allocated_custommetadatafilechunk(metadataFileChunk);
    return data;
}

void BackupService::sendBackupMetadata(const std::string& agentId, BackupFragmentInformation fragmentInformation, const std::string& backupName)
{
    std::cerr << "BackupService::sendBackupMetadata" << std::endl;
    Fragment* fragment = new Fragment();
    fragment->set_fragmentid(fragmentInformation.getFragmentId());
    fragment->set_version(fragmentInformation.getVersion());
    fragment->set_sizeinbytes(fragmentInformation.getSizeInBytes());

    Metadata* metaData = new Metadata();
    metaData->set_agentid(agentId);
    metaData->set_backupname(backupName);
    metaData->set_allocated_fragment(fragment);

    BackupData data;
    data.set_datamessagetype(DataMessageType::METADATA);
    data.set_allocated_metadata(metaData);
    backupStream->Write(data);
}

void BackupService::sendCustomFile(const std::string& path)
{
    std::cerr << "BackupService::sendCustomFile" << std::endl;
    boost::filesystem::path filePath(path);
    std::string filename = filePath.filename().string();
    ChecksumCalculator calculator;

    chunkService->setPath(path);
    IChunkServiceUtil::Consumer consumer = [&](const char* chunk, int chunkSize) {
        calculator.addBytes(std::string(chunk, chunkSize));
        backupStream->Write(buildBackupCustomData("", std::string(chunk, chunkSize), ""));
    };

    std::cerr << "Sending message with file name" << std::endl;
    backupStream->Write(buildBackupCustomData(filename, "", ""));
    std::cerr << "Sending message(s) with content" << std::endl;
    chunkService->processChunks(consumer);
    std::cerr << "Sending message with checksum" << std::endl;
    backupStream->Write(buildBackupCustomData("", "", calculator.getChecksum()));
}

void BackupService::sendFile(const std::string& path)
{
    std::cerr << "BackupService::sendFile" << std::endl;
    boost::filesystem::path filePath(path);
    std::string filename = filePath.filename().string();
    ChecksumCalculator calculator;

    IChunkServiceUtil::Consumer consumer = [&](const char* chunk, int chunkSize) {
        calculator.addBytes(std::string(chunk, chunkSize));
        backupStream->Write(buildBackupData("", std::string(chunk, chunkSize), ""));
    };

    std::cerr << "Sending message with file name" << std::endl;
    backupStream->Write(buildBackupData(filename, "", ""));
    std::cerr << "Sending message(s) with content" << std::endl;
    chunkService->processChunks(consumer);
    std::cerr << "Sending message with checksum" << std::endl;
    backupStream->Write(buildBackupData("", "", calculator.getChecksum()));
}
}
