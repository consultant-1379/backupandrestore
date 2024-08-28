import os
import sys
import pytest

agent_path = os.path.dirname(os.path.abspath(__file__))

sys.path.append(os.path.join(agent_path, "..", "..", "main", "python"))

from bro_agent.registration.RegistrationInformation import RegistrationInformation
from bro_agent.registration.SoftwareVersion import SoftwareVersion

class RegistrationInformationUtil:
    @staticmethod
    def get_test_registration_information():
        registrationInfo = RegistrationInformation()
        softwareVersion = SoftwareVersion()
        softwareVersion.description = "description"
        softwareVersion.productionDate = "productionDate"
        softwareVersion.productName = "productName"
        softwareVersion.productNumber = "productNumber"
        softwareVersion.type = "type"
        softwareVersion.revision = "revision"
        registrationInfo.agent_id = "123"
        registrationInfo.api_version = "apiVersion"
        registrationInfo.scope = "scope"
        registrationInfo.software_version = softwareVersion
        return registrationInfo

    @staticmethod
    def get_test_registration_information_using_constructors():
        softwareVersion = SoftwareVersion("productName", "productNumber",
            "revision", "productionDate", "description", "type")
        return RegistrationInformation("123", "scope", "apiVersion", softwareVersion)

    @staticmethod
    def get_null_test_registration_information():
        return RegistrationInformation(None, None, None, None)

    @staticmethod
    def get_blank_test_registration_information():
        softwareVersion = SoftwareVersion("", "", "", "", "", "")
        return RegistrationInformation("", "", "", softwareVersion)

    @staticmethod
    def get_test_registration_information_with_invalid_api_version():
        softwareVersion = SoftwareVersion("productName", "productNumber",
            "revision", "productionDate", "description", "type")
        return RegistrationInformation("AwesomeAgent", "Telescope", "", softwareVersion)
