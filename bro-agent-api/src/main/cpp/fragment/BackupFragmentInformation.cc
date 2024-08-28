#include "BackupFragmentInformation.hh"

namespace BackupRestoreAgent
{

BackupFragmentInformation::BackupFragmentInformation(const std::string& fragmentId, const std::string& version, const std::string& sizeInBytes, const std::string& backupFilePath, const std::string& customMetadataFilePath)
    : FragmentInformation(fragmentId, version, sizeInBytes)
    , backupFilePath(backupFilePath)
    , customMetadataFilePath(customMetadataFilePath)
{
}

std::string BackupFragmentInformation::getBackupFilePath()
{
    return backupFilePath;
}

std::string BackupFragmentInformation::getCustomMetadataFilePath()
{
    return customMetadataFilePath;
}
}
