#pragma once

#include <string>

namespace BackupRestoreAgent
{

/**
 * Holds information for a fragment
 */
class FragmentInformation
{
public:
    explicit FragmentInformation(const std::string& fragmentId, const std::string& version, const std::string& sizeInBytes);

    std::string getFragmentId();
    std::string getVersion();
    std::string getSizeInBytes();

private:
    std::string fragmentId;
    std::string version;
    std::string sizeInBytes;
};

}
