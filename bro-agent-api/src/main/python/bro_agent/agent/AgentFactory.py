import grpc
import logging

from bro_agent.agent.Agent import Agent
from bro_agent.agent.OrchestratorGrpcChannel import OrchestratorGrpcChannel


class AgentFactory:
    """ Agent Factory """
    def __init__(self, credentials=None):
        self.credentials = credentials

    def create_agent(self, orchestrator_connection_information, agent_behavior):
        """ Create the agent """
        logging.info(
            "Creating agent to communicate with orchestrator at host <%s> and port <%s>",
            orchestrator_connection_information.host, orchestrator_connection_information.port
        )

        if self.credentials is None:
            self.credentials = self.read_credentials(orchestrator_connection_information)

        connection_information = '{}:{}'.format(orchestrator_connection_information.host, orchestrator_connection_information.port)

        if orchestrator_connection_information.tls_enabled:
            logging.info("Agent has TLS enabled")
            channel = grpc.secure_channel(connection_information, self.credentials)
        else:
            logging.info("Agent has TLS disabled")
            channel = grpc.insecure_channel(connection_information)

        agent = Agent(agent_behavior, OrchestratorGrpcChannel(channel), orchestrator_connection_information, self.credentials)

        agent.register()
        return agent

    @staticmethod
    def read_credentials(orchestrator_connection_information):
        """ Read the credentials """
        with open(orchestrator_connection_information.certificate_authority_path, 'rb') as file:
            trusted_certs = file.read()

        return grpc.ssl_channel_credentials(root_certificates=trusted_certs)
