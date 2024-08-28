import logging
import bro_agent.agent.states

from bro_agent.generated.OrchestratorControl_pb2 import OrchestratorMessageType
from bro_agent.generated.Action_pb2 import Action


class WaitingForActionState(bro_agent.agent.AgentState.AgentState):
    """ Waits for Orchestrator control message """

    def __init__(self, agent):
        # @param agent The Agent waiting for action
        bro_agent.agent.AgentState.AgentState.__init__(self, agent)

    @staticmethod
    def is_preparation_message(message):
        """ Is the message a Preparation message? """
        return (message.orchestratorMessageType == OrchestratorMessageType.PREPARATION) and (message.preparation is not None)

    def process_message(self, message):
        if self.should_start_backup(message):
            logging.info("The Orchestrator has requested that the Agent creates backup: %s", message.preparation.backupName)
            return bro_agent.agent.BackupExecutionState.BackupExecutionState(self.agent, message.preparation)
        if self.should_start_restore(message):
            logging.info("The Orchestrator has requested that the Agent restores backup: %s", message.preparation.backupName)
            return bro_agent.agent.RestorePreparationState.RestorePreparationState(self.agent, message.preparation)
        logging.warning("The Orchestrator has requested an unknown operation: %s", message.action)
        return self

    def should_start_backup(self, message):
        return Action.BACKUP == message.action and self.is_preparation_message(message)

    def should_start_restore(self, message):
        return Action.RESTORE == message.action and self.is_preparation_message(message)

    def trigger(self):
        pass

    def finish_action(self):
        return self

    def cancel_action(self):
        return self
