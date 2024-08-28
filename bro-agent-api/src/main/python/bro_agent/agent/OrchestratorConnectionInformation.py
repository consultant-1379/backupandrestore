class OrchestratorConnectionInformation:
    """ Responsible for holding agent configuration for networking and security. """
    def __init__(self, host, port, certificate_authority_name=None,
                 certificate_authority_path=None):
        self.host = host
        self.port = port
        self.certificate_authority_name = certificate_authority_name
        self.certificate_authority_path = certificate_authority_path
        if self.certificate_authority_name is not None:
            self.tls_enabled = True
        else:
            self.tls_enabled = False
