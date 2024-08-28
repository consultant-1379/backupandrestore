import bro_agent.agent.states


# Final state of an action
# Awaits cancel message or new action message
class FinishedActionState(bro_agent.agent.WaitingForActionState.WaitingForActionState):
    """
    Final state of an action
    Awaits cancel message or new action message
    """
    def __init__(self, agent, backup_name, action):
        # @param agent The agent participating in action
        # @param backupName backup name
        # @param action action type
        bro_agent.agent.WaitingForActionState.WaitingForActionState.__init__(self, agent)
        self.action = action
        self.backup_name = backup_name

    def process_message(self, message):
        """ Process an incoming message"""
        if self.is_cancel_message(message) is True:
            return bro_agent.agent.CancelActionState.CancelActionState(self.agent, self.backup_name, self.action)
        return bro_agent.agent.WaitingForActionState.WaitingForActionState.process_message(self, message)
