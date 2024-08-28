from abc import ABC
import logging

from bro_agent.fragment.FragmentInformation import FragmentInformation
import bro_agent.generated.Action_pb2 as Action
import bro_agent.generated.INT_BR_ORCH_DATA_pb2 as INT_BR_ORCH_DATA
from bro_agent.registration.SoftwareVersion import SoftwareVersion
from bro_agent.service.RestoreService import RestoreService


class RestoreActions(ABC):
    """ Provides means to perform various restore actions
        Provides a function to indicate that a restore stage is complete
        Provides methods to access restore information """

    def __init__(self, agent, restore_information):
        # @param agent Agent participating in restore
        # @param restoreInformation required for restore
        self.agent = agent
        self.restore_information = restore_information

    def send_stage_complete(self, success, message):
        """ Once all the actions in the stage is completed
            call this method to inform the Orchestrator that the stage has completed """
        # @param success Inform the Orchestrator if the stage was successful or not
        # @param message Inform the Orchestrator why something went wrong or just that all is well
        self.agent.send_stage_complete_message(success, message, Action.RESTORE)

    def download_fragment(self, fragment, restore_location):
        """ Method to download a fragment to be restored """
        # * @param fragment is instance of FragmentData
        #           containing information needed to download a fragment
        # * @param restoreLocation The location to download a fragment to.
        # * @throws FailedToDownloadException when there occurs an issue while downloading fragment
        restore_service = RestoreService(restore_location)
        restore_data_iterator = self.agent.get_restore_data_iterator(self.metadata_builder(fragment))
        try:
            restore_service.download(restore_data_iterator)
        except Exception as exception:
            logging.error("An exception occured when trying to download fragment %s of backup %s",
                          fragment.fragmentId,
                          self.agent.get_backup_name())
            raise exception

    def get_backup_name(self):
        """ Provides the name of the backup that is being restored. """
        # @return backupName
        return self.restore_information.getBackupName()

    def get_backup_type(self):
        """ Provides the backup Type of the backup that is being restored. """
        # @return backupType
        return self.restore_information.getBackupType()

    def get_fragment_list(self):
        """ Provides list of fragments available to restore """
        # @return list of partial fragment information
        fragment_informations = []
        for fragment in self.restore_information.get_fragment_list().stream():
            fragment_informations.append(self.set_fragment(fragment))
        return fragment_informations

    def get_software_version(self):
        """ Provides software version of the backup to be restored.
            This should be used for validation.
            Call restore complete with success set to false if
            this version is incompatible"""
        # @return Software Version details
        software_version_info = self.restore_information.getSoftwareVersionInfo()

        software_version = SoftwareVersion()

        software_version.setProductName(software_version_info.get_product_name())
        software_version.setProductNumber(software_version_info.get_product_number())
        software_version.setRevision(software_version_info.get_revision())
        software_version.setDescription(software_version_info.get_description())
        software_version.setType(software_version_info.get_type())
        software_version.setProductionDate(software_version_info.get_production_date())

        return software_version

    def metadata_builder(self, fragment):
        metadata = INT_BR_ORCH_DATA.Metadata()
        metadata.agentId = self.agent.get_agent_id()
        metadata.backupName = self.agent.get_backup_name()

        metadata.fragment.fragmentId = fragment.fragmentId
        metadata.fragment.sizeInBytes = fragment.sizeInBytes
        metadata.fragment.version = fragment.version
        metadata.fragment.customInformation.update(fragment.customInformation)

        return metadata

    def set_fragment(self, fragment):
        """ Converting Fragment obtained from Orchestrator into
            PartialFragmentInformation to hide grpc details """
        # @param fragment obtained from Orchestrator
        # @return PartialFragmentInformation instance containing fragment details obtained from orchestrator
        fragment_information = FragmentInformation()
        fragment_information.setFragmentId(fragment.getFragmentId())
        fragment_information.setSizeInBytes(fragment.getSizeInBytes())
        fragment_information.setVersion(fragment.getVersion())
        fragment_information.setCustomInformation(fragment.getCustomInformationMap())
        return fragment_information
