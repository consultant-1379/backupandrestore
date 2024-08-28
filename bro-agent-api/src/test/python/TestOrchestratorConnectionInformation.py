from bro_agent.agent.OrchestratorConnectionInformation import OrchestratorConnectionInformation

def test_simple_instantiation():
    connection = OrchestratorConnectionInformation("localhost", "3000")
    assert connection.host == "localhost"
    assert connection.port == "3000"
    assert connection.tls_enabled == False

def test_secure_instantiation():
    connection = OrchestratorConnectionInformation("localhost", "3000", "auth", "path")
    assert connection.host == "localhost"
    assert connection.port == "3000"
    assert connection.certificate_authority_name == "auth"
    assert connection.certificate_authority_path == "path"
    assert connection.tls_enabled == True

if __name__ == "__main__" :
    import pytest
    raise SystemExit(pytest.main([__file__]))
