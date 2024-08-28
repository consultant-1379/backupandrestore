from abc import ABC


class ActionInformation(ABC):
    def __init__(self, backup_name, backup_type):
        self.backup_name = backup_name
        self.backup_type = backup_type

    @classmethod
    def from_preparation(cls, preparation):
        backup_name = preparation.backupName
        backup_type = preparation.backupType
        return cls(backup_name, backup_type)
