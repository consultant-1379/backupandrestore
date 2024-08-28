import logging
import bro_agent.agent.states

from bro_agent.agent.RestorePreparationActions import RestorePreparationActions
from bro_agent.generated.Action_pb2 import Action
from bro_agent.generated.OrchestratorControl_pb2 import OrchestratorMessageType
from bro_agent.agent.RestoreInformation import RestoreInformation


class RestorePreparationState(bro_agent.agent.RestoreState.RestoreState):
    """ Performs preparatory actions prior to restore """

    def __init__(self, agent, preparation):
        # @param agent The agent performing the restore
        # @param preparation preparation message obtained during restore
        bro_agent.agent.RestoreState.RestoreState.__init__(self, agent, RestoreInformation.from_preparation(preparation))
        agent.backup_name = preparation.backupName

    def process_message(self, message):
        if self.is_execution_message(message):
            logging.info("Received restore execution message for backup: %s", self.restore_information.backup_name)
            return bro_agent.agent.RestoreExecutionState.RestoreExecutionState(self.agent, self.restore_information)
        if self.is_cancel_message(message):
            logging.info("Received cancel restore message for backup: %s", self.restore_information.backup_name)
            return self.cancel_action()

        self.agent.sendStageCompleteMessage(False, "Unsuccessful restore stage", Action.RESTORE)
        return bro_agent.agent.WaitingForActionState.WaitingForActionState(self.agent)

    def is_execution_message(self, message):
        """ Is the meesage a proper execution message? """
        return (message.orchestratorMessageType == OrchestratorMessageType.EXECUTION and message.execution is not None)

    def trigger(self):
        self.agent.prepare_for_restore(RestorePreparationActions(self.agent, self.restore_information))

    def finish_action(self):
        self.agent.sendStageCompleteMessage(False,
                                            "Finish action called during restore preparation",
                                            Action.RESTORE)
        return bro_agent.agent.FinishedActionState.FinishedActionState(self.agent,
                                                                       self.restore_information.backup_name,
                                                                       Action.RESTORE)
