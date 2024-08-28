#pragma once

#include <vector>

#include "fragment/BackupFragmentInformation.hh"
#include "registration/SoftwareVersion.hh"

namespace BackupRestoreAgent
{

class Agent;

/**
 * provides method to download the fragments to be restored
 */
class RestoreExecutionActions
{
public:
    /**
     * provides method to download the fragments to be restored
     *
     * @param agent
     *            The agent performing the restore action
     */
    explicit RestoreExecutionActions(Agent* agent);
    /**
     * Method to download a fragment to be restored
     *
     * @param fragment
     *            is instance of FragmentData containing information needed to download a fragment
     * @param restoreLocation
     *            The location to download a fragment to.
     */
    void downloadFragment(FragmentInformation fragment, const std::string& restoreLocation);
    /**
     * Once all fragments have been restored call this method to inform the orchestrator that the restore execution has completed
     *
     * @param success
     *            Inform the orchestrator if the restore was successful or not
     * @param message
     *            Inform the orchestrator why something went wrong or just that all is well
     */
    void restoreComplete(bool success, const std::string& message);
    /**
     * Provides vector of fragments available to restore
     *
     * @return vector of partial fragment information
     */
    std::vector<FragmentInformation> getFragmentList();
    /**
     * Provides software version of the backup to be restored. This should be used for validation. Call restore complete with success set to false if
     * this version is incompatible
     *
     * @return Software Version details
     */
    SoftwareVersion getSoftwareVersion();
    /**
     * Provides the name of the backup that is being restored.
     *
     * @return backupName
     */
    std::string getBackupName();
private:
    Agent* agent;
};

}
