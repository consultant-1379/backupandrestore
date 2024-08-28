from unittest.mock import patch
from unittest.mock import Mock

import bro_agent.agent.states
from bro_agent.agent.RestoreActions import RestoreActions
from bro_agent.generated.Action_pb2 import Action
from bro_agent.generated.Fragment_pb2 import Fragment

def test_restore_actions_download_fragment():
    agent_mock = Mock()
    mock_download = Mock()
    agent_mock.get_agent_id.return_value = "TestId"
    agent_mock.get_backup_name.return_value = "TestBackup"
    restore_actions = RestoreActions(agent_mock, Mock())
    fragment = get_fragment()

    with patch("bro_agent.service.RestoreService.RestoreService.download", mock_download):
        restore_actions.download_fragment(fragment, "/tmp/restore")

    mock_download.assert_called()


def get_fragment():
    return Fragment(
        fragmentId = "1",
        sizeInBytes = "5",
        version = "0.0",
        customInformation = {"cus": "tom"}
    )

if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
