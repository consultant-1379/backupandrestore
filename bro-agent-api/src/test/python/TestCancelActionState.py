import pytest
import bro_agent.agent.states

from bro_agent.agent.Agent import Agent
from bro_agent.agent.OrchestratorGrpcChannel import OrchestratorGrpcChannel
from bro_agent.generated.Action_pb2 import Action
from bro_agent.generated.CancelBackupRestore_pb2 import CancelBackupRestore
from TestAgentBehavior import TestAgentBehavior
from bro_agent.generated.AgentControl_pb2 import AgentControl
from bro_agent.generated.OrchestratorControl_pb2 import OrchestratorControl
from bro_agent.generated.OrchestratorControl_pb2 import OrchestratorMessageType

class TestCancelActionState:

    @pytest.fixture()
    def setUp(self):
        self.channel = TestOrchestratorGrpcChannel()
        self.agentBehavior = TestAgentBehavior()
        self.agent = Agent(self.agentBehavior, self.channel, None, None)
        self.state = bro_agent.agent.CancelActionState.CancelActionState(self.agent, "Backup", Action.RESTORE)

    def test_processMessage_cancelMessage_stateChangeToWaitingForActions(self, setUp):
        stateChange = self.state.process_message(TestCancelActionState.getCancelMessage())
        assert isinstance(stateChange, bro_agent.agent.WaitingForActionState.WaitingForActionState)

    def test_trigger_agent_stateChangeToWaitingForActions(self, setUp):
        stateChange = self.state.process_message(TestCancelActionState.getCancelMessage())
        assert isinstance(stateChange, bro_agent.agent.WaitingForActionState.WaitingForActionState)

        stateChange.trigger()
        assert self.channel.get_message() == None

    def test_finishAction_returnsWaitingForActionState(self, setUp):
        stateChange  = self.state.finish_action()
        assert isinstance(stateChange, bro_agent.agent.WaitingForActionState.WaitingForActionState)

    @staticmethod
    def getCancelMessage():
        orchestratorControlMessage = OrchestratorControl()
        orchestratorControlMessage.action = Action.RESTORE
        orchestratorControlMessage.orchestratorMessageType = OrchestratorMessageType.CANCEL_BACKUP_RESTORE
        orchestratorControlMessage.cancel.CopyFrom(CancelBackupRestore())
        return orchestratorControlMessage


class TestOrchestratorGrpcChannel(OrchestratorGrpcChannel):
    def __init__(self):
        OrchestratorGrpcChannel.__init__(self)
        self.messages = []

    def send_control_message(self, message):
        self.messages.append(message)

    def get_message(self):
        if(len(self.messages) == 0):
            return None

        message = self.messages.remove(0)
        return message

if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
