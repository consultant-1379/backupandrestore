import grpc
from unittest.mock import MagicMock
from unittest.mock import patch

from bro_agent.agent.AgentBehavior import AgentBehavior
from bro_agent.agent.AgentFactory import AgentFactory
from bro_agent.agent.OrchestratorConnectionInformation import OrchestratorConnectionInformation
from bro_agent.registration.RegistrationInformation import RegistrationInformation
from bro_agent.registration.SoftwareVersion import SoftwareVersion

class TestAgentBehavior(AgentBehavior):
    def get_registration_information(self):
        softwareVersion = SoftwareVersion(productName = "Python Test Agent",
                                         productNumber = "0.0",
                                         revision = "0",
                                         productionDate = "01/01/01",
                                         description = "Python test agent for backup handling",
                                         type = "Python")
        self.registrationInformation = RegistrationInformation("PyAgent1", "alpha", softwareVersion)
        return self.registrationInformation

    def execute_backup(self, backup_execution_actions):
        pass

    def execute_restore(self, restore_execution_actions):
        pass


def test_create_agent():
    connection = OrchestratorConnectionInformation("localhost", "3000")
    agentFactory = AgentFactory("NoCred")
    mock_channel = MagicMock()

    with patch("grpc.insecure_channel", mock_channel):
        agent = agentFactory.create_agent(connection, TestAgentBehavior())

    agent.shutdown()
    assert agent != None
    mock_channel.assert_called()

if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
