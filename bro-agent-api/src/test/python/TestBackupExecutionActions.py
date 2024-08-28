import unittest
from unittest.mock import Mock
import pytest

from bro_agent.agent.Agent import Agent
from bro_agent.agent.BackupExecutionActions import BackupExecutionActions
from bro_agent.fragment.BackupFragmentInformation import BackupFragmentInformation

#import com.ericsson.adp.mgmt.data.BackupData

class TestBackupExecutionActions:
    @pytest.fixture()
    def setUp(self):
        # streamStub = StreamStub()
        self.agent = Mock()
        # expect(agent.getBackupStream()).andReturn(streamStub);
        self.agent.get_agent_id.return_value = "id"

        self.agent.channel = Mock()
        self.agent.channel.send_message = Mock(return_value=None)
        self.agent.channel.send_backup = Mock(return_value=None)

        action_information = Mock()
        action_information.backup_name = "myBackup"
        action_information.backup_type = "DEFAULT"

        # replay(agent, actionInformation)

        self.actions = BackupExecutionActions(self.agent, action_information)

    def test_sendBackup_fragmentInformation_sendsBackupDataToBackupStream(self, setUp):
        self.actions.send_backup(TestBackupExecutionActions.getFragmentInformation())
        assert self.agent.channel.send_backup.call_count == 7 
        # 7 because 
        # - 1 Metadata
        # - 3 for a backup file
        # - 3 for a custommetadata file

    def test_getBackupName_backupName(self, setUp):
        assert "myBackup" == self.actions.get_backup_name()

    def test_getBackupName_backupType(self, setUp):
        assert "DEFAULT" == self.actions.get_backup_type()

    @staticmethod
    def getFragmentInformation():
        fragment_information = BackupFragmentInformation(fragment_id="1", version="3", size_in_bytes="2",
                 custom_information=None, backup_path="test/resources/backup.txt", custom_metadata_path="test/resources/CustomMetadata.txt")

        # fragment_information.backup_file_path("src/test/resources/backup.txt")
        # fragment_information.set_custom_metadata_file_path("src/test/resources/CustomMetadata.txt")
        # fragment_information.setFragmentId("1")
        # fragment_information.setSizeInBytes("2")
        # fragment_information.setVersion("3")

        return fragment_information

# class StreamStub implements StreamObserver<BackupData> {

#         private final List<BackupData> messages = new ArrayList<>();

#         @Override
#         public void onCompleted() {

#         }

#         @Override
#         public void onError(final Throwable arg0) {

#         }

#         @Override
#         public void onNext(final BackupData arg0) {
#             this.messages.add(arg0);
#         }

#         public List<BackupData> getMessages() {
#             return messages;
#         }

#     }

# }

if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
