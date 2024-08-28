from bro_agent.fragment.FragmentInformation import FragmentInformation


class BackupFragmentInformation(FragmentInformation):
    """ Backup Fragment Information """
    def __init__(self, fragment_id, version, size_in_bytes,
                 custom_information, backup_path, custom_metadata_path):
        super().__init__(fragment_id, version, size_in_bytes, custom_information)
        self.backup_path = backup_path
        self.custom_metadata_path = custom_metadata_path
