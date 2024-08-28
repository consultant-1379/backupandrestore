import os
import logging

from bro_agent.exception.Exceptions import FailedToTransferBackupException
from bro_agent.filetransfer.FileChunkServiceUtil import FileChunkServiceUtil
import bro_agent.generated.Fragment_pb2 as Fragment
import bro_agent.generated.INT_BR_ORCH_DATA_pb2 as INT_BR_ORCH_DATA
from bro_agent.grpc.BackupFileMessageBuilder import BackupFileMessageBuilder
from bro_agent.grpc.CustomMetadataFileMessageBuilder import CustomMetadataFileMessageBuilder
from bro_agent.util.ChecksumCalculator import ChecksumCalculator


class BackupService:
    """ Backup Service """
    def __init__(self, channel):
        self.channel = channel

    def backup(self, fragment_information, agent_id, backup_name):
        """ Do the backup """
        try:
            self.channel.open_data_stream()
            self.send_metadata(agent_id, fragment_information, backup_name)
            self.send_file(fragment_information.backup_path, BackupService.get_backup_file_message_builder())
            custom = fragment_information.custom_metadata_path
            if (custom is not None):
                self.send_file(custom, BackupService.get_custom_metadata_file_message_builder())
            self.end_request()
        except Exception as e:
            logging.error("Error sending file for: {}".format(backup_name))
            raise FailedToTransferBackupException("There was an error while trying to transfer: {}".format(fragment_information.backup_path), e)

    def send_metadata(self, agent_id, fragment_information, backup_name):
        """ Send the metadata """
        fragment = Fragment.Fragment(fragmentId=str(fragment_information.fragment_id).encode('utf-8'),
                                     sizeInBytes=str(fragment_information.size_in_bytes).encode('utf-8'),
                                     version=str(fragment_information.version).encode('utf-8'))
        metadata = INT_BR_ORCH_DATA.Metadata(agentId=agent_id,
                                             fragment=fragment,
                                             backupName=backup_name)
        message = INT_BR_ORCH_DATA.BackupData(dataMessageType=INT_BR_ORCH_DATA.DataMessageType.METADATA,
                                              metadata=metadata)
        logging.info("Sending backup metadata {}".format(metadata))
        self.channel.send_backup(message)

    def send_file(self, path, backup_message_builder):
        """ Send the file """
        logging.info("Transferring file <{}>".format(path))
        calculator = ChecksumCalculator()

        logging.debug("Sending message with file name")
        self.channel.send_backup(backup_message_builder.get_file_name_message(os.path.basename(path)))

        logging.debug("Sending message(s) with file data")
        for chunk, bytes_read_in_chunk in FileChunkServiceUtil().process_file_chunks(path):
            self.channel.send_backup(backup_message_builder.get_data_message(chunk))
            calculator.add_bytes(chunk)

        logging.debug("Sending message with checksum")
        self.channel.send_backup(backup_message_builder.get_checksum_message(calculator.get_checksum().upper()))

        logging.info("Finished transferring file <{}>".format(path))

    def end_request(self):
        """ End of request """
        self.channel.close_data_stream()

    @staticmethod
    def get_backup_file_message_builder():
        return BackupFileMessageBuilder()

    @staticmethod
    def get_custom_metadata_file_message_builder():
        return CustomMetadataFileMessageBuilder()
