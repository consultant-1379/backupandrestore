from unittest.mock import Mock
from unittest.mock import patch
from unittest.mock import mock_open

from bro_agent.fragment.BackupFragmentInformation import BackupFragmentInformation
from bro_agent.service.BackupService import BackupService

SENT_MESSAGE_COUNT = 4
SENT_MESSAGE_COUNT_WITH_CUSTOMMETADATA = 7

@patch("builtins.open", mock_open(read_data=b"Data1"))
def test_backup_without_custommetadata():
    channel_mock = Mock()
    backup_service = BackupService(channel_mock)
    fragment = get_fragment_information()

    backup_service.backup(fragment, "TestAgent", "Test_backup")

    channel_mock.open_data_stream.assert_called_once()
    assert SENT_MESSAGE_COUNT == channel_mock.send_backup.call_count
    channel_mock.close_data_stream.assert_called_once()


@patch("builtins.open", mock_open(read_data=b"Data1"))
def test_backup_with_custommetadata():
    channel_mock = Mock()
    backup_service = BackupService(channel_mock)
    fragment = get_fragment_information_with_custommetadata()

    backup_service.backup(fragment, "TestAgent", "Test_backup")

    channel_mock.open_data_stream.assert_called_once()
    assert SENT_MESSAGE_COUNT_WITH_CUSTOMMETADATA == channel_mock.send_backup.call_count
    channel_mock.close_data_stream.assert_called_once()


def get_fragment_information():
    fragment_information = BackupFragmentInformation("1", "0.1", "5", None, "/tmp/file", None)
    return fragment_information


def get_fragment_information_with_custommetadata():
    fragment_information = BackupFragmentInformation("1", "0.1", "5", "", "/tmp/file", "/tmp/custom_file")
    return fragment_information

if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
