import bro_agent.generated.INT_BR_ORCH_DATA_pb2 as INT_BR_ORCH_DATA
from bro_agent.grpc.CustomMetadataFileMessageBuilder import CustomMetadataFileMessageBuilder

def test_builder_builds_correct_message_for_filename():
    backup_file_chunk = INT_BR_ORCH_DATA.CustomMetadataFileChunk(fileName="Test_file")
    expected_message = INT_BR_ORCH_DATA.BackupData(
        dataMessageType=INT_BR_ORCH_DATA.DataMessageType.CUSTOM_METADATA_FILE,
        customMetadataFileChunk=backup_file_chunk
    )

    assert expected_message == CustomMetadataFileMessageBuilder().get_file_name_message("Test_file")

def test_builder_builds_correct_message_for_content():
    backup_file_chunk = INT_BR_ORCH_DATA.CustomMetadataFileChunk(content=b"Data from file")
    expected_message = INT_BR_ORCH_DATA.BackupData(
        dataMessageType=INT_BR_ORCH_DATA.DataMessageType.CUSTOM_METADATA_FILE,
        customMetadataFileChunk=backup_file_chunk
    )

    assert expected_message == CustomMetadataFileMessageBuilder().get_data_message(b"Data from file")

def test_builder_builds_correct_message_for_checksum():
    backup_file_chunk = INT_BR_ORCH_DATA.CustomMetadataFileChunk(checksum="DEADBEEF0123456")
    expected_message = INT_BR_ORCH_DATA.BackupData(
        dataMessageType=INT_BR_ORCH_DATA.DataMessageType.CUSTOM_METADATA_FILE,
        customMetadataFileChunk=backup_file_chunk
    )

    assert expected_message == CustomMetadataFileMessageBuilder().get_checksum_message("DEADBEEF0123456")


if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
