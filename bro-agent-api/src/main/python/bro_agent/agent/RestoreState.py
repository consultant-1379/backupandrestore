from abc import ABC
import bro_agent.agent.states

from bro_agent.generated.Action_pb2 import Action


class RestoreState(bro_agent.agent.AgentState.AgentState, ABC):
    """ Represents agent participating in a restore. """

    def __init__(self, agent, restore_information):
        # @param agent The agent performing the restore
        # @param restoreInformation information required for restore
        bro_agent.agent.AgentState.AgentState.__init__(self, agent)
        self.restore_information = restore_information

    def cancel_action(self):
        """ Canceling the Action """
        return bro_agent.agent.CancelActionState.CancelActionState(self.agent,
                                                                   self.restore_information.backup_name,
                                                                   Action.RESTORE)
