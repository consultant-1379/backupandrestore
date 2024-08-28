#pragma once

#include <string>

#include "fragment/BackupFragmentInformation.hh"

namespace BackupRestoreAgent
{

class Agent;

/**
 *
 * Provides functions for an agent to send metadata, the backup and custom metadata.
 *
 * An agent implementer should always send metadata followed by the backup.
 *
 * It is optional to send custom metadata.
 *
 */
class BackupExecutionActions
{
public:
    /**
     * provides the actions that an agent is allowed to perform during the backup execution.
     *
     * @param agent
     *            The agent performing the backup actions
     */
    explicit BackupExecutionActions(Agent* agent);

    /**
     * To be used to send backup data to the orchestrator
     *
     * @param fragmentInformation
     *            The information about a fragment.
     */
    void sendBackup(BackupFragmentInformation fragmentInformation);
    /**
     * Once all backup fragments have been sent call this method to inform the orchestrator that the backup execution has completed
     *
     * @param success
     *            Inform the orchestrator if the backup was successful or not
     * @param message
     *            Inform the orchestrator why something went wrong or just that all is well
     */
    void backupComplete(bool success, const std::string& message);
    /**
     * Provides the name that was given with a backup request.
    *
     * @return backupName
     */
    std::string getBackupName();

private:
    Agent* agent;
};

}
