import pytest

from bro_agent.agent.Agent import Agent
from bro_agent.agent.AgentBehavior import AgentBehavior
from bro_agent.agent.BackupExecutionActions import BackupExecutionActions
from bro_agent.agent.CancelActions import CancelActions
from bro_agent.agent.PostRestoreActions import PostRestoreActions
from bro_agent.agent.RestoreExecutionActions import RestoreExecutionActions
from bro_agent.agent.RestorePreparationActions import RestorePreparationActions
from bro_agent.registration.RegistrationInformation import RegistrationInformation
from bro_agent.agent.OrchestratorGrpcChannel import OrchestratorGrpcChannel

from bro_agent.generated.Action_pb2 import Action
from bro_agent.generated.AgentControl_pb2 import AgentMessageType
from bro_agent.generated.OrchestratorControl_pb2 import Preparation
from bro_agent.generated.OrchestratorControl_pb2 import OrchestratorControl
from bro_agent.generated.OrchestratorControl_pb2 import OrchestratorMessageType
from bro_agent.generated.SoftwareVersionInfo_pb2 import SoftwareVersionInfo
from bro_agent.generated.Fragment_pb2 import Fragment
from bro_agent.generated.OrchestratorControl_pb2 import Execution
from bro_agent.generated.OrchestratorControl_pb2 import PostActions

from RegistrationInformationUtil import RegistrationInformationUtil

class TestAgent:
    @pytest.fixture()
    def setUp(self):
        self.channel = TestOrchestratorGrpcChannel()
        self.agentBehavior = TestAgentBehavior()
        self.agent = Agent(self.agentBehavior, self.channel, None, None)

    def test_register(self, setUp):
        # test_register_agentWithBehaviorAndChannel_createsNewControlStreamAndSendsRegistrationInformationOnIt
        self.agent.register()

        assert self.channel.created_new_stream == True
        assert Action.REGISTER == self.channel.get_message().action
        assert AgentMessageType.REGISTER == self.channel.get_message().agentMessageType

    def test_shutdown_agent_shutsDownChannel(self, setUp):
        self.agent.shutdown()
        assert self.channel.is_shutdown() == True

    def test_process_backupMessage_executesBackup(self, setUp):

        orchestratorControlMessage = OrchestratorControl()
        orchestratorControlMessage.action = Action.BACKUP
        orchestratorControlMessage.orchestratorMessageType = OrchestratorMessageType.PREPARATION
        orchestratorControlMessage.preparation.backupName = "Backup"
        orchestratorControlMessage.preparation.backupType = "DEFAULT"

        self.agent.process(orchestratorControlMessage)

        assert self.agentBehavior.executed_backup()

        agentControlMessageExecutionComplete = self.channel.get_message()

        assert Action.BACKUP == agentControlMessageExecutionComplete.action
        assert AgentMessageType.STAGE_COMPLETE == agentControlMessageExecutionComplete.agentMessageType
        assert "Test backup" == agentControlMessageExecutionComplete.stageComplete.message
        assert agentControlMessageExecutionComplete.stageComplete.success

    def test_process_restoreMessage_stateChange(self, setUp):
        self.agent.process(TestAgent.getPreparationMessage())

        assert self.agentBehavior.prepared_for_restore()

        orchestratorControl1 = OrchestratorControl()
        orchestratorControl1.orchestratorMessageType = OrchestratorMessageType.EXECUTION
        orchestratorControl1.action = Action.RESTORE
        self.agent.process(orchestratorControl1)

        assert self.agentBehavior.executed_restore()

        orchestratorControl2 = OrchestratorControl()
        orchestratorControl2.orchestratorMessageType = OrchestratorMessageType.POST_ACTIONS
        orchestratorControl2.action = Action.RESTORE
        self.agent.process(orchestratorControl2)

        assert self.agentBehavior.performed_post_restore()

    def test_process_messageDifferentThanBackupAndRestores_doesNothing(self, setUp):
        orchestratorControlMessage = OrchestratorControl()
        orchestratorControlMessage.action = Action.CANCEL
        orchestratorControlMessage.orchestratorMessageType = OrchestratorMessageType.CANCEL_BACKUP_RESTORE

        self.agent.process(orchestratorControlMessage)

    def test_executeBackup_agentBehaviorThrowsException_catchesAndSendsStageCompleteFalse(self, setUp):
        agent = Agent(ExceptionAgentBehavior(), self.channel, None, None)

        agent.execute_backup(None)

        assert Action.BACKUP == self.channel.get_message().action
        assert self.channel.get_message().stageComplete != None
        assert self.channel.get_message().stageComplete.success == False

    def test_prepareForRestore_agentBehaviorThrowsException_catchesAndSendsStageCompleteFalse(self, setUp):
        agent = Agent(ExceptionAgentBehavior(), self.channel, None, None)

        agent.prepare_for_restore(None)

        assert Action.RESTORE == self.channel.get_message().action
        assert self.channel.get_message().stageComplete.success == False

    def test_executeRestore_agentBehaviorThrowsException_catchesAndSendsStageCompleteFalse(self, setUp):
        agent = Agent(ExceptionAgentBehavior(), self.channel, None, None)

        agent.execute_restore(None)

        assert Action.RESTORE == self.channel.get_message().action
        assert self.channel.get_message().stageComplete.success == False

    def test_postRestore_agentBehaviorThrowsException_catchesAndSendsStageCompleteFalse(self, setUp):
        agent = Agent(ExceptionAgentBehavior(), self.channel, None, None)

        agent.post_restore(None)

        assert Action.RESTORE, self.channel.get_message().action
        assert self.channel.get_message().stageComplete.success == False

    def test_cancelAction_agentBehaviorThrowsException_catchesAndSendsStageCompleteFalse(self, setUp):
        agent = Agent(ExceptionAgentBehavior(), self.channel, None, None)

        agent.cancel_action(None)

        assert Action.CANCEL, self.channel.get_message().action
        assert self.channel.get_message().stageComplete.success == False

    def test_register_isRegisteringAgainInTheMiddleOfAnAction_cancelsCurrentActionAndRegistersAgain(self, setUp):
        self.agent.register()

        self.agent.process(TestAgent.getPreparationMessage())

        assert self.agentBehavior.prepared_for_restore()

        self.channel.reset()
        self.agentBehavior.reset()

        self.agent.register()

        assert self.agentBehavior.cancelled_action()

        self.agent.process(TestAgent.getPreparationMessage())

        assert self.agentBehavior.prepared_for_restore()

    @staticmethod
    def getPreparationMessage():
        orchestratorControlMessage = OrchestratorControl()
        orchestratorControlMessage.action = Action.RESTORE
        orchestratorControlMessage.orchestratorMessageType = OrchestratorMessageType.PREPARATION

        orchestratorControlMessage.preparation.backupName = "Backup"
        fragment1 = Fragment()
        fragment1.fragmentId = "id"
        fragment1.sizeInBytes = "size"
        fragment1.version = "version"
        orchestratorControlMessage.preparation.fragment.append(fragment1)

        fragment2 = Fragment()
        fragment2.fragmentId = "id2"
        fragment2.sizeInBytes = "size2"
        fragment2.version = "version2"
        orchestratorControlMessage.preparation.fragment.append(fragment2)

        TestAgent.fillInSoftwareVersionInfo(orchestratorControlMessage.preparation.softwareVersionInfo)

        return orchestratorControlMessage

    @staticmethod
    def fillInSoftwareVersionInfo(softwareVersionInfo):
        softwareVersionInfo.productName = "name"
        softwareVersionInfo.description = "description"
        softwareVersionInfo.productNumber = "id"
        softwareVersionInfo.productionDate = "date"
        softwareVersionInfo.revision = "R1"
        softwareVersionInfo.type = "type"

class TestAgentBehavior(AgentBehavior):

    def __init__(self):
        self.state_executed_backup = False
        self.state_executed_restore = False
        self.state_prepared_for_restore = False
        self.state_performed_post_restore = False
        self.state_cancelled_action = False

    def get_registration_information(self):
        return RegistrationInformationUtil.get_test_registration_information()

    def execute_backup(self, backup_execution_actions):
        self.state_executed_backup = True
        backup_execution_actions.backup_complete(True, "Test backup")

    def prepare_for_restore(self, restore_preparation_actions):
        self.state_prepared_for_restore = True
        restore_preparation_actions.send_stage_complete(True, "stage success")

    def post_restore(self, post_restore_actions):
        self.state_performed_post_restore = True
        post_restore_actions.send_stage_complete(True, "stage success")

    def execute_restore(self, restore_execution_actions):
        self.state_executed_restore = True
        restore_execution_actions.restore_complete(True, "stage success")

    def cancel_action(self, cancel_actions):
        self.state_cancelled_action = True
        cancel_actions.send_stage_complete(True, "stage success")

    def executed_backup(self):
        return self.state_executed_backup

    def executed_restore(self):
        return self.state_executed_restore

    def prepared_for_restore(self):
        return self.state_prepared_for_restore

    def performed_post_restore(self):
        return self.state_performed_post_restore

    def cancelled_action(self):
        return self.state_cancelled_action

    def reset(self):
        self.state_executed_backup = False
        self.state_executed_restore = False
        self.state_prepared_for_restore = False
        self.state_performed_post_restore = False
        self.state_cancelled_action = False

class ExceptionAgentBehavior(TestAgentBehavior):
    def execute_backup(self, backupExecutionActions):
        raise Exception("Backup")

    def prepare_for_restore(self, restorePreparationActions):
        raise Exception("Restore preparation")

    def execute_restore(self, restoreExecutionActions):
        raise Exception("Restore execution")

    def post_restore(self, postRestoreActions):
        raise Exception("Restore Post")

    def cancel_action(self, cancelActions):
        raise Exception("Cancel")

class TestOrchestratorGrpcChannel(OrchestratorGrpcChannel):

    def __init__(self):
        self.created_new_stream = False
        self.message = None
        self.shutdown_state = False

    def establish_control_channel(self):
        self.created_new_stream = True

    def send_message(self, message):
        """ Send Message """
        self.message = message

    def sendControlMessage(self, message):
        self.message = message

    def shutdown(self):
        self.shutdown_state = True

    def get_message(self):
        return self.message

    def is_shutdown(self):
        return self.shutdown_state

    def reset(self):
        self.created_new_stream = False
        self.message = None
        self.shutdown_state = False

if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
