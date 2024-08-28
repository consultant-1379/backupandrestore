import pytest

import bro_agent.agent.states

from bro_agent.generated.Action_pb2 import Action
from bro_agent.generated.CancelBackupRestore_pb2 import CancelBackupRestore
from TestAgentBehavior import TestAgentBehavior
from bro_agent.generated.AgentControl_pb2 import AgentControl
from bro_agent.generated.OrchestratorControl_pb2 import OrchestratorControl
from bro_agent.generated.OrchestratorControl_pb2 import OrchestratorMessageType
from bro_agent.generated.OrchestratorControl_pb2 import PostActions
from bro_agent.generated.OrchestratorControl_pb2 import Preparation
from bro_agent.generated.Fragment_pb2 import Fragment
from bro_agent.generated.SoftwareVersionInfo_pb2 import SoftwareVersionInfo
from bro_agent.agent.Agent import Agent
from bro_agent.agent.OrchestratorGrpcChannel import OrchestratorGrpcChannel

class TestFinishedActionState:
    @pytest.fixture()
    def setUp(self):
        self.channel = TestOrchestratorGrpcChannel()
        self.agentBehavior = TestAgentBehavior()
        self.agent = Agent(self.agentBehavior, self.channel, None, None)
        self.state = bro_agent.agent.FinishedActionState.FinishedActionState(self.agent, "Backup", Action.RESTORE)

    def test_processMessage_cancelMessage_stateChangeToWaitingForActions(self, setUp):
        stateChange = self.state.process_message(TestFinishedActionState.getCancelMessage())
        assert isinstance(stateChange, bro_agent.agent.CancelActionState.CancelActionState)

    def test_processMessage_postActionMessage_stateChangeToWaitingForActions(self, setUp):
        stateChange = self.state.process_message(TestFinishedActionState.getPostActionMessage())
        assert isinstance(stateChange, bro_agent.agent.WaitingForActionState.WaitingForActionState)

        stateChange.trigger()
        assert self.channel.get_message() == None

    def test_processMessage_preparationMessage_stateChangeToRestorePreparation(self, setUp):
        stateChange = self.state.process_message(TestFinishedActionState.getPreparationMessage())
        assert isinstance(stateChange, bro_agent.agent.RestorePreparationState.RestorePreparationState)

    def test_trigger_preparationMessage_sendStageComplete(self, setUp):
        stateChange = self.state.process_message(TestFinishedActionState.getPreparationMessage())
        assert isinstance(stateChange, bro_agent.agent.RestorePreparationState.RestorePreparationState)

        stateChange.trigger()
        assert self.channel.get_message().stageComplete.success

    def test_trigger_cancelState_stateChangeToWaitingForActions(self, setUp):
        stateChange = self.state.process_message(TestFinishedActionState.getCancelMessage())
        assert isinstance(stateChange, bro_agent.agent.CancelActionState.CancelActionState)

        stateChange.trigger()
        assert self.channel.get_message().stageComplete.success

    def test_finishAction_returnsFinishedActionState(self, setUp):
        stateChange = self.state.finish_action()
        assert isinstance(stateChange, bro_agent.agent.FinishedActionState.FinishedActionState)

    @staticmethod
    def getCancelMessage():
        orchestratorControlMessage = OrchestratorControl()
        orchestratorControlMessage.action = Action.RESTORE
        orchestratorControlMessage.orchestratorMessageType = OrchestratorMessageType.CANCEL_BACKUP_RESTORE
        return orchestratorControlMessage

    @staticmethod
    def getPostActionMessage():
        orchestratorControlMessage = OrchestratorControl()
        orchestratorControlMessage.action = Action.RESTORE
        orchestratorControlMessage.orchestratorMessageType = OrchestratorMessageType.POST_ACTIONS
        return orchestratorControlMessage

    @staticmethod
    def getPreparationMessage():
        orchestratorControlMessage = OrchestratorControl()
        orchestratorControlMessage.action = Action.RESTORE
        orchestratorControlMessage.orchestratorMessageType = OrchestratorMessageType.PREPARATION

        orchestratorControlMessage.preparation.backupName = "Backup"
        TestFinishedActionState.fillInSoftwareVersionInfo(orchestratorControlMessage.preparation.softwareVersionInfo)
        orchestratorControlMessage.preparation.fragment.append(TestFinishedActionState.getFragment())

        return orchestratorControlMessage

    @staticmethod
    def fillInSoftwareVersionInfo(softwareVersionInfo):
        softwareVersionInfo.productName = "Name"
        softwareVersionInfo.description = "Description"
        softwareVersionInfo.productNumber = "Number"
        softwareVersionInfo.productionDate = "Date"
        softwareVersionInfo.revision = "Revision"
        softwareVersionInfo.type = "Type"

    @staticmethod
    def getFragment():
        fragment = Fragment()
        fragment.fragmentId = "id"
        fragment.sizeInBytes = "size"
        fragment.version = "version"

        return fragment

class TestOrchestratorGrpcChannel(OrchestratorGrpcChannel):

    def __init__(self):
        OrchestratorGrpcChannel.__init__(self, None)
        self.messages = []

    def send_message(self, message):
        self.messages.append(message)

    def get_message(self):
        if(len(self.messages) == 0):
            return None

        message = self.messages[0]
        return message

if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
