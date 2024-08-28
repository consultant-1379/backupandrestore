import logging
import bro_agent.agent.states

from bro_agent.agent.RestoreExecutionActions import RestoreExecutionActions
from bro_agent.generated.Action_pb2 import Action
from bro_agent.generated.OrchestratorControl_pb2 import OrchestratorMessageType


class RestoreExecutionState(bro_agent.agent.RestoreState.RestoreState):
    """ Performs restore """

    def __init__(self, agent, restore_information):
        # @param agent The agent performing the restore
        # @param restoreInformation information required for restore
        bro_agent.agent.RestoreState.RestoreState.__init__(self, agent, restore_information)

    def process_message(self, message):
        if self.is_post_actions_message(message):
            logging.info("Received restore post action message for backup: %s", self.restore_information.backup_name)
            return bro_agent.agent.RestorePostActionState.RestorePostActionState(self.agent, self.restore_information)
        elif self.is_cancel_message(message):
            logging.info("Received cancel restore message for backup: %s", self.restore_information.backup_name)
            return self.cancel_action()

        self.agent.send_stage_complete_message(False, "Unsuccessful restore stage", Action.RESTORE)
        return bro_agent.agent.WaitingForActionState.WaitingForActionState(self.agent)

    @staticmethod
    def is_post_actions_message(message):
        """ Is the message a postaction? """
        return (message.orchestratorMessageType == OrchestratorMessageType.POST_ACTIONS and message.postActions is not None)

    def trigger(self):
        self.agent.execute_restore(RestoreExecutionActions(self.agent, self.restore_information))

    def finish_action(self):
        self.agent.send_stage_complete_message(False,
                                               "Finish action called during restore execution",
                                               Action.RESTORE)
        return bro_agent.agent.FinishedActionState.FinishedActionState(self.agent,
                                                                       self.restore_information.backup_name,
                                                                       Action.RESTORE)
