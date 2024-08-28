#include <cstdio>

#include "exception/FailedToDownloadException.hh"

#include "RestoreService.hh"

using namespace com::ericsson::adp::mgmt::data;

namespace BackupRestoreAgent
{

RestoreService::RestoreService(RestoreDataStream restoreStream, IStream* outStream)
    : restoreStream(std::move(restoreStream))
    , outStream(outStream)
{
}

RestoreService::~RestoreService()
{
    delete outStream;
    grpc::Status status = restoreStream->Finish();
    if (!status.ok())
    {
        std::cerr << "Cannot finish reading RestoreData stream!" << std::endl;
    }
}

void RestoreService::download(const std::string& restoreLocation)
{
    std::cerr << "RestoreService::download" << std::endl;
    try
    {
        RestoreData server_data;
        while (restoreStream->Read(&server_data))
        {
            parseServerData(server_data, restoreLocation);
        }
    }
    catch (...)
    {
        throw new FailedToDownloadException("Error at Orchestrator while downloading data during restore");
    }
}

void RestoreService::parseServerData(const RestoreData& server_data, const std::string& restoreLocation)
{
    switch (server_data.datamessagetype())
    {
    case DataMessageType::CUSTOM_METADATA_FILE:
        parseFileData(server_data, &RestoreData::custommetadatafilechunk, restoreLocation);
        break;
    case DataMessageType::BACKUP_FILE:
        parseFileData(server_data, &RestoreData::backupfilechunk, restoreLocation);
        break;
    default:
        break;
    }
}

template <class T, class FType>
void RestoreService::parseFileData(const T& server_data, FType getfilechunk, const std::string& restoreLocation)
{
    if ((server_data.*getfilechunk)().filename() != "")
    {
        restoreFile = restoreLocation + "/" + (server_data.*getfilechunk)().filename();
        outStream->open(restoreFile);
    }
    if ((server_data.*getfilechunk)().content() != "")
    {
        outStream->write((server_data.*getfilechunk)().content());
        calculator.addBytes((server_data.*getfilechunk)().content());
    }
    if ((server_data.*getfilechunk)().checksum() != "")
    {
        outStream->close();
        std::string checksum = calculator.getChecksum();
        if (checksum.compare((server_data.*getfilechunk)().checksum()) != 0)
        {
            std::cerr << "CHECKSUM mismatch: " << checksum << std::endl;
            std::remove(restoreFile.c_str());
        }
        calculator = ChecksumCalculator();
    }
}
}
