import bro_agent.generated.Action_pb2 as Action
from bro_agent.service.BackupService import BackupService


class BackupExecutionActions:
    """ BackupExecutionActions """
    def __init__(self, agent, action_information):
        self.agent = agent
        self.action_information = action_information

    def send_backup(self, fragment_information):
        """ Send Backup """
        backup_service = BackupService(self.agent.channel)
        backup_service.backup(fragment_information,
                              self.agent.get_agent_id(),
                              self.action_information.backup_name)

    def backup_complete(self, success, message):
        self.agent.send_stage_complete_message(success, message, Action.BACKUP)
        self.agent.backup_name = None
        self.agent.backup_type = None

    def get_backup_name(self):
        return self.action_information.backup_name

    def get_backup_type(self):
        return self.action_information.backup_type
