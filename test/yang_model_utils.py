"""
This module contains utilities used to verify yang models.
"""

import time
import re
import os
import bro_data
from utilprocs import log
from bro_utils import assert_equal


class YangModelUtils:
    """
    This class is used to verify yang models in the CMYP CLI
    """

    def __init__(self, cmyp_instance1, cmyp_instance2, bro_ctrl_client):
        """
        This is the constructor of the class.

        :param cmyp_instance1: An instance of CMYP client
        :param cmyp_instance2: A second isntance of CMYP client
        :param bro_ctrl_client: A BroCtrlClient object
        """

        self.cmyp_instance1 = cmyp_instance1
        self.cmyp_instance2 = cmyp_instance2
        self.bro_ctrl_client = bro_ctrl_client

    def verify_brm_config(self):
        """
        Verifies that the expected BRM configuration is in CMYP

        :raises: Assertion if expected BRMs are not in CMYP
        """

        log("Verifying brm config")
        # Fetch backup-managers in BRO
        bro_brms = self.bro_ctrl_client.get_all_backup_managers()
        # Fetch brms in CMYP
        cmyp_brms = self.get_brm_config()

        assert_equal(cmyp_brms, bro_brms)

    def get_brm_config(self):
        """
        Fetches the backup managers available in the CMYP configuration.

        :return: a list of the brms in CMYP
        """

        log("Fetching backup managers from brm config")
        # Fetch configuration from CMYP
        command = "screen-length 100 ; show running-config brm"
        expected_response_substring = "brm backup-manager DEFAULT"

        response = self.cmyp_instance1.operational_command(
            command, expected_response_substring)

        brms = []
        for entry in response.splitlines():
            if 'brm' in entry:
                brms.append(entry.split()[-1])
        return brms

    def get_model_config(self, model):
        """
        Checks if the model is available in the CMYP configuration.

        :return: the response received from the model in CMYP
        """

        log("Fetching {} config".format(model))
        # Fetch configuration from CMYP
        command = "show running-config {}".format(model)
        expected_response_substring = model

        response = self.cmyp_instance1.operational_command(
            command, expected_response_substring)

        config_response = []
        for entry in response.splitlines():
            if model in entry:
                config_response.append(entry.split()[-1])

        return config_response

    def get_certm_model_config(self, model):
        """
        Checks if the CertM model is available in the CMYP configuration.

        :return: the response received from the CertM model in CMYP
        """

        log("Fetching {} config".format(model))
        # Fetch configuration from CMYP
        command = "{}".format(model)
        expected_response_substring = "syntax"
        response = self.cmyp_instance2.operational_command(
            command, expected_response_substring)
        return response

    def define_sftp_server(self, sftp_server_name, endpoint,
                           remote_address, remote_port, remote_path,
                           client_username, client_public_key,
                           client_private_key, server_host_key,
                           backup_manager="DEFAULT"):
        """
        Defines an SFTP server

        :param sftp_server_name: The SFTP server name.
        :param endpoint: The endpoint for the SFTP server.
        :param remote_address: The remote address for the SFTP server.
        :param remote_port:  The remote port for the SFTP server.
        :param remote_path:  The remote path for the SFTP server.
        :param client_username:  The clients username for the SFTP server.
        :param client_public_key: The public key for the SFTP server.
        :param client_private_key: The private key for the SFTP server.
        :param server_host_key: The host key for the SFTP server.
        Accepted single key or an array of keys
        :param backup_manager: The backup manager the command will be ran on.
        """

        log("Defining the {} SFTP server within backup manager {}"
            .format(sftp_server_name, backup_manager))
        commands = []

        command = ("brm backup-manager {} sftp-server {} endpoints "
                   "endpoint {}").format(backup_manager,
                                         sftp_server_name,
                                         endpoint)

        address = ("{} remote-address {}").format(command, remote_address)
        commands.append(address)

        port = ("{} remote-port {}").format(command, remote_port)
        commands.append(port)

        path = ("{} remote-path {}").format(command, remote_path)
        commands.append(path)

        client_identity_public = ("{} client-identity username {} public-key "
                                  "local-definition public-key {}").format(
                                      command, client_username,
                                      client_public_key)
        commands.append(client_identity_public)

        client_identity_private = ("{} client-identity username {} public-key "
                                   "local-definition private-key {}").format(
                                       command, client_username,
                                       client_private_key)
        commands.append(client_identity_private)

        if isinstance(server_host_key, list):
            server_host_key = "[ {} ]".format(" ".join(server_host_key))

        host_keys = ("{} server-authentication ssh-host-keys "
                     "local-definition host-key {}").format(command,
                                                            server_host_key)
        commands.append(host_keys)

        self.cmyp_instance1.config_commands(commands)

    @staticmethod
    def verify_sftp_persisted_data_in_brm(operation, sftp_filename,
                                          sftp_server_path,
                                          file_count, bro_pod_name,
                                          bro_container_name,
                                          namespace,
                                          fields_to_verify=None):
        """
        Verify that the sftp server data(file) has been
        created/removed in the Orchestrator.

        :param fields_to_verify: fields to be verified in the sftp file
        :param sftp_filename: Expected SFTP server persist filename.
        :param file_count: Expected file counts in the path.
        :param operation: create or remove file.
        :param sftp_server_path: The filepath to the SFTP server in BRM.
        :param bro_pod_name: BRO pod name.
        :param bro_container_name:  BRO container name.
        :param namespace:  namespace where orchestrator pod is deployed.
        """
        if fields_to_verify is None:
            fields_to_verify = []
        count = 0
        log("Waiting sftp server data in file {}".format(sftp_filename))
        while count < 15:
            files = []
            files += bro_data.get_container_files(sftp_server_path,
                                                  bro_pod_name,
                                                  namespace,
                                                  container=bro_container_name)
            if len(files) != file_count:
                log("Waiting sftp server data {}".format(operation))
                time.sleep(10)
            else:
                break
            count += 1
        if operation == "create":
            assert sftp_filename in files, \
                "Timeout waiting sftp server data create"
            file_path = os.path.join(sftp_server_path, sftp_filename)
            file_content = bro_data.read_container_content(
                file_path,
                bro_pod_name,
                namespace,
                container=bro_container_name)
            log("Content of {}: {}".format(sftp_filename, file_content))
            log("path {}".format(file_path))
            for field in fields_to_verify:
                log("field is {}".format(field))
                assert field in file_content, \
                    f"F {field} n {sftp_filename}"
        elif operation == "remove":
            assert sftp_filename not in files, \
                "Timeout waiting sftp server data remove"

    def get_sftp_server_config(self, backup_manager):
        """
        gets the sftp server config

        :param backup_manager: The backup manager
        :return: The response from the operational command.
        """
        command = (
            "show running-config brm backup-manager {} sftp-server"
        ).format(backup_manager)

        expected_response_substring = (
            "brm backup-manager {}"
        ).format(backup_manager)
        response = self.cmyp_instance1.operational_command(
            command, expected_response_substring)
        return response

    def verify_sftp_server_on_cmyp(
            self,
            sftp_server_name,
            sftp_endpoint,
            sftp_remote_port,
            sftp_remote_path,
            sftp_username,
            public_key,
            host_key,
            backup_manager
    ):
        """
        Verifies the sftp server config available in cmyp with
        the sftp server credentials passed in the function
        :param sftp_server_name: The SFTP server name.
        :param sftp_endpoint: The endpoint for the SFTP server
        :param sftp_remote_port: The remote port for the SFTP server
        :param sftp_remote_path: The remote path for the SFTP server.
        :param public_key: The public key for the SFTP server
        :param sftp_username: The username for the sftp server
        :param host_key: The host key for the SFTP server
        :param backup_manager: The backup manager the command will be ran on

        """
        try:
            defined_sftp_server = self.get_sftp_server_config(
                backup_manager=backup_manager
            )
        except Exception as error:
            print(f"Sftp server not present: {error}")
            return False

        try:
            assert sftp_server_name in defined_sftp_server, (
                f"{sftp_server_name} is incorrect"
            )
            assert sftp_endpoint in defined_sftp_server, (
                f"{sftp_endpoint} is incorrect"
            )
            assert sftp_remote_port in defined_sftp_server, (
                f"{sftp_remote_port} is incorrect"
            )
            assert sftp_remote_path in defined_sftp_server, (
                f"{sftp_remote_path} is incorrect"
            )
            assert sftp_username in defined_sftp_server, (
                f"{sftp_username} is incorrect"
            )
            pattern = r'client-identity public-key[^"]* "(.*?)"'
            match = re.search(pattern, defined_sftp_server)
            checking = str(match.group(1))
            if match:
                stored_public_key = checking.replace(r'\n', '')
                assert stored_public_key == str(public_key), (
                    f"Stored public key doesnt match")
            else:
                print("Public key not found in the dictionary.")
            # Verify Private Key
            pattern = r'client-identity private-key[^"]* "(.*?)"'
            match = re.search(pattern, defined_sftp_server)
            if match:
                stored_private_key = match.group(1).replace(r'\n', '')
                assert str(stored_private_key).startswith('000PASSPHRASE'), (
                    f"Private key is incorrect'"
                )

            else:
                print("Private key not found in the dictionary.")

            # Verify Host Key
            pattern = r'server-authentication ssh-host-keys[^"]* "(.*?)"'
            match = re.search(pattern, defined_sftp_server)
            if match:
                stored_host_key = match.group(1).replace(r'\n', '')
                assert stored_host_key == str(host_key), (
                    f"Stored host key doesn't match"
                )
            else:
                print("Host key not found in the dictionary.")
        except AssertionError as error:
            print(f"Verification failed: {error}")
            return False

        return True

    @staticmethod
    def verify_periodic_event_file_in_brm(operation, event_filename,
                                          periodic_event_path, file_count,
                                          bro_pod_name, bro_container_name,
                                          namespace):
        """
        Verify that the periodic event data(file) has been
        created/removed in the Orchestrator.

        :param operation: create or remove file.
        :param event_filename: Expected periodic event persist filename.
        :param periodic_event_path: The filepath to the periodic event in BRM.
        :param file_count: Expected file counts in the path.
        :param bro_pod_name: BRO pod name.
        :param bro_container_name:  BRO container name.
        :param namespace:  namespace where orchestrator pod is deployed.
        """
        count = 0
        log("Waiting periodic event data {} {}".format(
            event_filename, operation))
        while count < 15:
            files = []
            files += bro_data.get_container_files(periodic_event_path,
                                                  bro_pod_name,
                                                  namespace,
                                                  container=bro_container_name)
            if len(files) != file_count:
                log("Waiting periodic event data {}".format(operation))
                time.sleep(10)
            else:
                break
            count += 1
        if operation == "create":
            assert event_filename in files, \
                "Timeout waiting for the periodic event data to be created"
        elif operation == "remove":
            assert event_filename not in files, \
                "Timeout waiting for the periodic event data to be removed"
        elif operation == "clear":
            assert not files, \
                "Timeout waiting for the periodic event data to be cleared"
