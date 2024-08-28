from bro_agent.agent.RestoreActions import RestoreActions


class PostRestoreActions(RestoreActions):
    # * @param agent The agent performing the restore
    # * @param restoreInformation required for restore
    def __init__(self, agent, restore_information):
        RestoreActions.__init__(self, agent, restore_information)
