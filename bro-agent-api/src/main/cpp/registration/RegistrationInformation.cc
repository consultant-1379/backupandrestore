#include "RegistrationInformation.hh"

namespace BackupRestoreAgent
{

RegistrationInformation::RegistrationInformation(const std::string& agentId, const std::string& scope, const SoftwareVersion& softwareVersion)
    : agentId(agentId)
    , scope(scope)
    , softwareVersion(softwareVersion)
{
}

std::string RegistrationInformation::getAgentId()
{
    return agentId;
}

std::string RegistrationInformation::getScope()
{
    return scope;
}

SoftwareVersion RegistrationInformation::getSoftwareVersion()
{
    return softwareVersion;
}
}
