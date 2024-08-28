from abc import ABC
from abc import abstractmethod


class AgentBehavior(ABC):
    """ Agent Behavior """

    @abstractmethod
    def get_registration_information(self):
        """ Gets agent's registration information. """
        pass

    @abstractmethod
    def execute_backup(self, backup_execution_actions):
        """ Used to call an agents custom code for a backup from the orchestrator """
        # @param provides methods to allow an agent to transfer
        #        backups and indicate that a backup has completed.
        pass

    @abstractmethod
    def execute_restore(self, restore_execution_actions):
        """ Used to call an agents custom code for a restore from the orchestrator """
        # @param provides methods to download fragments to be restored
        #        by an agent and indicate that a restore has completed.
        pass

    def prepare_for_restore(self, restore_preparation_actions):
        """ Used to do some preparation before restore is called """
        # @param provides method to indicate that preparation for restore has completed.
        restore_preparation_actions.send_stage_complete(True,
                                                        "Preparation for restore is successful")

    def post_restore(self, post_restore_actions):
        """"Used to do perform some actions post restore """
        # @param provides method to indicate that post restore actions has completed.
        post_restore_actions.send_stage_complete(True, "Post actions completed")

    def cancel_action(self, cancel_actions):
        """ Used to do perform some actions on cancel """
        # @param provides method to indicate that cancel actions has completed.
        cancel_actions.send_stage_complete(True, "Cancel actions completed")

    def on_error(self):
        """ Used to call to perform some error handling in case agent cannot communicate
            properly and will not try to register again to BRO """
        pass
