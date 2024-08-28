"""
This module is used to validate BRO dual port feature.
"""
import time
import requests
import utilprocs


class DualPortValidator:
    """
    This class is used to validate the BRO's dual REST port feature
    """
    host_port = 7001
    tls_port = 7002

    def __init__(self, hostport=7001, tlsport=7002, ca_cert_path=None):
        """
        Class constructor

        :param hostport: plain text port, default 7001
        :param tlsport: encrypted port, default 7002
        :param ca_cert_path: Certificate path
        """
        self.ca_cert_path = ca_cert_path
        self.host_port = DualPortValidator.geturl("http", hostport)
        self.tls_port = DualPortValidator.geturl("https", tlsport)

    @staticmethod
    def geturl(protocol="http", port=7001):
        """
        Creates an url base from protocol and port

        :param protocol: String either "http" or "https"
        :param port: integer port
        :return: an String indicating the URL
        """
        return "{}://eric-ctrl-bro:{}/v1".format(protocol, port)

    @staticmethod
    def validate_ports_orchestrator(health_url, verify):
        """
        Validates the health_url, if health endpoint fails, the
        function will retry 10 times waiting for 1 second in each call

        :param health_url: url representing the health service exposed by BRO
        :param verify: to indicate if a tls certificate is used or not
        :return: an array with the response, including status
        """
        utilprocs.log("Get Orchestrator Health: {}".format(health_url))
        count = 0
        while True:
            try:
                response = requests.get(health_url, timeout=3,
                                        verify=verify)
                # Raise if exception other than networking
                response.raise_for_status()
                break
            except requests.ConnectionError as err:
                # log the networking exception
                utilprocs.log(
                    "Received ConnectionError running health check: {}"
                    .format(err))
                if count > 10:
                    raise Exception(
                        "Continuous Network Errors on the Cluster") from err
                count = count + 1
                time.sleep(1)
        # return the response as a dictionary
        return response.json()

    @staticmethod
    def is_valid_url(health_url, verify):
        """
        Validates the health_url, if health endpoint fails, the
        function will retry 10 times waiting for 1 second in each call

        :param health_url: url representing the health service exposed by BRO
        :param verify: to indicate if a tls certificate is used or not
        :return: boolean: true if the url is reachable false otherwise
        """
        try:
            response = requests.get(health_url, timeout=1, verify=verify)
            response.raise_for_status()
            return True
        except requests.ConnectionError as err:
            utilprocs.log(
                "Received ConnectionError checking url: {}"
                .format(err))
            return False

    def validate_http_tls_ports(self):
        """
        Basic validation for both ports HTTP and TLS.

        :return: Boolean True both ports are valid, otherwise False
        on error throws the exception
        """
        health_url = "{}/health".format(self.host_port)
        status_http = DualPortValidator. \
            validate_ports_orchestrator(health_url, False)['status']
        health_url = "{}/health".format(self.tls_port)

        if DualPortValidator.is_valid_url(health_url, self.ca_cert_path):
            status_tls = DualPortValidator.validate_ports_orchestrator(
                health_url, self.ca_cert_path)['status']
        else:
            self.tls_port = DualPortValidator.geturl("https", 7001)
            health_url = "{}/health".format(self.host_port)
            status_tls = DualPortValidator.validate_ports_orchestrator(
                health_url, self.ca_cert_path)['status']

        assert status_http == "Healthy", "Unexpected http error on: {} ". \
            format(DualPortValidator.host_port)
        assert status_tls == "Healthy", "Unexpected tls error on: {} ". \
            format(DualPortValidator.tls_port)
