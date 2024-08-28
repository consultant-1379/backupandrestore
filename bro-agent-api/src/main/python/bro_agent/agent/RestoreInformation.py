from bro_agent.agent.ActionInformation import ActionInformation


class RestoreInformation(ActionInformation):
    def __init__(self, backup_name, backup_type, software_version_info, fragment):
        ActionInformation.__init__(self, backup_name, backup_type)
        self.software_version_info = software_version_info
        self.fragment = fragment

    @classmethod
    def from_preparation(cls, preparation):
        # super(RestoreInformation, cls).from_preparation(preparation)
        backup_name = preparation.backupName
        backup_type = preparation.backupType
        software_version_info = preparation.softwareVersionInfo
        fragment = preparation.fragment
        return cls(backup_name, backup_type, software_version_info, fragment)
