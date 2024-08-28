"""
This module is used to interact with the CMYP CLI.
"""
import re
import time
import json
import paramiko
import rest_utils
import traceback
from utilprocs import log
from bro_utils import assert_equal
from paramiko_expect import SSHClientInteraction


class CMYPCliClient:
    """
    This class is used to connect to the CMYP CLI
    and send commands to it.
    """

    def __init__(self, username, password, bro_ctrl_client, timeout=None):
        """
        This is the constructor of the class.

        :param username: CMYP user
        :param password: password for CMYP user
        :param bro_ctrl_client: A BroCtrlClient object
        :param timeout: Connection timeout in seconds
        """

        self.hostname = "eric-cm-yang-provider-external"
        self.username = username
        self.prompt = "{}.*".format(self.username)
        self.port = 22
        self.password = password
        self.bro_ctrl_client = bro_ctrl_client
        self.client = paramiko.SSHClient()

        if timeout is None:
            self.timeout = 10
        else:
            self.timeout = timeout

    def connect(self):
        """
        Creates the SSH connection towards the CMYP CLI.

        """

        self.client.load_system_host_keys()
        self.client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        self.client.connect(self.hostname, self.port, self.username,
                            self.password)
        log("Connected to CMYP CLI")

    def execute_command_in_cmyp_cli(self, interact, command, response):
        """
        Sends a command to the CMYP CLI.
        Parses the output to remove the CLI command prompt
        and returns the output.

        :param interact: the ssh client session
        :param command: command to be entered into the CMYP CLI
        :param response: expected response to that command
                         This can be a snippet/substring of the expected
                         response as a regex is used either side to handle
                         extra characters.
        :return: parsed output from CMYP CLI
        """

        log("Executing command in CMYP CLI")
        # Receive the CLI prompt
        interact.expect(self.prompt)
        log("Sent command: {}".format(command))
        interact.send(command)
        interact.expect(".*{}.*".format(response))

        output = interact.current_output_clean
        # Parse the response to remove the prompt
        cmd_output = output.rstrip()
        log("Received response: {}".format(cmd_output))

        return cmd_output

    def operational_command(self, command, response):
        """
        Creates a SSH Client Session for operational commands.

        :param command: command to be entered to the CMYP CLI
        :param response: expected response to that command
                         This can be a snippet/substring of the expected
                         response as a regex is used either side to handle
                         extra characters.

        :return: output from the client interaction function
        """
        log("Establishing ssh connection to CMYP")
        try:
            # Create a client session with the CMYP SSH connection
            time.sleep(1)
            with SSHClientInteraction(self.client, self.timeout,
                                      buffer_size=10000,
                                      tty_height=10000,
                                      tty_width=1500) as interact:

                log("Executing operational command in CMYP CLI")
                return self.execute_command_in_cmyp_cli(interact, command,
                                                        response)

        except Exception as e_obj:
            traceback.print_exc()
            raise ValueError("Interaction with CMYP failed: {}"
                             .format(e_obj)) from e_obj

    def config_command(self, command):
        """
        Creates a SSH Client Session for config commands.
        Sends a command through configuration mode and commits config change

        :param command: command to be entered to the CMYP CLI

        """
        self.config_commands([command])

    def config_commands(self, commands):
        """
        Creates a SSH Client Session for config commands.
        Sends a list of commands through configuration mode and
        commits config change

        :param commands: a list of commands to be entered to the CMYP CLI

        """
        log("Establishing ssh connection to CMYP")
        try:
            time.sleep(1)
            # Create a client session with the CMYP SSH connection
            with SSHClientInteraction(self.client, self.timeout,
                                      buffer_size=10000) as interact:

                log("Executing command in CMYP CLI in Config mode")
                # Passes config command and response to client function
                self.execute_command_in_cmyp_cli(interact, "config",
                                                 "Entering configuration mode")
                time.sleep(1)
                # Command that requires config mode is executed
                for command in commands:
                    log("Sent command: {}".format(command))
                    interact.send(command)

                # Passes commit command and response to client function
                self.execute_command_in_cmyp_cli(interact, "commit",
                                                 "Commit complete")

        except Exception as e_obj:
            raise ValueError("Interaction with CMYP failed: {}"
                             .format(e_obj)) from e_obj

    def close(self):
        """
        Closes the SSH connection towards CMYP CLI.

        """
        self.client.close()
        log("Connection to CMYP CLI is closed")

    def verify_brm_config(self):
        """
        Verifies that the expected BRM configuration is in CMYP
        This method is deprecated. Use new method in yang_model_utils.py.

        :raises: Assertion if expected BRMs are not in CMYP
        """

        log("This method is deprecated. Use new method in yang_model_utils.py")
        log("Verifying brm config")
        # Fetch backup-managers in BRO
        bro_brms = self.bro_ctrl_client.get_all_backup_managers()
        # Fetch brms in CMYP
        cmyp_brms = self.get_brm_config()

        assert_equal(cmyp_brms, bro_brms)

    def get_brm_config(self):
        """
        Fetches the backup managers available in the CMYP configuration.
        This method is deprecated. Use new method in yang_model_utils.py.

        :return: a list of the brms in CMYP
        """

        log("This method is deprecated. Use new method in yang_model_utils.py")
        log("Fetching backup managers from brm config")
        # Fetch configuration from CMYP
        command = "screen-length 100 ; show running-config brm"
        expected_response_substring = "brm backup-manager DEFAULT"

        response = self.operational_command(command,
                                            expected_response_substring)

        brms = []
        for entry in response.splitlines():
            if 'brm' in entry:
                brms.append(entry.split()[-1])
        return brms

    def extract_response_from_command(self, command, expected_response):
        """
        Executes a command on CMYP CLI and parses the response.

        :param command: the command to be executed in CMYP
        :param expected_response: the expected partial response to be received
        :return: the desired response or raises an exception if not found
        """

        response = self.operational_command(command, expected_response)
        return self.extract_params_from_response(response, expected_response)

    @classmethod
    def extract_params_from_response(cls, response, expected_param):
        """
        Parses the given response

        :param response: the response to be parsed
        :param expected_param: the expected partial response to be received
        :return: the desired response or raises an exception if not found
        """
        desired_response = None
        for entry in response.splitlines():
            if expected_param in entry:
                desired_response = re.split(expected_param, entry)[1]
                desired_response = desired_response.strip()

        if desired_response:
            return desired_response

        raise ValueError("CMYP didn't return desired response. Response {}"
                         .format(str(response)))

    def execute_yang_action(self, command):
        """
        Executes a yang action and returns the progress-report id

        :param command: the command to be executed in CMYP
        :return: the response from extract_response_from_command
        """

        return self.extract_response_from_command(command, "return-value")

    def export_backup(self, backup_name, uri, password=None,
                      backup_manager="DEFAULT"):
        """
        Runs an export on a given backup manager for an existing backup.

        :param backup_name: The name of the backup to be exported.
        :param uri: The path of the SFTP or HTTP server.
        :param password: The password required for the SFTP server.
        :param backup_manager: The backup manager the action will be ran on.
        :return: a progress-report id
        """

        log("Exporting backup {}".format(backup_name))
        if password:
            command = "brm backup-manager {} backup {} export password {}" \
                      " uri {}"\
                .format(backup_manager, backup_name, password, uri)
        else:
            command = "brm backup-manager {} backup {} export uri {}"\
                .format(backup_manager, backup_name, uri)

        return self.execute_yang_action(command)

    def export_backup_key_auth(self, backup_name, sftp_server_name,
                               backup_manager="DEFAULT"):
        """
        Runs an export on a given backup manager for an existing backup.

        :param backup_name: The name of the backup to be exported.
        :param sftp_server_name: The name of the SFTP server.
        :param backup_manager: The backup manager the action will be ran on.
        :return: a progress-report id
        """

        log("Exporting backup {}".format(backup_name))
        command = "brm backup-manager {} backup {} export sftp-server-name {}"\
            .format(backup_manager, backup_name, sftp_server_name)

        return self.execute_yang_action(command)

    def import_backup(self, uri, password=None,
                      backup_manager="DEFAULT"):
        """
        Runs an import on a given backup manager from an SFTP server.

        :param uri: The path of the SFTP or HTTP server.
        :param password: The password required for the SFTP server.
        :param backup_manager: The backup manager the action will be ran on.
        :return: a progress-report id
        """

        log("Importing backup from {}".format(uri))
        if password:
            command = "brm backup-manager {} import-backup password {} uri {}"\
                .format(backup_manager, password, uri)
        else:
            command = "brm backup-manager {} import-backup uri {}"\
                .format(backup_manager, uri)

        return self.execute_yang_action(command)

    def import_backup_key_auth(self, sftp_server_name, backup_path,
                               backup_manager="DEFAULT"):
        """
        Runs an import on a given backup manager from an SFTP server.

        :param sftp_server_name: The name of the SFTP server.
        :param backup_path: The backup_path of the SFTP server.
        :param backup_manager: The backup manager the action will be ran on.
        :return: a progress-report id
        """

        log("Importing backup from {}".format(sftp_server_name))
        command = "brm backup-manager {} import-backup sftp-server-name"\
            " {} backup-path {}".format(backup_manager, sftp_server_name,
                                        backup_path)

        return self.execute_yang_action(command)

    def delete_backup(self, backup_name, backup_manager="DEFAULT"):
        """
        Runs a delete on a given backup manager for an existing backup.

        :param backup_name: The name of the backup to be deleted.
        :param backup_manager: The backup manager the action will be ran on.
        :return: a progress-report id
        """

        log("Deleting backup {}".format(backup_name))
        command = "brm backup-manager {} delete-backup name {}"\
            .format(backup_manager, backup_name)

        return self.execute_yang_action(command)

    def create_backup(self, backup_name, backup_manager="DEFAULT"):
        """
        Creates a new backup on a given backup manager.

        :param backup_name: The name of the backup to be created.
        :param backup_manager: The backup manager the action will be ran on.
        :return: a progress-report id
        """

        log("Creating backup {}".format(backup_name))
        command = "brm backup-manager {} create-backup name {}"\
            .format(backup_manager, backup_name)

        return self.execute_yang_action(command)

    def restore_backup(self, backup_name, backup_manager="DEFAULT"):
        """
        Restores a backup on a given backup manager.

        :param backup_name: The name of the backup to be restored.
        :param backup_manager: The backup manager the action will be ran on.
        :return: a progress-report id
        """

        log("Restoring backup {}".format(backup_name))
        command = "brm backup-manager {} backup {} restore" \
            .format(backup_manager, backup_name)

        return self.execute_yang_action(command)

    def backup_manager_has_backup(self, backup_name, backup_manager="DEFAULT"):
        """
        Checks if a backup is present on a given backup manager.

        :param backup_name: The name of the backup to be found.
        :param backup_manager: The backup manager the command will be ran on.
        :return: boolean indicating whether the backup is present or not
        """

        log("Verifying backup-manager {} has backup {}".format(
            backup_manager, backup_name))
        command = "show brm backup-manager {} backup {}".format(backup_manager,
                                                                backup_name)
        expected_response_substring = backup_name

        try:
            self.operational_command(command, expected_response_substring)
            return True
        except ValueError:
            return False

    def get_backup_status(self, backup_name, backup_manager="DEFAULT"):
        """
        Gets the status of a backup on a given backup manager.

        :param backup_name: The name of the backup to get the status of.
        :param backup_manager: The backup manager the command will be ran on.
        :return: the status of a backup
        """

        log("Fetching status of backup {}".format(backup_name))
        command = "show brm backup-manager {} backup {} status"\
            .format(backup_manager, backup_name)
        expected_response_substring = "status"

        return self.extract_response_from_command(command,
                                                  expected_response_substring)

    def wait_until_progress_report_is_complete(self, progress_report_id,
                                               action_name,
                                               scope="DEFAULT",
                                               expected_result="SUCCESS",
                                               backup_name=None,
                                               agents=None):
        """
        Waits till a progress-report is successful.

        :param progress_report_id: id of progress-report
        :param action_name: name of action
        :param scope: The scope to be used for checking the progress-report
        :param backup_name: backup who owns the progress-report
        :param agents: agents expected in result-info
        """
        if action_name in \
                ["CREATE_BACKUP", "IMPORT", "DELETE_BACKUP"]:
            backup_name = None

        progress_resport_exists = \
            self.check_progress_report_exists(scope, progress_report_id,
                                              backup_name)
        assert_equal(progress_resport_exists, True), \
            "Progress report does not exist after 3 retries"

        get_attribute = \
            self.__get_progress_report_attribute_function(progress_report_id,
                                                          scope, backup_name)
        time.sleep(1)
        assert_equal(
            get_attribute("action-name"), action_name)

        count = 0
        is_percent_change = False
        while count < 300:
            time.sleep(1)
            # Verify progress percentage doesnt jump from 0 to 100
            if 0 < int(get_attribute("progress-percentage")) < 100:
                is_percent_change = True

            # Sleep to allow for possible new CMM update push
            time.sleep(1)

            if get_attribute("state") == "finished":
                log("Progress-Report has finished")
                break

            count = count + 2

        assert count < 300, "State is not finished."
        # 1 second gap between each cmyp command
        time.sleep(1)
        if expected_result == "SUCCESS":
            assert_equal(get_attribute("result"), "success")
            time.sleep(1)
            assert_equal(get_attribute("progress-percentage"), "100")
            # Excluding IMPORT until ADPPRG-138169 is implemented
            # if action_name in ["IMPORT", "EXPORT"]:
            if action_name in ["EXPORT"]:
                assert is_percent_change is True, \
                    "Expected CMM progress percent to update incrementally" + \
                    " but it did not"

            try:
                # Check if result-info is present
                result_info = get_attribute("result-info")
            except:
                log("result-info is not present in progress report")
            else:
                # Check if the agents are provided for check
                if agents is not None:
                    # Check if the expected agents are present
                    for agent in agents:
                        assert agent["AgentId"] in result_info, \
                            "Expected agents are not present in result-info"
                    # Check if the expected stages are present
                    assert "PREPERATION" and "EXECUTION" and "POST_ACTIONS" \
                        in result_info, \
                        "Expected stages are not present in result-info"
                else:
                    # Added log in order to check any tests in test suites that
                    # contains result-info but has not provided the agents for
                    # the check
                    log("No agents provided, test needs updating")
        else:
            assert_equal(get_attribute("result"), "failure")

    def get_scheduled_action_id(self, backup_manager="DEFAULT"):
        """
        Gets a scheduler's last progress-report's id

        :param backup_manager: The scope to be used for checking the
        progress-report
        :return: progress report
        """

        log("Fetching last ran scheduled action in backup manager {}"
            .format(backup_manager))
        command = "show brm backup-manager {} scheduler progress-report" \
            .format(backup_manager)

        return self.extract_response_from_command(command, "progress-report")

    def get_progress_report_attribute(self, progress_report_id, scope,
                                      backup_name, attribute):
        """
        Gets a progress-report's attribute

        :param progress_report_id: id of progress-report
        :param scope: The scope to be used for checking the progress-report
        :param backup_name: backup who owns the progress-report
        :param attribute: to be returned
        :return: value of attribute
        """

        log("Fetching {} of progress report {}"
            .format(attribute, progress_report_id))
        command = "show brm backup-manager {}".format(scope)
        if backup_name:
            command = "{} backup {}".format(command, backup_name)
        command = "{} progress-report {} {}".format(command,
                                                    progress_report_id,
                                                    attribute)

        return self.extract_response_from_command(command, attribute)

    def __get_progress_report_attribute_function(self, progress_report_id,
                                                 scope, backup_name):
        """
        Currying function to get an attribute from a specific progress-report

        :param progress_report_id: id of progress-report
        :param scope: The scope to be used for checking the progress-report
        :param backup_name: backup who owns the progress-report
        :return: function to get an attribute from a specific progress-report
        """

        log("Fetching progress report {}".format(progress_report_id))
        return lambda attribute: \
            self.get_progress_report_attribute(progress_report_id, scope,
                                               backup_name, attribute)

    def check_progress_report_exists(self, scope, progress_report_id,
                                     backup_name=None):
        """
        Checks if the progress report exists. If the progress report does
        exist then progress_report_exists is returned as True.
        If the progress report does not exist yet the method waits 1 second
        and retries. If progress report does not exist after 3 retry attempts
        the method returns progress_report_exists as False.

        :param scope: The scope to be used for checking the progress-report
        :param progress_report_id: id of progress-report
        :param backup_name: backup who owns the progress-report
        :return: progress_report_exists True if the progress report
                 exists, False otherwise
        """

        retry = 3
        attempt = 1
        progress_report_exists = False
        for i in range(retry):
            try:
                log("Fetching progress report")
                command = "show brm backup-manager {}".format(scope)
                if backup_name:
                    command = "{} backup {}".format(command, backup_name)

                command = "{} progress-report {}".format(command,
                                                         progress_report_id)
                response = self.operational_command(
                    command, "progress-report {}".format(progress_report_id))

                if "progress-report {}".format(progress_report_id) in \
                        response:
                    log("Progress Report exists")
                    progress_report_exists = True
                    return progress_report_exists

            except ValueError as e:
                if i < retry - 1:
                    log("Progress report does not exist, Retrying")
                    log("Retry attempt: {}".format(attempt))
                    attempt = attempt + 1
                    time.sleep(1)
                    continue
                else:
                    log("Progress Report does not exist")
                    return progress_report_exists

    def get_housekeeping_configuration(self, backup_manager="DEFAULT"):
        """
        Gets the current values for auto-delete and max-stored-manual-backups.

        :param backup_manager: The backup manager the command will be ran on.
        :return: the values contained in max-stored-manual-backups
        and auto-delete.
        """

        log("Fetching housekeeping config of backup manager {}"
            .format(backup_manager))
        command = "show running-config brm backup-manager {} housekeeping"\
            .format(backup_manager)

        response = self.operational_command(command, "housekeeping")

        max_backups = self.extract_params_from_response(
            response, "max-stored-manual-backups")

        status = self.extract_params_from_response(
            response, "auto-delete")

        return max_backups, status

    def set_housekeeping_configuration(self, auto_delete, max_backups,
                                       backup_manager="DEFAULT"):
        """
        Configures the auto-delete and max-stored-manual-backups fields.
        Must be executed in config mode.

        :param auto_delete: auto-delete value to be set.
        :param max_backups: max-stored-manual-backups value to be set.
        :param backup_manager: The backup manager the command will be ran on.
        """
        self.config_command(self.get_housekeeping_configuration_command(
            auto_delete, max_backups, backup_manager))
        # Wait for housekeeping action to start in Orchestrator
        time.sleep(2)

    @staticmethod
    def get_housekeeping_configuration_command(auto_delete, max_backups,
                                               backup_manager="DEFAULT"):
        """
        Creates a housekeeping configuration command with the given
        auto_delete and max_stored_manual_backups values

        :param auto_delete: auto-delete value to be set.
        :param max_backups: max-stored-manual-backups value to be set.
        :param backup_manager: The backup manager the command will be ran on.
        """

        log("Setting housekeeping configuration of backup manager {}"
            .format(backup_manager))
        command = ("brm backup-manager {} housekeeping auto-delete {} "
                   "max-stored-manual-backups {}")\
            .format(backup_manager, auto_delete, max_backups)
        return command

    def get_scheduler_configuration(self, backup_manager="DEFAULT",
                                    sftp_credentials="auto-export-uri"):
        """
        Gets the current values for admin-state, auto-export and
        scheduled-backup-name.

        :param backup_manager: The backup manager the command will be ran on.
        :param sftp_credentials: Updated SFTP credentials, options are either
        "auto-export-uri" or "sftp-server-name".
        :return: the values contained in admin-state, auto-export,
        scheduled-backup-name and auto-export-uri.
        """

        log("Fetching scheduler configuration of backup manager {}"
            .format(backup_manager))
        command = "show running-config brm backup-manager {} scheduler"\
            .format(backup_manager)

        response = self.operational_command(command, "scheduler")

        admin_state = self.extract_params_from_response(
            response, "admin-state")

        auto_export = self.extract_params_from_response(
            response, "auto-export ")

        backup_name = self.extract_params_from_response(
            response, "scheduled-backup-name")

        sftp_return = ""

        if sftp_credentials == "auto-export-uri" and auto_export == "enabled":
            auto_export_uri = self.extract_params_from_response(
                response, "auto-export-uri")

            auto_export_password = self.extract_params_from_response(
                response, "auto-export-password")
            passphrase_prefix = re.search("^000PASSPHRASE000v[0-9]{1,2}v",
                                          auto_export_password)
            assert (passphrase_prefix is not None), \
                "Expected prefix 000PASSPHRASE000v[0-9]{1,2}v " + \
                "is not found in auto-export-password: " + auto_export_password

            sftp_return = auto_export_uri

        elif sftp_credentials == "sftp-server-name" \
                and auto_export == "enabled":
            sftp_server_name = self.extract_params_from_response(
                response, "sftp-server-name")

            sftp_return = sftp_server_name

        return admin_state, auto_export, backup_name, sftp_return

    def set_scheduler_configuration(self, backup_name, admin_state="unlocked",
                                    auto_export="disabled", auto_export_uri="",
                                    auto_export_password="",
                                    sftp_server_name="",
                                    backup_manager="DEFAULT"):
        """
        Configures the admin-state and scheduled-backup-name fields.
        Must be executed in config mode.

        :param backup_name: scheduled-backup-name value to be set.
        :param admin_state: admin-state value to be set.
        :param backup_manager: The backup manager the command will be ran on.
        """

        log("Setting scheduler configuration of backup manager {}"
            .format(backup_manager))
        command = ("brm backup-manager {} scheduler admin-state {} "
                   "scheduled-backup-name {} auto-export {}")\
            .format(backup_manager, admin_state, backup_name, auto_export)

        if auto_export_uri != "" and auto_export_password != "":
            command += (" auto-export-uri {} auto-export-password {}")\
                .format(auto_export_uri, auto_export_password)

        elif sftp_server_name != "":
            command += (" sftp-server-name {}").format(sftp_server_name)

        self.config_command(command)

    def create_or_update_periodic_event(self, event_id, minutes, hours="0",
                                        backup_manager="DEFAULT",
                                        start_time=None,
                                        stop_time=None):
        """
        Creates a periodic event with given id if doesn't exist already.
        Updates a periodic event if already exists
        Must be executed in config mode.
        :param event_id: Id of the event to be created or updated.
        :param minutes: minutes value to be set.
        :param hours: hours value to be set.
        :param backup_manager: The backup manager the command will be ran on.
        :param start_time: when the periodic event will start
        :param stop_time: when the periodic event will end
        """
        log("Creating or updating periodic event {}".format(event_id))

        command = ("brm backup-manager {} scheduler periodic-event {} "
                   "hours {} minutes {}")
        if None not in (start_time, stop_time):
            command += " start-time {} stop-time {}"
            command = command.format(backup_manager, event_id, hours,
                                     minutes, start_time, stop_time)
        elif start_time is not None:
            command += " start-time {}"
            command = command.format(backup_manager, event_id, hours,
                                     minutes, start_time)
        elif stop_time is not None:
            command += " stop-time {}"
            command = command.format(backup_manager, event_id, hours,
                                     minutes, stop_time)
        else:
            command = command.format(backup_manager, event_id, hours, minutes)

        self.config_command(command)

    def generate_create_or_update_periodic_event_cmd(
            self, event_id, minutes, hours="0",
            backup_manager="DEFAULT",
            start_time=None,
            stop_time=None):
        """
        generate command for
        Creates a periodic event with given id if doesn't exist already.
        Updates a periodic event if already exists
        Must be executed in config mode.
        :param event_id: Id of the event to be created or updated.
        :param minutes: minutes value to be set.
        :param hours: hours value to be set.
        :param backup_manager: The backup manager the command will be ran on.
        :param start_time: when the periodic event will start
        :param stop_time: when the periodic event will end
        :return the command to be executed in CMYP
        """
        log("generate command for creating or updating periodic event {}"
            .format(event_id))

        command = ("brm backup-manager {} scheduler periodic-event {} "
                   "hours {} minutes {}")
        if None not in (start_time, stop_time):
            command += " start-time {} stop-time {}"
            command = command.format(backup_manager, event_id, hours,
                                     minutes, start_time, stop_time)
        elif start_time is not None:
            command += " start-time {}"
            command = command.format(backup_manager, event_id, hours,
                                     minutes, start_time)
        elif stop_time is not None:
            command += " stop-time {}"
            command = command.format(backup_manager, event_id, hours,
                                     minutes, stop_time)
        else:
            command = command.format(backup_manager, event_id, hours, minutes)
        return command

    def create_or_update_periodic_event_multi_BRM(self, event_id,
                                                  minutes, hours="0",
                                                  backup_managers=[],
                                                  start_time=None,
                                                  stop_time=None):
        """
        for each backup manager in backup_managers:
        Creates a periodic event with given id if doesn't exist already.
        Updates a periodic event if already exists
        Must be executed in config mode.
        :param event_id: Id of the event to be created or updated.
        :param minutes: minutes value to be set.
        :param hours: hours value to be set.
        :param backup_managers: The backup manager(s) the command
        will be ran on.
        :param start_time: when the periodic event will start
        :param stop_time: when the periodic event will end
        """
        log("Creating or updating periodic event {} in BRMs {}"
            .format(event_id, backup_managers))

        command = ""

        for brm in backup_managers:
            command += ("brm backup-manager {} scheduler periodic-event {} "
                        "hours {} minutes {}")
            if None not in (start_time, stop_time):
                command += " start-time {} stop-time {}"
                command = command.format(brm, event_id, hours,
                                         minutes, start_time,
                                         stop_time)
            elif start_time is not None:
                command += " start-time {}"
                command = command.format(brm, event_id, hours,
                                         minutes, start_time)
            elif stop_time is not None:
                command += " stop-time {}"
                command = command.format(brm, event_id, hours,
                                         minutes, stop_time)
            else:
                command = command.format(brm, event_id, hours, minutes)
            command += "\n"

        self.config_command(command)

    def get_periodic_event(self, event_id,
                           backup_manager="DEFAULT",
                           check_stop_time=False,
                           check_start_time=False):
        """
        Gets a periodic event with given id.

        :param event_id: Id of the event to be fetched.
        :param backup_manager: The backup manager the command will be ran on.
        :param check_stop_time: check stop_time of periodic event
        :param check_start_time: check start_time of periodic event
        :return: the values contained in minutes, hours, start-time
                 and stop-time of periodic event.
        """

        log("Fetching periodic event {}".format(event_id))
        command = ("show running-config brm backup-manager {} scheduler "
                   "periodic-event {}")\
            .format(backup_manager, event_id)

        expected_response_substring =\
            "periodic-event {}".format(event_id)

        response = self.operational_command(
            command, expected_response_substring)

        hours = self.extract_params_from_response(response, "hours")

        minutes = self.extract_params_from_response(response, "minutes")

        if check_stop_time and check_start_time:
            start_time = self.extract_params_from_response(response,
                                                           "start-time")
            stop_time = self.extract_params_from_response(response,
                                                          "stop-time")
            result = (minutes, hours, start_time, stop_time)
        else:
            result = (minutes, hours)

        log("Returned Periodic Event: {}".format(result))
        return result

    def verify_periodic_event_not_exist_cmyp(
            self, event_id,
            backup_manager="DEFAULT"):
        """
        Gets a periodic event with given id. verify it does not exist
        in cmyp

        :param event_id: Id of the event to be fetched.
        :param backup_manager: The backup manager the command will be ran on.
        :return: True if the periodic event does not exist, otherwise False.
        """

        log("Fetching periodic event {}".format(event_id))
        command = ("show running-config brm backup-manager {} scheduler "
                   "periodic-event {}")\
            .format(backup_manager, event_id)

        expected_response_substring = "element does not exist"

        response = self.operational_command(
            command, expected_response_substring)

        if response:
            return True
        else:
            return False

    def verify_no_periodic_event_exist_cmyp(
            self, backup_manager="DEFAULT"):
        """
        verify no periodic event exist in BRM

        :param backup_manager: The backup manager the
        command will be ran on.
        :return: True if no periodic event exist, otherwise False.
        """

        log("check no periodic event exist in backup managers {}"
            .format(backup_manager))
        command = ("show running-config brm backup-manager {} scheduler "
                   "periodic-event")\
            .format(backup_manager)

        expected_response_substring = "No entries found"

        response = self.operational_command(
            command, expected_response_substring)

        if response:
            return True
        else:
            return False

    def delete_periodic_event(self, event_id,
                              backup_manager="DEFAULT"):
        """
        Delete a periodic event with given id.
        Must be executed in config mode.

        :param event_id: Id of the event to be deleted.
        :param backup_manager: The backup manager the command will be ran on.
        """

        log("Deleting periodic event {}".format(event_id))
        command = "no brm backup-manager {} scheduler periodic-event {}"\
            .format(backup_manager, event_id)

        self.config_command(command)

    def delete_periodic_event_multi_BRM(self, event_id,
                                        backup_managers=[]):
        """
        Delete periodic events with given id and BRMs.
        Must be executed in config mode.

        :param event_id: Id of the event to be deleted.
        :param backup_manager: The list of backup managers the
        command will be ran on.
        """

        log("Deleting periodic event {} from {}".format(
            event_id, backup_managers))
        command = ""
        for brm in backup_managers:
            command += "no brm backup-manager {} scheduler periodic-event {}"\
                .format(brm, event_id)
            command += "\n"

        self.config_command(command)

    def delete_all_periodic_event_multi_BRM(self,
                                            backup_managers=[]):
        """
        Delete all periodic event(s) in BRMs.
        Must be executed in config mode.

        :param backup_manager: The list of backup managers the
        command will be ran on.
        """

        log("Deleting all periodic event(s) from {}".format(
            backup_managers))
        command = ""
        for brm in backup_managers:
            command += "no brm backup-manager {} scheduler periodic-event"\
                .format(brm)
            command += "\n"

        self.config_command(command)

    def create_remove_periodic_event_multi_BRM(self,
                                               event_ids=[],
                                               operations=[],
                                               backup_managers=[]):
        """
        Operate (create or remove) periodic events with given id and BRMs.
        Must be executed in config mode.

        :param event_ids: Id(s) of the event to be operated.
        :param operations: The list of operations the
        command will be ran on.
        :param backup_manager: The list of backup managers the
        command will be ran on.

        all params should be of same length, and operations should match
        the event_ids with operation in backup_manager at same index.
        """

        log("Operating periodic event in backup managers")
        command = ""
        for event_id, operation, backup_manager in \
                zip(event_ids, operations, backup_managers):
            if operation == "create":
                command += self.generate_create_or_update_periodic_event_cmd(
                    event_id, "1", "1", backup_manager)
            elif operation == "remove":
                command +=\
                        "no brm backup-manager {} scheduler periodic-event {}"\
                        .format(backup_manager, event_id)
            command += "\n"

        self.config_command(command)

    def remove_sftp_server_from_scheduler(self, backup_manager="DEFAULT"):
        """
        Remove a sftp server in scheduler with given name.
        Must be executed in config mode.

        :param backup_manager: The backup manager the command will be ran on.
        """

        log("Remove sftp server from scheduler")
        command = "no brm backup-manager {} scheduler sftp-server-name"\
            .format(backup_manager)

        self.config_command(command)

    def remove_sftp_server(self, sftp_server_name,
                           backup_manager="DEFAULT"):
        """
        Remove a sftp server with given name.
        Must be executed in config mode.

        :param sftp_server_name: name of the sftp server to be removed.
        :param backup_manager: The backup manager the command will be ran on.
        """

        log("Remove sftp server {}".format(sftp_server_name))
        command = "no brm backup-manager {} sftp-server {}"\
            .format(backup_manager, sftp_server_name)

        self.config_command(command)

    def prepare_cmyp(self, cmyp_psw, cmyp_user,
                     user_idnumber="710",
                     user_role="system-admin"):
        """
        Prepare the cmyp user and connect to the cmyp server

        :param cmyp_psw: the cmyp client password for user creation
        :param cmyp_user: the cmyp client username for user creation
        :param user_idnumber: the id number for the user. This id number should
                             be unique and within the allowed range(700 to 800)
        :param user_role: the role for the user
        """
        # Create the cmyp user in LDAP via IAM
        token_query_url = (
            'https://eric-sec-access-mgmt-http:8443/'
            'auth/realms/master/protocol/openid-connect/token')
        token_request_data = {
            'grant_type': 'password', 'username': 'adminplanb',
            'password': 'kcpwb@Planb1', 'client_id': 'admin-cli'}
        token_request_headers = {
            'content-type': 'application/x-www-form-urlencoded'}
        # Query the endpoint for the token
        token_request_response = rest_utils.rest_request(
            rest_utils.REQUESTS.post, "Retrieve IAM token",
            token_query_url, token_request_data, headers=token_request_headers,
            content_type_json=False, ca_cert=False)
        token = json.loads(token_request_response)['access_token']
        user_creation_query_url = (
            'https://eric-sec-access-mgmt-http:8443/'
            'auth/admin/realms/local-ldap3/users')
        user_creation_request_data = {
            "username": cmyp_user,
            "enabled": "true",
            "credentials": [
                {
                    "type": "password",
                    "value": cmyp_psw
                }
            ],
            "attributes": {
                "uidNumber": [user_idnumber],
                "roles": [user_role],
                "pwdReset": ["FALSE"]
            }
        }
        user_creation_headers = {'content-type': 'application/json',
                                 'Authorization': ''}
        user_creation_headers['Authorization'] = 'bearer ' + token
        # Send the create user request
        rest_utils.rest_request(
            rest_utils.REQUESTS.post, "Create cmyp user",
            user_creation_query_url, user_creation_request_data,
            headers=user_creation_headers, ca_cert=False)
        self.connect()
