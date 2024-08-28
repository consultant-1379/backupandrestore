#pragma once

#include "registration/RegistrationInformation.hh"

namespace BackupRestoreAgent
{

class BackupExecutionActions;
class RestoreExecutionActions;

/**
 * Represents specific agent behavior.
 */
class AgentBehavior
{
public:
    virtual ~AgentBehavior() {};
    /**
     * Gets agent's registration information.
     *
     * @return registration information.
     */
    virtual RegistrationInformation getRegistrationInformation() = 0;
    /**
     * Used to call an agents custom code for a backup from the orchestrator
     *
     * @param backupExecutionActions
     *            provides methods to allow an agent to transfer backups and indicate that a backup has completed.
     *
     */
    virtual void executeBackup(BackupExecutionActions* backupExecutionActions) = 0;
    /**
     * Used to call an agents custom code for a restore from the orchestrator
     *
     * @param restoreExecutionActions
     *            provides methods to download fragments to be restored by an agent and indicate that a restore has completed.
     *
     */
    virtual void executeRestore(RestoreExecutionActions* restoreExecutionActions) = 0;
};

}
