from bro_agent.agent.RestoreActions import RestoreActions


class RestorePreparationActions(RestoreActions):
    """ Provides means to perform some preparatory actions prior to restore """

    def __init__(self, agent, restore_information):
        # @param agent The agent performing the restore
        # @param restoreInformation required for restore
        RestoreActions.__init__(self, agent, restore_information)
