from bro_agent.agent.RestoreActions import RestoreActions
import bro_agent.generated.Action_pb2 as Action


class RestoreExecutionActions(RestoreActions):
    """ provides method to download the fragments to be restored """

    def __init__(self, agent, restore_information):
        """ provides method to download the fragments to be restored """
        # @param agent The agent performing the restore action
        # @param restoreInformation required for restore
        RestoreActions.__init__(self, agent, restore_information)

    def restore_complete(self, success, message):
        """ Once all fragments have been restored call this method
            to inform the Orchestrator that the restore execution has completed """
        # @param success, inform the Orchestrator if the restore was successful or not
        # @param message, inform the Orchestrator why something went wrong or just that all is well
        self.agent.send_stage_complete_message(success, message, Action.RESTORE)
        self.agent.backup_name = None
        self.agent.backup_type = None
