import logging
import grpc

import bro_agent.agent.states
from bro_agent.generated.AgentControl_pb2 import AgentControl
from bro_agent.generated.AgentControl_pb2 import AgentMessageType
from bro_agent.generated.AgentControl_pb2 import Register
from bro_agent.generated.AgentControl_pb2 import StageComplete
from bro_agent.generated.Action_pb2 import Action
from bro_agent.generated.SoftwareVersionInfo_pb2 import SoftwareVersionInfo


class Agent:
    """ Agent class """
    def __init__(self, agent_behavior, orchestrator_channel, orchestrator_connection_information, credentials):
        self.agent_behavior = agent_behavior
        self.orchestrator_connection_information = orchestrator_connection_information
        self.credentials = credentials

        self.channel = orchestrator_channel
        self.channel.set_process_callback(self.process)
        self.channel.set_on_error(self.on_error)
        self.backup_name = ""
        self.backup_type = ""
        self.state = bro_agent.agent.WaitingForActionState.WaitingForActionState(self)

    def register(self):
        """ Register """
        logging.info("Attempting to send registration message to Orchestrator")
        self.change_state(self.state.cancel_action())
        logging.info("Establishing control channel with Agent<%s>.", self.get_agent_id())
        self.channel.establish_control_channel()
        logging.info("Agent <%s> has established a control channel.", self.get_agent_id())
        self.channel.send_message(self.get_registration_message())
        logging.info("Registration message sent.")

    def on_error(self):
        logging.error("Error occured! Trying to re-register to BRO")
        if self.orchestrator_connection_information.tls_enabled:
            channel = grpc.secure_channel('{}:{}'.format(self.orchestrator_connection_information.host, self.orchestrator_connection_information.port), self.credentials)
        else:
            channel = grpc.insecure_channel('{}:{}'.format(self.orchestrator_connection_information.host, self.orchestrator_connection_information.port))

        if not self.channel.agent_alive:
            try:
                self.agent_behavior.on_error()
            except Exception:
                logging.error("Exception executing agent behavior on_error method. This exception should have been caught by the agent")
        else:
            self.reset(channel)
            self.channel.send_message(self.get_registration_message())

    def reset(self, channel):
        self.channel.channel_init(channel)
        self.backup_name = ""
        self.backup_type = ""
        self.state = bro_agent.agent.WaitingForActionState.WaitingForActionState(self)

    def process(self, message):
        """ Process a message"""
        self.change_state(self.state.process_message(message))

    def shutdown(self):
        """ Shut down """
        self.channel.shutdown()

    def execute_backup(self, backup_execution_actions):
        """ Execute the backup """
        self.safely_execute_agent_behavior(
            lambda: self.agent_behavior.execute_backup(backup_execution_actions), Action.BACKUP)

    def execute_restore(self, restore_execution_actions):
        """ Execute the restore """
        self.safely_execute_agent_behavior(
            lambda: self.agent_behavior.execute_restore(restore_execution_actions), Action.RESTORE)

    def prepare_for_restore(self, preparation_actions):
        self.safely_execute_agent_behavior(
            lambda: self.agent_behavior.prepare_for_restore(preparation_actions), Action.RESTORE)

    def post_restore(self, post_actions):
        self.safely_execute_agent_behavior(
            lambda: self.agent_behavior.post_restore(post_actions), Action.RESTORE)

    def cancel_action(self, cancel_actions):
        self.safely_execute_agent_behavior(
            lambda: self.agent_behavior.cancel_action(cancel_actions), Action.RESTORE)

    def get_restore_data_iterator(self, metadata):
        return self.channel.get_restore_data_iterator(metadata)

    def get_agent_id(self):
        return self.agent_behavior.get_registration_information().agent_id

    def get_backup_name(self):
        return self.backup_name

    def send_stage_complete_message(self, success, message, action):
        """ Send the stage complete message """
        self.channel.send_message(self.get_stage_complete_message(message, success, action))

    def change_state(self, state_change):
        self.state = state_change
        self.state.trigger()

    def finish_action(self):
        self.change_state(self.state.finish_action())

    def safely_execute_agent_behavior(self, behavior, action):
        """ Safely execute a behavior """
        try:
            behavior()
        except Exception:
            logging.error("Exception executing agent behavior. This exception should have been caught by the agent")
            self.send_stage_complete_message(False, "Exception executing agent behavior", action)

    def get_stage_complete_message(self, message, success, action):
        """ Get the stage complete message """
        stage_complete = StageComplete(success=success,
                                       agentId=self.agent_behavior.get_registration_information().agent_id,
                                       message=message)
        agent_control = AgentControl(action=action,
                                     agentMessageType=AgentMessageType.STAGE_COMPLETE,
                                     stageComplete=stage_complete)
        return agent_control

    def get_registration_message(self):
        logging.info("get_registration_message")
        registration_info = self.agent_behavior.get_registration_information()
        software_version_info = SoftwareVersionInfo(productName=registration_info.software_version.productName,
                                                    productNumber=registration_info.software_version.productNumber,
                                                    revision=registration_info.software_version.revision,
                                                    productionDate=registration_info.software_version.productionDate,
                                                    description=registration_info.software_version.description,
                                                    type=registration_info.software_version.type)

        register = Register(agentId=registration_info.agent_id,
                            apiVersion="2.0",
                            scope=registration_info.scope,
                            softwareVersionInfo=software_version_info)

        agent_control = AgentControl(action=Action.REGISTER,
                                     agentMessageType=AgentMessageType.REGISTER,
                                     register=register)

        return agent_control
