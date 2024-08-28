from unittest.mock import Mock

import bro_agent.agent.states
from bro_agent.generated.Action_pb2 import Action
from bro_agent.generated.Fragment_pb2 import Fragment
from bro_agent.generated.OrchestratorControl_pb2 import OrchestratorControl
from bro_agent.generated.OrchestratorControl_pb2 import OrchestratorMessageType
from bro_agent.generated.OrchestratorControl_pb2 import Preparation
from bro_agent.generated.SoftwareVersionInfo_pb2 import SoftwareVersionInfo

def test_backup_preparation_message_changes_to_backup_preparation_state():
    waiting_for_action_state = bro_agent.agent.states.WaitingForActionState(Mock())
    message = generate_message(Action.BACKUP)

    new_state = waiting_for_action_state.process_message(message)

    assert isinstance(new_state, bro_agent.agent.BackupExecutionState.BackupExecutionState)


def test_restore_preparation_message_changes_to_restore_preparation_state():
    waiting_for_action_state = bro_agent.agent.states.WaitingForActionState(Mock())
    message = generate_message(Action.RESTORE)

    new_state = waiting_for_action_state.process_message(message)

    assert isinstance(new_state, bro_agent.agent.RestorePreparationState.RestorePreparationState)


def test_restore_execution_message_does_not_changes_state():
    waiting_for_action_state = bro_agent.agent.states.WaitingForActionState(Mock())
    message = generate_message(Action.RESTORE, OrchestratorMessageType.EXECUTION)

    new_state = waiting_for_action_state.process_message(message)

    assert isinstance(new_state, bro_agent.agent.WaitingForActionState.WaitingForActionState)


def generate_message(message_action, message_type=OrchestratorMessageType.PREPARATION):
    version = SoftwareVersionInfo(
        productName = "TestProduct",
        productNumber = "TestNumber",
        revision = "TestRevision",
        productionDate = "1990-01-01",
        description = "TestSoftware",
        type = "TestType",
    )
    fragment = Fragment(
        fragmentId = "1",
        version = "0.0",
        sizeInBytes = "5",
        customInformation = {"cus": "tom"}
    )
    preparation = Preparation(
        backupName = "TestBackup",
        softwareVersionInfo = version,
        fragment = [fragment],
        backupType = "DEFAULT"
    )
    message = OrchestratorControl(
        action = message_action,
        orchestratorMessageType = message_type,
        preparation = preparation
    )
    return message


if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
