from abc import ABC, abstractmethod

from bro_agent.generated.OrchestratorControl_pb2 import OrchestratorMessageType


class AgentState(ABC):
    """ Base class for AgentState classes """

    def __init__(self, agent):
        # @param agent the Agent performing action
        self.agent = agent

    @abstractmethod
    def process_message(self, message):
        """Process control messages from Orchestrator"""
        # @param message OrchestratorControl message
        # @return Agent state change
        pass

    @abstractmethod
    def trigger(self):
        """ execute state's actions """
        pass

    @abstractmethod
    def finish_action(self):
        """ Finish the current action """
        # @return Agent state
        pass

    @abstractmethod
    def cancel_action(self):
        """ Cancel current action """
        # @return next state
        pass

    @staticmethod
    def is_cancel_message(message):
        """ Indicates if the message is of type CANCEL_BACKUP_RESTORE """
        # @param message Orchestrator control message
        # @return boolean indicating if the message is of type CANCEL_BACKUP_RESTORE
        return ((message.orchestratorMessageType == OrchestratorMessageType.CANCEL_BACKUP_RESTORE)
                and (message.cancel is not None))
