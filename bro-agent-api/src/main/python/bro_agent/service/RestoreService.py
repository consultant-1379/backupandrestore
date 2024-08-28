import logging

import bro_agent.generated.INT_BR_ORCH_DATA_pb2 as INT_BR_ORCH_DATA
from bro_agent.util.ChecksumCalculator import ChecksumCalculator


class RestoreService:
    def __init__(self, restore_location):
        self.calculator = ChecksumCalculator()
        self.restore_location = restore_location

    def download(self, restore_data_iterator):
        for data in restore_data_iterator:
            if data.dataMessageType is INT_BR_ORCH_DATA.DataMessageType.BACKUP_FILE:
                self.write_backup_file(data.backupFileChunk)
            if data.dataMessageType is INT_BR_ORCH_DATA.DataMessageType.CUSTOM_METADATA_FILE:
                self.write_custom_metadata_file(data.customMetadataFileChunk)
        logging.info("Finished downloading data!")

    def write_backup_file(self, chunk):
        if chunk.fileName != "":
            self.fileName = chunk.fileName
            self.fileDescriptor = open(self.restore_location + '/' + self.fileName, "wb")
        elif chunk.content != b"":
            self.fileDescriptor.write(chunk.content)
            self.calculator.add_bytes(chunk.content)
        elif chunk.checksum != b"":
            self.fileDescriptor.close()
            if chunk.checksum == self.calculator.get_checksum().upper():
                logging.info("Checksum matched!")
            else:
                logging.error("Checksum mismatch!")

    def write_custom_metadata_file(self, chunk):
        self.write_backup_file(chunk)
