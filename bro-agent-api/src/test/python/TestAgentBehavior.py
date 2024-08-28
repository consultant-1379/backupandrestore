from bro_agent.agent.AgentBehavior import AgentBehavior
from bro_agent.agent.BackupExecutionActions import BackupExecutionActions
from bro_agent.agent.CancelActions import CancelActions
from bro_agent.agent.PostRestoreActions import PostRestoreActions
from bro_agent.agent.RestoreExecutionActions import RestoreExecutionActions
from bro_agent.agent.RestorePreparationActions import RestorePreparationActions
from bro_agent.registration.RegistrationInformation import RegistrationInformation
from RegistrationInformationUtil import RegistrationInformationUtil

class TestAgentBehavior(AgentBehavior):
    def __init__(self):
        self.executed_backup = False
        self.executed_restore = False
        self.prepared_for_restore = False
        self.performed_post_restore = False
        self.cancelled_action = False

    def get_registration_information(self):
        return RegistrationInformationUtil.get_test_registration_information()

    def execute_backup(self, backupExecutionActions):
        self.executed_backup = True
        backupExecutionActions.backup_complete(True, "Test backup")

    def prepare_for_restore(self, restorePreparationActions):
        self.prepared_for_restore = True
        restorePreparationActions.send_stage_complete(True, "stage success")

    def post_restore(self, postRestoreActions):
        self.performed_post_restore = True
        postRestoreActions.send_stage_complete(True, "stage success")

    def execute_restore(self, restoreExecutionActions):
        self.executed_restore = True
        restoreExecutionActions.restore_complete(True, "stage success")

    def cancel_action(self, cancelActions):
        self.cancelled_action = True
        cancelActions.send_stage_complete(True, "stage success")

    def executedBackup(self):
        return self.executed_backup

    def executedRestore(self):
        return self.executed_restore

    def preparedForRestore(self):
        return self.prepared_for_restore

    def performedPostRestore(self):
        return self.performed_post_restore

    def cancelledAction(self):
        return self.cancelled_action

    def reset(self):
        self.executed_backup = False
        self.executed_restore = False
        self.prepared_for_restore = False
        self.performed_post_restore = False
        self.cancelled_action = False
