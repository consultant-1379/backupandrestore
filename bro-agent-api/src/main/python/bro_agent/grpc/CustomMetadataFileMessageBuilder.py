from bro_agent.grpc.BackupMessageBuilder import BackupMessageBuilder
import bro_agent.generated.INT_BR_ORCH_DATA_pb2 as INT_BR_ORCH_DATA


class CustomMetadataFileMessageBuilder(BackupMessageBuilder):
    """
    * Builds messages to send custom metadata file through Data Channel.
    """
    @staticmethod
    def get_file_name_message(file_name):
        """ get the file name """
        backup_file_chunk = INT_BR_ORCH_DATA.CustomMetadataFileChunk(fileName=file_name)
        backup_data = INT_BR_ORCH_DATA.BackupData(dataMessageType=INT_BR_ORCH_DATA.DataMessageType.CUSTOM_METADATA_FILE,
                                                  customMetadataFileChunk=backup_file_chunk)
        return backup_data

    @staticmethod
    def get_data_message(data):
        """ get data message """
        backup_file_chunk = INT_BR_ORCH_DATA.CustomMetadataFileChunk(content=data)
        backup_data = INT_BR_ORCH_DATA.BackupData(dataMessageType=INT_BR_ORCH_DATA.DataMessageType.CUSTOM_METADATA_FILE,
                                                  customMetadataFileChunk=backup_file_chunk)
        return backup_data

    @staticmethod
    def get_checksum_message(checksum):
        """"get checksum message """
        backup_file_chunk = INT_BR_ORCH_DATA.CustomMetadataFileChunk(checksum=checksum)
        backup_data = INT_BR_ORCH_DATA.BackupData(dataMessageType=INT_BR_ORCH_DATA.DataMessageType.CUSTOM_METADATA_FILE,
                                                  customMetadataFileChunk=backup_file_chunk)
        return backup_data
