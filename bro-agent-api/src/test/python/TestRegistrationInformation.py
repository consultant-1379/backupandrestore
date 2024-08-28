from bro_agent.registration.RegistrationInformation import RegistrationInformation

def test_registration_information_can_be_constructed():
    registration_info = RegistrationInformation("TestAgent", "beta", "1.0", "v1.0")
    assert \
        registration_info.agent_id == "TestAgent" and \
        registration_info.scope == "beta" and \
        registration_info.software_version == "1.0" and \
        registration_info.api_version == "v1.0"

def test_api_version_validation():
    registration_info = RegistrationInformation("TestAgent", "beta", "1.0")
    try:
        registration_info.validate_api_version()
        assert False
    except:
        assert True

def test_software_version_validation():
    registration_info = RegistrationInformation("TestAgent", "beta", api_version="v1.0")
    try:
        registration_info.validate_software_version()
        assert False
    except:
        assert True

def test_agent_id_validation():
    registration_info = RegistrationInformation(scope="beta", software_version="1.0", api_version="v1.0")
    try:
        registration_info.validate_agent_id()
        assert False
    except:
        assert True

def test_empty_agent_id():
    registration_info = RegistrationInformation(agent_id="", scope="beta", software_version="1.0", api_version="v1.0")
    try:
        registration_info.validate_agent_id()
        assert False
    except:
        assert True

def test_scope_validation():
    registration_info = RegistrationInformation("TestAgent", software_version="1.0", api_version="v1.0")
    try:
        registration_info.validate_scope()
        assert False
    except:
        assert True

def test_validation():
    registration_info = RegistrationInformation("TestAgent", "beta", "1.0", "v1.0")
    try:
        registration_info.validate()
        assert True
    except:
        assert False


if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
