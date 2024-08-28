#include "FragmentInformation.hh"

namespace BackupRestoreAgent
{

FragmentInformation::FragmentInformation(const std::string& fragmentId, const std::string& version, const std::string& sizeInBytes)
    : fragmentId(fragmentId)
    , version(version)
    , sizeInBytes(sizeInBytes)
{
}

std::string FragmentInformation::getFragmentId()
{
    return fragmentId;
}

std::string FragmentInformation::getVersion()
{
    return version;
}

std::string FragmentInformation::getSizeInBytes()
{
    return sizeInBytes;
}
}
