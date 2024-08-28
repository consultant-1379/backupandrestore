class CancelActions:
    """ Provides means to perform some actions on cancel """

    def __init__(self, agent, backup_name, action):
        # @param agent Agent participating in action
        # @param backup_name Name of the backup
        # @param action type of action that being executed
        self.agent = agent
        self.backup_name = backup_name
        self.action = action

    def send_stage_complete(self, success, message):
        """ Once all the actions in the stage is completed call this
        method to inform the Orchestrator that the stage has completed """
        # * @param success
        # *            Inform the Orchestrator if the stage was successful or not
        # * @param message
        # *            Inform the Orchestrator why something went wrong or just that all is well
        self.agent.send_stage_complete_message(success, message, self.action)

    def get_backup_name(self):
        """ Provides the name of the backup. """
        # @return backup_name
        return self.backup_name

    def get_action(self):
        """ Provides the type of action that being executed """
        # @return action type
        return self.action
