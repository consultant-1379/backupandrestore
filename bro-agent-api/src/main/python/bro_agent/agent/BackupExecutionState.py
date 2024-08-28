import bro_agent.agent.states

from bro_agent.agent.ActionInformation import ActionInformation
from bro_agent.agent.BackupExecutionActions import BackupExecutionActions
from bro_agent.generated.Action_pb2 import Action


class BackupExecutionState(bro_agent.agent.AgentState.AgentState):
    def __init__(self, agent, preparation):
        bro_agent.agent.AgentState.AgentState.__init__(self, agent)
        self.action_information = ActionInformation.from_preparation(preparation)

    def process_message(self, message):
        return bro_agent.agent.WaitingForActionState.WaitingForActionState(self.agent)

    def trigger(self):
        self.agent.execute_backup(BackupExecutionActions(self.agent, self.action_information))
        self.agent.finish_action()

    def finish_action(self):
        return bro_agent.agent.FinishedActionState.FinishedActionState(self.agent, self.action_information.backup_name, Action.BACKUP)

    def cancel_action(self):
        return bro_agent.agent.WaitingForActionState.WaitingForActionState(self.agent)
