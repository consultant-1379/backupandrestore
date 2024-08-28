from bro_agent.exception.Exceptions import InvalidRegistrationInformationException


class RegistrationInformation:
    def __init__(self, agent_id=None, scope=None, software_version=None, api_version=None):
        self.agent_id = agent_id
        self.scope = scope
        self.software_version = software_version
        self.api_version = api_version

    def validate_api_version(self):
        if self.api_version is None:
            raise InvalidRegistrationInformationException("Api version is not specified!")

    def validate_software_version(self):
        if self.software_version is None:
            raise InvalidRegistrationInformationException("Software version is not specified!")

    def validate_agent_id(self):
        if self.agent_id is None:
            raise InvalidRegistrationInformationException("Agent id is not specified!")
        elif self.agent_id == "":
            raise InvalidRegistrationInformationException("Agent id is empty!")

    def validate_scope(self):
        if self.scope is None:
            raise InvalidRegistrationInformationException("Scope is not specified!")

    def validate(self):
        self.validate_agent_id()
        self.validate_api_version()
        self.validate_scope()
        self.validate_software_version()
