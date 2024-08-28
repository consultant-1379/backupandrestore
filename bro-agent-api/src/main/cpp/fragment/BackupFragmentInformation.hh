#pragma once

#include <string>

#include "FragmentInformation.hh"

namespace BackupRestoreAgent
{

/**
 * Holds the fragment information, as well as where it is located.
 */
class BackupFragmentInformation : public FragmentInformation
{
public:
    /**
     * Use constructor to fill the structure
     */
    explicit BackupFragmentInformation(const std::string& fragmentId, const std::string& version, const std::string& sizeInBytes, const std::string& backupFilePath, const std::string& customMetadataPath = "");

    /**
     * @return the backupFilePath
     */
    std::string getBackupFilePath();
    /**
     * @return the customMetadataFilePath
     */
    std::string getCustomMetadataFilePath();

private:
    std::string backupFilePath;
    std::string customMetadataFilePath;
};

}
