from unittest.mock import Mock

import bro_agent.agent.states
from bro_agent.generated.Action_pb2 import Action
from bro_agent.generated.CancelBackupRestore_pb2 import CancelBackupRestore
from bro_agent.generated.OrchestratorControl_pb2 import OrchestratorControl
from bro_agent.generated.OrchestratorControl_pb2 import OrchestratorMessageType

def test_restore_post_action_state_changes_to_waiting_for_action_state():
    restore_post_action_state = bro_agent.agent.states.RestorePostActionState(Mock(), Mock())
    message = generate_post_actions_message()

    new_state = restore_post_action_state.process_message(message)

    assert isinstance(new_state, bro_agent.agent.WaitingForActionState.WaitingForActionState)


def test_restore_post_action_state_changes_to_waiting_for_action_state_in_case_cancel():
    restore_post_action_state = bro_agent.agent.states.RestorePostActionState(Mock(), Mock())
    message = generate_cancel_message()

    new_state = restore_post_action_state.process_message(message)

    assert isinstance(new_state, bro_agent.agent.WaitingForActionState.WaitingForActionState)


def test_restore_post_action_state_changes_to_finished_action_state():
    restore_post_action_state = bro_agent.agent.states.RestorePostActionState(Mock(), Mock())

    new_state = restore_post_action_state.finish_action()

    assert type(new_state) is bro_agent.agent.FinishedActionState.FinishedActionState


def generate_cancel_message():
    cancel_backup_restore = CancelBackupRestore(
        message = "TestCancelMessage"
    )
    message = OrchestratorControl(
        action = Action.RESTORE,
        orchestratorMessageType = OrchestratorMessageType.CANCEL_BACKUP_RESTORE,
        cancel = cancel_backup_restore
    )
    return message


def generate_post_actions_message():
    message = OrchestratorControl(
        action = Action.RESTORE,
        orchestratorMessageType = OrchestratorMessageType.POST_ACTIONS,
        postActions = {}
    )
    return message


if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
