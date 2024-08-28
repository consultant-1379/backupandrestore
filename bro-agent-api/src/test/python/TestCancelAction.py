import pytest

from bro_agent.generated.Action_pb2 import Action
from bro_agent.registration.RegistrationInformation import RegistrationInformation
from TestAgentBehavior import TestAgentBehavior
from bro_agent.agent.AgentBehavior import AgentBehavior
from bro_agent.generated.AgentControl_pb2 import AgentControl
from bro_agent.agent.OrchestratorGrpcChannel import OrchestratorGrpcChannel
from bro_agent.agent.Agent import Agent
from bro_agent.agent.CancelActions import CancelActions
from RegistrationInformationUtil import RegistrationInformationUtil

class TestCancelActions:
    @pytest.fixture()
    def setUp(self):
        self.channel = TestOrchestratorGrpcChannel()

    def test_sendStageComplete_testAgentBehavior_stageCompleteTrue(self, setUp):
        self.agent = Agent(TestAgentBehavior(), self.channel, None, None)
        self.cancelActions = CancelActions(self.agent, "BackupName", Action.BACKUP)

        self.cancelActions.send_stage_complete(True, "Success")
        assert self.channel.get_message().stageComplete.success

        assert "BackupName" == self.cancelActions.backup_name
        assert Action.BACKUP == self.cancelActions.action

    def test_sendStageComplete_cancelAgentBehavior_stageCompleteTrue(self, setUp):
        self.agent = Agent(CancelTestAgentBehavior(), self.channel, None, None)
        self.cancelActions = CancelActions(self.agent, "Backup", Action.RESTORE)

        self.cancelActions.send_stage_complete(True, "Success")
        assert self.channel.get_message().stageComplete.success

        assert "Backup" == self.cancelActions.backup_name
        assert Action.RESTORE == self.cancelActions.action

class TestOrchestratorGrpcChannel(OrchestratorGrpcChannel):
    def __init__(self):
        OrchestratorGrpcChannel.__init__(self)
        self.messages = []

    def send_message(self, message):
        self.messages.append(message)

    def get_message(self):
        if(len(self.messages) == 0):
            return None

        message = self.messages[0]
        return message

class CancelTestAgentBehavior(AgentBehavior):

    def get_registration_information(self):
        return RegistrationInformationUtil.get_test_registration_information()

    def execute_backup(self, backupExecutionActions):
        pass

    def execute_restore(self, restoreExecutionActions):
        pass

if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
