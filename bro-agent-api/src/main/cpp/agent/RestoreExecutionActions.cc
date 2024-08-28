#include "service/RestoreService.hh"

#include "Agent.hh"
#include "RestoreExecutionActions.hh"
#include "util/FileStream.hh"

namespace BackupRestoreAgent
{

RestoreExecutionActions::RestoreExecutionActions(Agent* agent)
    : agent(agent)
{
}

void RestoreExecutionActions::downloadFragment(FragmentInformation fragment, const std::string& restoreLocation)
{
    grpc::ClientContext context;
    RestoreService restoreService(agent->getRestoreStream(fragment, &context), new FileStream());
    restoreService.download(restoreLocation);
}

void RestoreExecutionActions::restoreComplete(bool success, const std::string& message)
{
    agent->sendStageCompleteMessage(success, message, Action::RESTORE);
    agent->cleanUpAfterRestore();
}

std::vector<FragmentInformation> RestoreExecutionActions::getFragmentList()
{
    std::vector<FragmentInformation> result;
    google::protobuf::RepeatedPtrField<Fragment> fragments = agent->getFragmentList();
    std::transform(fragments.begin(),
        fragments.end(),
        std::back_inserter(result),
        [](const Fragment& fragment) {
            return FragmentInformation(fragment.fragmentid(), fragment.version(), fragment.sizeinbytes());
        });
    return result;
}

SoftwareVersion RestoreExecutionActions::getSoftwareVersion()
{
    SoftwareVersionInfo versionInfo = agent->getSoftwareVersionInfo();
    return SoftwareVersion(versionInfo.productname(), versionInfo.productnumber(), versionInfo.revision(), versionInfo.productiondate(), versionInfo.description(), versionInfo.type());
}

std::string RestoreExecutionActions::getBackupName()
{
    return agent->getBackupName();
}
}
