#pragma once

#include <string>

#include "SoftwareVersion.hh"

namespace BackupRestoreAgent
{

/**
 * Holds agent's registration information.
 */
class RegistrationInformation
{
public:
    /**
     * Provides a constructor that can be used to create a registration information object. This
     * information is sent to the orchestrator during registration.
     *
     * @param agentId The id of the agent
     * @param scope The scope this agent will be a part of. Scope here refers to the backup type in
     * the Ericsson BRM.
     * @param softwareVersion The software version information to register with
     */
    RegistrationInformation(const std::string& agentId, const std::string& scope, const SoftwareVersion& softwareVersion);

    std::string getAgentId();
    std::string getScope();
    SoftwareVersion getSoftwareVersion();

private:
    std::string agentId;
    std::string scope;
    SoftwareVersion softwareVersion;
};

}
