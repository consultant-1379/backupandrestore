import bro_agent.generated.INT_BR_ORCH_DATA_pb2 as INT_BR_ORCH_DATA
from bro_agent.grpc.BackupMessageBuilder import BackupMessageBuilder


class BackupFileMessageBuilder(BackupMessageBuilder):
    """ To build the backup file message"""

    @staticmethod
    def get_file_name_message(file_name):
        """ get the file name """
        backup_file_chunk = INT_BR_ORCH_DATA.BackupFileChunk(fileName=file_name)
        backup_data = INT_BR_ORCH_DATA.BackupData(dataMessageType=INT_BR_ORCH_DATA.DataMessageType.BACKUP_FILE,
                                                  backupFileChunk=backup_file_chunk)
        return backup_data

    @staticmethod
    def get_data_message(data):
        """ get data message """
        backup_file_chunk = INT_BR_ORCH_DATA.BackupFileChunk(content=data)
        backup_data = INT_BR_ORCH_DATA.BackupData(dataMessageType=INT_BR_ORCH_DATA.DataMessageType.BACKUP_FILE,
                                                  backupFileChunk=backup_file_chunk)
        return backup_data

    @staticmethod
    def get_checksum_message(checksum):
        """"get checksum message """
        backup_file_chunk = INT_BR_ORCH_DATA.BackupFileChunk(checksum=checksum)
        backup_data = INT_BR_ORCH_DATA.BackupData(dataMessageType=INT_BR_ORCH_DATA.DataMessageType.BACKUP_FILE,
                                                  backupFileChunk=backup_file_chunk)
        return backup_data
