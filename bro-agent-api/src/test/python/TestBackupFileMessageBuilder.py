import bro_agent.generated.INT_BR_ORCH_DATA_pb2 as INT_BR_ORCH_DATA
from bro_agent.grpc.BackupFileMessageBuilder import BackupFileMessageBuilder

def test_builder_builds_correct_message_for_filename():
    backup_file_chunk = INT_BR_ORCH_DATA.BackupFileChunk(fileName="Test_file")
    expected_message = INT_BR_ORCH_DATA.BackupData(
        dataMessageType=INT_BR_ORCH_DATA.DataMessageType.BACKUP_FILE,
        backupFileChunk=backup_file_chunk
    )

    assert expected_message == BackupFileMessageBuilder().get_file_name_message("Test_file")

def test_builder_builds_correct_message_for_content():
    backup_file_chunk = INT_BR_ORCH_DATA.BackupFileChunk(content=b"Data from file")
    expected_message = INT_BR_ORCH_DATA.BackupData(
        dataMessageType=INT_BR_ORCH_DATA.DataMessageType.BACKUP_FILE,
        backupFileChunk=backup_file_chunk
    )

    assert expected_message == BackupFileMessageBuilder().get_data_message(b"Data from file")

def test_builder_builds_correct_message_for_checksum():
    backup_file_chunk = INT_BR_ORCH_DATA.BackupFileChunk(checksum="DEADBEEF0123456")
    expected_message = INT_BR_ORCH_DATA.BackupData(
        dataMessageType=INT_BR_ORCH_DATA.DataMessageType.BACKUP_FILE,
        backupFileChunk=backup_file_chunk
    )

    assert expected_message == BackupFileMessageBuilder().get_checksum_message("DEADBEEF0123456")


if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
