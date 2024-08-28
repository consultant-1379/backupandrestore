import bro_agent.agent.states
import bro_agent.agent.PostRestoreActions

from bro_agent.generated.Action_pb2 import Action


class RestorePostActionState(bro_agent.agent.RestoreState.RestoreState):
    """ Performs actions post restore """
    def __init__(self, agent, restore_information):
        # @param agent The agent performing the restore
        # @param restoreInformation required for restore
        bro_agent.agent.RestoreState.RestoreState.__init__(self, agent, restore_information)

    def process_message(self, message):
        return bro_agent.agent.WaitingForActionState.WaitingForActionState(self.agent)

    def trigger(self):
        self.agent.post_restore(bro_agent.agent.PostRestoreActions.PostRestoreActions(self.agent, self.restore_information))
        self.agent.finish_action()

    def finish_action(self):
        return bro_agent.agent.FinishedActionState.FinishedActionState(self.agent,
                                                                       self.restore_information.backup_name,
                                                                       Action.RESTORE)
