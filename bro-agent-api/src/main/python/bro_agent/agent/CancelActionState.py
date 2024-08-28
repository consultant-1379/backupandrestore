import bro_agent.agent.states

from bro_agent.agent.CancelActions import CancelActions


class CancelActionState(bro_agent.agent.AgentState.AgentState):
    """ Performs actions upon receiving cancel message """

    def __init__(self, agent, backup_name, action):
        # @param agent The agent participating in the action
        # @param backupName Name of the backup
        # @param action type of action that being executed
        bro_agent.agent.AgentState.AgentState.__init__(self, agent)
        self.action = action
        self.backup_name = backup_name

    def process_message(self, message):
        return bro_agent.agent.WaitingForActionState.WaitingForActionState(self.agent)

    def trigger(self):
        self.agent.cancel_action(CancelActions(self.agent, self.backup_name, self.action))
        self.agent.finish_action()

    def finish_action(self):
        return bro_agent.agent.WaitingForActionState.WaitingForActionState(self.agent)

    def cancel_action(self):
        return self.finish_action(self)
