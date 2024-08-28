from unittest.mock import Mock
from unittest.mock import patch
from unittest.mock import mock_open

from bro_agent.generated import INT_BR_ORCH_DATA_pb2 as INT_BR_ORCH_DATA
from bro_agent.service.RestoreService import RestoreService

def test_restore_without_custommetadata():
    restore_service = RestoreService("/tmp/restore")
    mock = mock_open()

    with patch("builtins.open", mock):
        restore_service.download(generate_messages())

    mock().write.assert_called()
    mock().close.assert_called()


def test_restore_with_custommetadata():
    restore_service = RestoreService("/tmp/restore")
    mock = mock_open()

    with patch("builtins.open", mock):
        restore_service.download(generate_messages_custom_metadata())

    mock().write.assert_called()
    mock().close.assert_called()

INT_BR_ORCH_DATA.DataMessageType.CUSTOM_METADATA_FILE
def generate_messages():
    file_chunk = INT_BR_ORCH_DATA.BackupFileChunk(
        fileName = "test.txt",
        content = b"",
        checksum = ""
    )
    data = INT_BR_ORCH_DATA.RestoreData(
        dataMessageType=INT_BR_ORCH_DATA.DataMessageType.BACKUP_FILE,
        backupFileChunk=file_chunk
    )
    yield data
    file_chunk = INT_BR_ORCH_DATA.BackupFileChunk(
        fileName = "",
        content = b"Data1",
        checksum = ""
    )
    data = INT_BR_ORCH_DATA.RestoreData(
        dataMessageType=INT_BR_ORCH_DATA.DataMessageType.BACKUP_FILE,
        backupFileChunk=file_chunk
    )
    yield data
    file_chunk = INT_BR_ORCH_DATA.BackupFileChunk(
        fileName = "",
        content = b"",
        checksum = "C5F89EB8A2EA0A459F1B342A19A8F0A3"
    )
    data = INT_BR_ORCH_DATA.RestoreData(
        dataMessageType=INT_BR_ORCH_DATA.DataMessageType.BACKUP_FILE,
        backupFileChunk=file_chunk
    )
    yield data


def generate_messages_custom_metadata():
    file_chunk = INT_BR_ORCH_DATA.CustomMetadataFileChunk(
        fileName = "test.txt",
        content = b"",
        checksum = ""
    )
    data = INT_BR_ORCH_DATA.RestoreData(
        dataMessageType=INT_BR_ORCH_DATA.DataMessageType.CUSTOM_METADATA_FILE,
        customMetadataFileChunk=file_chunk
    )
    yield data
    file_chunk = INT_BR_ORCH_DATA.CustomMetadataFileChunk(
        fileName = "",
        content = b"Data1",
        checksum = ""
    )
    data = INT_BR_ORCH_DATA.RestoreData(
        dataMessageType=INT_BR_ORCH_DATA.DataMessageType.CUSTOM_METADATA_FILE,
        customMetadataFileChunk=file_chunk
    )
    yield data
    file_chunk = INT_BR_ORCH_DATA.CustomMetadataFileChunk(
        fileName = "",
        content = b"",
        checksum = "C5F89EB8A2EA0A459F1B342A19A8F0A3"
    )
    data = INT_BR_ORCH_DATA.RestoreData(
        dataMessageType=INT_BR_ORCH_DATA.DataMessageType.CUSTOM_METADATA_FILE,
        customMetadataFileChunk=file_chunk
    )
    yield data


if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
