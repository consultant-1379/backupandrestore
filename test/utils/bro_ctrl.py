#!/usr/bin/env python3
"""
This module handles actions and other rest calls towards the orchestrator.
"""

import json
import time
import operator
from datetime import datetime, timedelta
import requests
from retry import retry  # pylint: disable=E0611
from utilprocs import log
from bro_utils import (assert_equal,
                       calculate_time_in_timezone,
                       remove_trailing_zeros,
                       assert_time_matches_regex_eoi)
import bro_data
import rest_utils
import re

DEFAULT_SCOPE = "DEFAULT"

# Set BRO variables
BRO_NAME = "eric-ctrl-bro"
BRO_POD_NAME = "eric-ctrl-bro-0"
DATE_FORMAT = "%Y-%m-%dT%H:%M:%S"
TIME_FORMAT = "%H:%M:%S"


class BroCtrlClient:
    """
    This class provides methods to handle actions and other rest calls towards
    the orchestrator.
    """

    def __init__(self, ca_cert_path=None):

        self.ca_cert_path = ca_cert_path
        if self.ca_cert_path:
            protocol = "https"
            self.base_url = "{}://eric-ctrl-bro:7002/v1".format(protocol)
            self.v2_base_url = "{}://eric-ctrl-bro:7002/v2/".format(protocol)
            self.v3_base_url = "{}://eric-ctrl-bro:7002/v3".format(protocol)
        else:
            protocol = "http"
            port = 7001

        self.base_url = "{}://eric-ctrl-bro:{}/v1".format(protocol, port)
        self.v2_base_url = "{}://eric-ctrl-bro:{}/v2/".format(protocol, port)
        self.v3_base_url = "{}://eric-ctrl-bro:{}/v3".format(protocol, port)
        v4_path = "backup-restore/v4"
        self.v4_base_url = "{}://eric-ctrl-bro:{}/{}".format(protocol, port,
                                                             v4_path)

        self.v2_create_backup_url = self.v2_base_url \
            + "ericsson-brm:brm::backup-manager::create-backup"
        self.v2_delete_backup_url = self.v2_base_url \
            + "ericsson-brm:brm::backup-manager::delete-backup"
        self.v2_restore_url = self.v2_base_url \
            + "ericsson-brm:brm::backup-manager::backup::restore"

    def get_base_url(self, rest_version="v1"):
        """
        Get the base url for the given rest version
        """
        return {
            "v1": self.base_url,
            "v2": self.v2_base_url,
            "v3": self.v3_base_url,
            "v4": self.v4_base_url
        }[rest_version]

    def verify_orchestrator_health(self, rest_version="v1"):
        """
        Uses the Orchestrator's REST interface to determine its status

        :return: dict of the orchestrator status info
        """

        health_url = "{}/health".format(self.get_base_url(rest_version))

        log("Get Orchestrator Health: {}".format(health_url))
        count = 0
        while True:
            try:
                response = requests.get(health_url, timeout=5,
                                        verify=self.ca_cert_path)
                log("Got response {0}".format(response.text))

                # Raise if exception other than networking
                response.raise_for_status()
                break

            except requests.ConnectionError as err:
                # log the networking exception
                log("Received ConnectionError running health check: {}"
                    .format(err))
                if count > 30:
                    raise Exception(
                        "Continuous Network Errors on the Cluster") from err
                count = count + 1
                time.sleep(1)

        # return the response as a dictionary
        return response.json()

    def get_housekeeping_config(self, backup_manager_name=DEFAULT_SCOPE,
                                rest_version="v3"):
        """
        Retrieves housekeeping configuration for the backup manager

        :param backup_manager_name: the name of the backup manager to get

        :param rest_version: BRO REST API version

        :return: Response to REST GET request
        """

        raise_for_status = True

        if rest_version == "v1":
            raise_for_status = False

        housekeeper_url = self.get_backupmanager_url(
            backup_manager_name, rest_version) \
            + {"v1": "/housekeeper", "v3": "/housekeeping",
               "v4": "/configuration/housekeeping"}[rest_version]

        return rest_utils.rest_request(rest_utils.REQUESTS.get,
                                       "Get housekeeping info",
                                       housekeeper_url,
                                       ca_cert=self.ca_cert_path,
                                       raise_for_status=raise_for_status)

    def update_housekeeping_config(self, auto_delete=None,
                                   max_stored_backups=None,
                                   backup_manager_name=DEFAULT_SCOPE,
                                   rest_version="v3",
                                   rest_method=rest_utils.REQUESTS.post):
        """
        Updates housekeeping configuration for the backup manager.

        :param auto_delete: indicates if housekeeping is enabled for the
                            backup manager

        :param max_stored_backups: indicates the maximum number of backups
                                   that can be stored in the backup manager

        :param backup_manager_name: the name of the backup manager to update

        :param rest_version: BRO REST API version

        :param rest_method: POST/PUT/PATCH

        :return: Response to REST POST/PUT/PATCH request
        """
        raise_for_status = True

        if rest_version == "v1":
            raise_for_status = False

        housekeeper_url = self.get_backupmanager_url(
            backup_manager_name, rest_version) \
            + {"v1": "/housekeeper", "v3": "/housekeeping",
               "v4": "/configuration/housekeeping"}[rest_version]

        update_hk = {}
        if rest_version == "v4":
            if auto_delete is not None:
                update_hk.update({"autoDelete": auto_delete})
            if max_stored_backups is not None:
                update_hk.update({"maxStoredBackups": max_stored_backups})
        else:
            update_hk = {"auto-delete": auto_delete,
                         "max-stored-manual-backups": max_stored_backups}

        hse_log = "Update housekeeping to {}".format(update_hk)

        output = rest_utils.rest_request(rest_method,
                                         hse_log,
                                         housekeeper_url, body=update_hk,
                                         ca_cert=self.ca_cert_path,
                                         raise_for_status=raise_for_status)
        out = ""
        if rest_version == "v4":
            actions = self.get_actions(backup_manager_name)
            out = actions[-1]
            if out != "":
                self.wait_for_action_complete(out["id"],
                                              "HOUSEKEEPING", None,
                                              backup_manager_name,
                                              rest_version="v3")
        else:
            out = json.loads(output)

        if rest_version == "v3":
            action_id = out["id"]
            # Follow the create backup progress
            self.wait_for_action_complete(action_id,
                                          "HOUSEKEEPING", None,
                                          backup_manager_name,
                                          rest_version=rest_version)
        return out

    def get_backup_manager(self, backup_manager_name=DEFAULT_SCOPE,
                           rest_version="v3"):
        """
        Gets a backup manager

        :param backup_manager_name: the name of the backup manager to get
        """
        log("Retrieving backup manager {}".format(backup_manager_name))

        url = self.get_backupmanager_url(backup_manager_name, rest_version)
        return rest_utils.get_resource(url, ca_cert=self.ca_cert_path)

    def update_backup_manager(self, request_body,
                              backup_manager_name=DEFAULT_SCOPE,
                              rest_version="v1"):
        """
        Updates a backup manager

        :param request_body: the body of the request for updating the backup
                             manager
        :param backup_manager_name: the name of the backup manager to update
        """
        log("Updating backup manager {} with the following fields: {}"
            .format(backup_manager_name, request_body))
        url = self.get_backupmanager_url(backup_manager_name, rest_version)

        rest_utils.update_resource(url, request_body,
                                   ca_cert=self.ca_cert_path)

    def get_registered_agents_of_brm(self, scope=DEFAULT_SCOPE):
        """
        Fetches the agents registered to a backup manager
        : param scope: the name of the backup manager
        : return a list of registered agentIds
        """
        log("Retrieving agents of backup manager {}".format(scope))
        url = "{}/{}".format(self.get_backupmanager_url(scope, "v4"), "agents")
        return rest_utils.get_resource(url, ca_cert=self.ca_cert_path)

    def get_registered_agents(self, rest_version="v3"):
        """
        Fetches the agents registered in the orchestrator

        :return: a list of the registered AgentIds
        """
        health_info = self.verify_orchestrator_health(rest_version)
        return health_info['registeredAgents']

    def get_availability_status(self, rest_version="v3"):
        """
        Fetches the availability status of the orchestrator

        :return: a string representing availability of Orchestrator
        """
        return self.verify_orchestrator_health(rest_version)['availability']

    def get_action_url(self, scope=DEFAULT_SCOPE, rest_version="v3"):
        """
        :param scope: the scope to be included in the URL
        :param rest_version: the BRO REST API version
        :return action_url: the url to be used to call an action including
                            scope
        """
        return "{}/{}".format(
            self.get_backupmanager_url(scope, rest_version),
            {"v1": "action", "v3": "actions"}[rest_version]
        )

    def get_backup_url(self, scope=DEFAULT_SCOPE, rest_version="v3"):
        """
        :param scope: the scope to be included in the URL
        :param rest_version: the BRO REST API version
        :return backup_url: the url to be used to work with a backup
        """
        return "{}/{}".format(
            self.get_backupmanager_url(scope, rest_version),
            {"v1": "backup", "v3": "backups", "v4": "backups"}[rest_version]
        )

    def get_specific_backup_url(self, backup_id, scope=DEFAULT_SCOPE,
                                rest_version="v3"):
        """
        :param backup_id: the name of the backup
        :param scope: the scope to be included in the URL
        :param rest_version: the BRO REST API version
        :return backup_url: the url to be used to work with a backup
        """
        return "{}/{}".format(
            self.get_backup_url(scope, rest_version), backup_id)

    def get_backupmanager_url(self, scope=DEFAULT_SCOPE, rest_version="v3"):
        """
        :param scope: the scope to be included in the URL
        :return backup_url: the url to be used to work with a backup-manager
        """
        base_backup_manager_url = "{}/{}"\
            .format(
                self.get_base_url(rest_version),
                {
                    "v1": "backup-manager",
                    "v3": "backup-managers",
                    "v4": "backup-managers"
                }[rest_version]
            )
        return "{}/{}".format(base_backup_manager_url, scope)

    def get_actions(self, scope=DEFAULT_SCOPE, rest_version="v3"):
        """
        Gets all actions from the given scope/backup manager
        :param scope: The scope to be used for getting an action
        :return list of actions
        """
        action_url = self.get_action_url(scope, rest_version)

        out = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                      "Get all actions for {} backup-manager"
                                      .format(scope),
                                      action_url,
                                      ca_cert=self.ca_cert_path)
        return out["actions"]

    def get_action(self, action_id, action_type,
                   scope=DEFAULT_SCOPE, rest_version="v3"):
        """
        Returns the action of specified action_id
        :param action_id: id of an action
        :param action_type: type of action
        :param scope: The scope to be used for checking an action

        :return: Action of specified action_id
        """
        action_url = self.get_action_url(scope, rest_version)
        self.check_for_specific_action_id(
            action_id,
            action_type,
            scope,
            rest_version)
        action = "{}/{}".format(action_url, action_id)
        out = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                      "Get action progress",
                                      action,
                                      ca_cert=self.ca_cert_path)
        return out

    def get_backup(self, backup_name, scope=DEFAULT_SCOPE, rest_version="v3"):
        """
        Gets one backup with a given name from a given scope

        :param backup_name: backup name
        :param scope: scope of the backup

        :returns: A backup as a dict
        """
        log("Retrieving backup {} from backup manager {}"
            .format(backup_name, scope))
        backup_url = "{}/{}".format(
            self.get_backup_url(scope, rest_version),
            backup_name)
        return rest_utils.get_resource(backup_url, ca_cert=self.ca_cert_path)

    def get_backups(self, scope=DEFAULT_SCOPE, rest_version="v3"):
        """
        Gets all backups from the given scope/backup manager
        :param scope: The scope to be used for getting an action
        :return list of backups
        """
        backup_url = self.get_backup_url(scope, rest_version)

        out = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                      "Get all backups for {} backup-manager"
                                      .format(scope),
                                      backup_url,
                                      ca_cert=self.ca_cert_path)
        return out["backups"]

    def update_backup(self, backup_name,
                      label, scope=DEFAULT_SCOPE, rest_version="v1"):
        """
        Updates a backup by adding a label

        :param backup_name: backup name
        :param label: the updated label value
        :param scope: scope of the backup
        """
        log("Updating backup {} from backup manager {} with label {}"
            .format(backup_name, scope, label))
        backup_url = "{}/{}".format(
            self.get_backup_url(scope, rest_version),
            backup_name)
        body = {"userLabel": label}
        rest_utils.update_resource(backup_url, body, ca_cert=self.ca_cert_path)

    def get_all_backup_managers(self, rest_version="v3"):
        """
        Gets all backup-managers in the Orchestrator.

        :return: a list of backup manager ids
        """
        # Get all backup-managers
        backup_manager_base_url = "/".join(
            self.get_backupmanager_url(
                DEFAULT_SCOPE,
                rest_version).split("/")[:-1])
        out = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                      "Get all backup-managers",
                                      backup_manager_base_url,
                                      ca_cert=self.ca_cert_path)
        ids = []
        for backupmanager in out["backupManagers"]:
            ids.append(backupmanager["id"])
        return ids

    @retry(AssertionError, tries=6, delay=10.0)
    def check_backup_manager_is_created(self, scope=DEFAULT_SCOPE,
                                        expected_number_of_managers=1,
                                        rest_version="v1"):
        """
        Verifies that a backup-manager is created for a given scope.
        :param scope: the scope to check a backup manager is available for
        :param expected_number_of_managers: The total number of expected
                                            backup managers.
        """

        log("Checking backup-manager is created for scope {}".format(scope))

        # Get all backup-managers
        backup_manager_base_url = "/".join(
            self.get_backupmanager_url(
                DEFAULT_SCOPE,
                rest_version).split("/")[:-1])

        out = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                      "Get all backup-managers",
                                      backup_manager_base_url,
                                      ca_cert=self.ca_cert_path)

        number_of_found_backup_managers = len(out["backupManagers"])
        log("Found {} backup-manager(s) now running checks"
            .format(number_of_found_backup_managers))
        assert number_of_found_backup_managers == \
            expected_number_of_managers,\
            "The expected number of backup managers was not found, expected \
             {} but was {}".format(expected_number_of_managers,
                                   number_of_found_backup_managers)

        # Check housekeeping metadata is created
        # Hard coded to use v3 because v1 housekeeping isn't implemented
        housekeeper_url = self.get_backupmanager_url(scope, "v3") + \
            "/housekeeping"

        housekeeping = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                               "Get housekeeping info",
                                               housekeeper_url,
                                               ca_cert=self.ca_cert_path)

        assert {housekeeping["auto-delete"], housekeeping[
            "max-stored-manual-backups"]} == \
            {"enabled", 1}, "Housekeeping metadata was not" \
            " set at {} brm creation".format(scope)

        manager_ids = []
        for backup_manager in out["backupManagers"]:
            manager_ids.append(backup_manager["id"])

        assert scope in manager_ids,\
            "{} backup-manager not created".format(scope)

    def create_backup(self, backup_name, agent_list, namespace,
                      scope=DEFAULT_SCOPE, expected_result="SUCCESS",
                      failure_info=None, test_agent=True,
                      rest_version="v1"):
        """
        Creates a backup, verifying the content if test agents are being used

        :param backup_name: name of backup
        :param agent_list: list of agents in the backup.
                           Each agent is represented by a dictionary
        :param namespace: namespace where orchestrator pod is deployed
        :param scope: name of the scope
        :param expected_result: indicates if backup is expected to be
                                successful
        :param failure_info: string or substring expected in the additionalInfo
                             of a failed action
        :param test_agent: indicates if test agents are participating in backup
        :param rest_version: the BRO REST API version
        :returns action id of the backup action
        """
        log("Creating backup {}".format(backup_name))

        # Create an action for CREATE_BACKUP
        create_payload = {"backupName": backup_name}

        out = self.create_action(
            "CREATE_BACKUP",
            create_payload,
            scope,
            rest_version)
        action_id = out["id"]

        # Follow the backup progress
        self.wait_for_action_complete(action_id, "CREATE_BACKUP",
                                      backup_name, scope, expected_result,
                                      failure_info,
                                      rest_version=rest_version)

        if expected_result == "SUCCESS":
            self.verify_backup_is_taken(backup_name,
                                        agent_list,
                                        namespace,
                                        scope,
                                        test_agent,
                                        rest_version)

        return action_id

    def v4_create_backup(self, backup_name, agent_list, namespace,
                         scope=DEFAULT_SCOPE, expected_result="SUCCESS",
                         failure_info=None, test_agent=True,
                         wait_for_action_complete=True):
        """
        Creates a backup, verifying the content if test agents are being used

        :param backup_name: name of backup
        :param agent_list: list of agents in the backup.
                           Each agent is represented by a dictionary
        :param namespace: namespace where orchestrator pod is deployed
        :param scope: name of the scope
        :param expected_result: indicates if backup is expected to be
                                successful
        :param failure_info: string or substring expected in the additionalInfo
                             of a failed action
        :param test_agent: indicates if test agents are participating in backup
        :returns action id of the backup action
        """
        log("Creating backup {}".format(backup_name))
        action_payload = {"name": backup_name}
        action_url = self.get_backup_url(scope, rest_version="v4")
        out = self.post_request("CREATE_BACKUP", action_url, action_payload)
        action_id = out["id"]
        # Follow the backup progress
        if wait_for_action_complete:
            self.wait_for_action_complete(action_id, "CREATE_BACKUP",
                                          backup_name, scope, expected_result,
                                          failure_info,
                                          rest_version="v3")

        if expected_result == "SUCCESS" and wait_for_action_complete:
            self.verify_backup_is_taken(backup_name,
                                        agent_list,
                                        namespace,
                                        scope,
                                        test_agent,
                                        "v4")
        return action_id

    def v4_restore_backup(self, backup_name, agent_list, namespace,
                          scope=DEFAULT_SCOPE, expected_result="SUCCESS",
                          failure_info=None, test_agent=True,
                          validate_checksum=True,
                          wait_for_action_complete=True):
        """
        Create a Restore via the v4 API, verifying the content if test
        agents are being used
        :param backup_name: name of backup
        :param agent_list: list of agents in the backup.
                           Each agent is represented by a dictionary
        :param namespace: namespace where orchestrator pod is deployed
        :param scope: name of the scope
        :param expected_result: indicates if restore is expected to be
                                successful
        :param failure_info: string or substring expected in the additionalInfo
                             of a failed action
        :param test_agent: indicates if test agents are participating in
                           restore
        :returns action id of the restore action
        """
        log("Restoring backup via v4 API: {}".format(backup_name))
        action_url = self.get_backup_url(scope, rest_version="v4")
        action_url += "/" + backup_name + "/restores"
        res = self.post_request("V4_RESTORE", action_url, None)
        action_id = res["id"]
        if wait_for_action_complete:
            self.wait_for_action_complete(action_id, "RESTORE",
                                          backup_name, scope, expected_result,
                                          failure_info,
                                          rest_version="v3")

        # Verify file transfer only if test agents are participating in restore
        if test_agent and wait_for_action_complete:
            # Verify restore files
            bro_data.verify_file_transfer(backup_name, agent_list,
                                          "restore", namespace,
                                          scope,
                                          validate_checksum=validate_checksum)
        return action_id

    def v4_get_restores(self, backup_name,
                        scope=DEFAULT_SCOPE):
        """
        Gets all restores using the BRO v4 endpoint

        :param scope: name of the scope
        :param back_name: name of the backup
        :returns list of all restores
        """
        action_url = "{}/{}/{}/{}".format(
            self.get_backupmanager_url(scope, "v4"),
            "backups", backup_name, "restores")

        out = rest_utils.rest_request(
            rest_utils.REQUESTS.get,
            "Get all {} restores"
            .format(scope),
            action_url,
            ca_cert=self.ca_cert_path)

        return out

    def v4_get_restores_by_id(self, backup_name, restore_id,
                              scope=DEFAULT_SCOPE):
        """
        Gets restore by id by using the BRO v4 endpoint

        :param scope: name of the scope
        :param backup_name: name of the backup
        :param restore_id: id of the restore
        :returns restore by id
        """
        action_url = "{}/{}/{}/{}/{}".format(
            self.get_backupmanager_url(scope, "v4"),
            "backups", backup_name, "restores", restore_id)

        out = rest_utils.rest_request(
            rest_utils.REQUESTS.get,
            "Get restores {} by ID"
            .format(scope),
            action_url,
            ca_cert=self.ca_cert_path)

        return out

    def create_backup_yang_action(self, backup_name, agent_list, namespace,
                                  scope=DEFAULT_SCOPE,
                                  expected_result="SUCCESS",
                                  failure_info=None, test_agent=True):
        """
        Creates a backup via yang action
        Verifies the content if test agents are being used
        :param backup_name: name of backup
        :param agent_list: list of agents in the backup.
                           Each agent is represented by a dictionary
        :param namespace: namespace where orchestrator pod is deployed
        :param scope: name of the scope
        :param expected_result: indicates if backup is expected to be
                                successful
        :param failure_info: string or substring expected in the additionalInfo
                             of a failed action
        :param test_agent: indicates if test agents are participating in backup

        :returns action id of the backup action
        """

        log("Creating backup {} via yang action".format(backup_name))

        context = self.get_context(scope)

        payload = {"input": {"ericsson-brm:name": backup_name},
                   "context": context}

        response = json.loads(rest_utils.rest_request(
            rest_utils.REQUESTS.put,
            "Creating backup {} with payload {}".format(backup_name, payload),
            self.v2_create_backup_url,
            body=payload,
            ca_cert=self.ca_cert_path))
        action_id = str(response["ericsson-brm:return-value"])

        self.wait_for_action_complete(action_id, "CREATE_BACKUP",
                                      backup_name, scope, expected_result,
                                      failure_info)

        if expected_result == "SUCCESS":
            self.verify_backup_is_taken(backup_name,
                                        agent_list,
                                        namespace,
                                        scope,
                                        test_agent)
        return action_id

    def restore_backup(self, backup_name, agent_list, namespace,
                       scope=DEFAULT_SCOPE, test_agent=True,
                       rest_version="v1", validate_checksum=True,
                       owns_backup=True):
        """
        Performs restore, verifying the content if test agents are being used

        :param backup_name: name of backup
        :param agent_list: list of participating agents
        :param namespace: the namespace the pods are deployed
        :param scope: name of the scope
        :param test_agent: indicates if test agents are participating in
                        restore
        :param rest_version: the BRO REST API version
        :param validate_checksum: Validates checksum files

        :param owns_backup: indicates if specified backup manager owns backup
        :returns action id of the restore action
        """
        log("Restoring backup {}".format(backup_name))

        # Create an action for RESTORE
        restore_payload = {"backupName": backup_name}
        out = self.create_action(
            "RESTORE",
            restore_payload,
            scope,
            rest_version)
        action_id = out["id"]

        # Follow the restore progress
        self.wait_for_action_complete(action_id, "RESTORE", backup_name,
                                      scope, rest_version=rest_version)

        # Verify file transfer only if test agents are participating in restore
        if test_agent:
            # Verify restore files
            if not owns_backup:
                scope = scope.split("-")[0]
            bro_data.verify_file_transfer(backup_name, agent_list,
                                          "restore", namespace,
                                          scope,
                                          validate_checksum=validate_checksum)
        return action_id

    def restore_backup_yang_action(self, backup_name, agent_list, namespace,
                                   scope=DEFAULT_SCOPE, test_agent=True):
        """
        Performs restore using v2 endpoint
        Verifies the content if test agents are being used

        :param backup_name: name of backup
        :param agent_list: list of participating agents
        :param namespace: the namespace the pods are deployed
        :param scope: name of the scope
        :param test_agent: indicates if test agents are participating in
                        restore

        :returns action id of the restore action
        """
        log("Restoring backup {} via yang action".format(backup_name))

        context = self.get_context(scope, backup_name)

        payload = {"context": context}

        response = json.loads(rest_utils.rest_request(
            rest_utils.REQUESTS.put,
            "Restoring backup {} with payload {}".format(backup_name, payload),
            self.v2_restore_url,
            body=payload,
            ca_cert=self.ca_cert_path))

        action_id = str(response["ericsson-brm:return-value"])

        # Follow the restore progress
        self.wait_for_action_complete(action_id, "RESTORE", backup_name, scope)

        # Verify file transfer only if test agents are participating in restore
        if test_agent:
            # Verify restore files
            bro_data.verify_file_transfer(backup_name, agent_list,
                                          "restore", namespace, scope)

        return action_id

    def delete_backup(self, backup_name, namespace,
                      scope=DEFAULT_SCOPE, rest_version="v1",
                      verify_files=True):
        """
        Performs delete backup and verifies content has been deleted
        :param backup_name: name of backup
        :param namespace: namespace where orchestrator pod is deployed
        :param scope: name of the scope
        :param rest_version: the BRO REST API version
        :verify_files: Allow local verification of backups on PVC

        :returns action id of the delete action
        """

        log("Deleting backup {}".format(backup_name))

        # Create an action for DELETE_BACKUP
        delete_payload = {"backupName": backup_name}
        response = self.create_action(
            "DELETE_BACKUP",
            delete_payload,
            scope,
            rest_version)
        action_id = response["id"]

        # Follow the delete backup progress
        self.wait_for_action_complete(action_id,
                                      "DELETE_BACKUP",
                                      backup_name,
                                      scope,
                                      rest_version=rest_version)

        self.verify_backup_is_deleted(
            backup_name,
            namespace,
            scope,
            rest_version,
            verify_files)

        return action_id

    def v4_delete_backup(self, backup_name, namespace,
                         scope=DEFAULT_SCOPE):
        """
        Deletes a backup using BRO's v4 REST endpoint
        :param backup_name: the name of the backup to be deleted
        :param namespace: namespace where orchestrator pod is deployed
        :param scope: name of the scope
        """
        action_url = "{}/{}".format(self.get_backup_url(scope, "v4"),
                                    backup_name)
        rest_utils.rest_request(rest_utils.REQUESTS.delete,
                                "Deleting the backup {}".format(backup_name),
                                action_url, ca_cert=self.ca_cert_path)
        last_action_id = self.get_actions(scope, "v3")[-1]["id"]
        # Follow the delete backup progress
        self.wait_for_action_complete(last_action_id,
                                      "DELETE_BACKUP",
                                      backup_name,
                                      scope,
                                      rest_version="v3")

        self.verify_backup_is_deleted(
            backup_name,
            namespace,
            scope,
            "v4")

        return last_action_id

    def delete_backup_yang_action(self, backup_name, namespace,
                                  scope=DEFAULT_SCOPE):
        """
        Performs delete backup via yang action, verifying content has been
        deleted

        :param backup_name: name of backup
        :param namespace: namespace where orchestrator pod is deployed
        :param scope: name of the scope

        :returns action id of the delete action
        """
        log("Deleting backup {} via yang action".format(backup_name))

        context = self.get_context(scope)

        payload = {"input": {"ericsson-brm:name": backup_name},
                   "context": context}

        response = json.loads(rest_utils.rest_request(
            rest_utils.REQUESTS.put,
            "Deleting backup {} with payload {}".format(backup_name, payload),
            self.v2_delete_backup_url,
            body=payload,
            ca_cert=self.ca_cert_path))
        action_id = str(response["ericsson-brm:return-value"])

        self.wait_for_action_complete(action_id,
                                      "DELETE_BACKUP",
                                      backup_name,
                                      scope)

        self.verify_backup_is_deleted(backup_name, namespace, scope)

        return action_id

    def verify_backup_is_taken(self, backup_name, agent_list, namespace,
                               scope=DEFAULT_SCOPE, test_agent=True,
                               rest_version="v1"):
        """
        Verifies if a backup was taken
        :param backup_name: name of backup
        :param agent_list: list of agents in the backup.
                           Each agent is represented by a dictionary
        :param namespace: namespace where orchestrator pod is deployed
        :param scope: name of the scope
        :param test_agent: indicates if test agents are participating in backup
        :param rest_version: the BRO REST API version

        """
        # Verify the metadata stored in the orchestrator for the backup
        self.verify_backup_metadata(
            backup_name,
            agent_list,
            scope,
            rest_version)

        # Verify file transfer only if test agents are participating in backup
        if test_agent:
            # Verify the backup transfer
            bro_data.verify_file_transfer(backup_name, agent_list,
                                          "backup", namespace, scope)

    def backup_exists(self, backup_name,
                      scope=DEFAULT_SCOPE, rest_version="v1"):
        """
        Verifies backup exists on backup manager
        :param backup_name: name of backup
        :param scope: name of the scope
        :param rest_version: the BRO REST API version

        :returns True of backup exist, otherwise false
        """
        out = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                      "Get all backups",
                                      self.get_backup_url(scope, rest_version),
                                      ca_cert=self.ca_cert_path)
        for backup in out["backups"]:
            if backup["name"] == backup_name:
                return True
        return False

    def verify_backup_is_deleted(self, backup_name, namespace,
                                 scope=DEFAULT_SCOPE,
                                 rest_version="v1",
                                 verify_files=True):
        """
        Verifies backup has been deleted
        :param backup_name: name of backup
        :param namespace: namespace where orchestrator pod is deployed
        :param scope: name of the scope
        :param rest_version: the BRO REST API version
        :verify_files: Allow local verification of backups on PVC

        """
        out = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                      "Get all backups",
                                      self.get_backup_url(scope, rest_version),
                                      ca_cert=self.ca_cert_path)

        if rest_version == "v4":
            backups = out
        else:
            backups = out["backups"]

        backup_deleted = True
        for backup in backups:
            if backup["name"] == backup_name:
                backup_deleted = False

        assert backup_deleted, "Backup not deleted"

        # Verify that the backup data has been removed from the Orchestrator
        path = "{}/{}".format(bro_data.BACKUP_LOCATION, scope)
        if verify_files:
            files = bro_data.get_container_files(path, BRO_POD_NAME,
                                                 namespace,
                                                 container=BRO_NAME)
            assert backup_name not in files, "Backup data not deleted"

    def get_backup_manager_index(self, backup_manager_id, rest_version="v3"):
        """
        Gets index of backup manager
        :param backup_manager_id: id of backup manager
        :param rest_version: the BRO REST API version

        :return: index of backup manager
        """

        base_backup_manager_url = "/".join(
            self.get_backupmanager_url(
                DEFAULT_SCOPE,
                rest_version).split("/")[:-1])
        output = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                         "Get all backup-managers",
                                         base_backup_manager_url,
                                         ca_cert=self.ca_cert_path)
        backup_managers = output["backupManagers"]

        for index, backup_manager in enumerate(backup_managers):
            if backup_manager["id"] == backup_manager_id:
                return index

        return -1

    def get_backup_index(self, backup_manager_id,
                         backup_id, rest_version="v3"):
        """
        Gets index of backup
        :param backup_manager_id: id of backup manager
        :param backup_id: id of backup
        :param rest_version: the BRO REST API version

        :return: index of backup
        """

        backups = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                          "Get all backups",
                                          self.get_backup_url(
                                              backup_manager_id,
                                              rest_version),
                                          ca_cert=self.ca_cert_path)["backups"]

        for index, backup in enumerate(backups):
            if backup["id"] == backup_id:
                return index

        return -1

    def create_action(self, action, payload, scope=DEFAULT_SCOPE,
                      rest_version="v1", raise_for_status=True,
                      action_created=True):
        """
        Creates an action in the Orchestrator

        :param action: action name
        :param payload: action payload
        :param scope: The scope to be used for creating an action
        :param rest_version: BRO's REST API version
        :param raise_for_status: Specifies if an exception should be raised
                             if the response is a HTTP error response
        :param action_created: Specifies if an action is expected to be created
                             in either successful or failed request.
        :return: the output of the request
        """
        action_url = self.get_action_url(scope, rest_version)
        action_payload = {"action": action, "payload": payload}

        return self.post_request(action,
                                 action_url,
                                 action_payload,
                                 raise_for_status,
                                 action_created)

    def v4_get_last_task(self, scope=DEFAULT_SCOPE):
        """
        Gets the last task completed using the BRO v4 endpoint
        :param scope: name of the scope
        :returns the last task
        """
        action_url = "{}/{}".format(
            self.get_backupmanager_url(scope, "v4"), "last-task")

        out = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                      "Get last-task by {}"
                                      .format(scope),
                                      action_url,
                                      ca_cert=self.ca_cert_path)

        return out

    def v4_get_tasks(self, scope=DEFAULT_SCOPE):
        """
        Gets all tasks using the BRO v4 endpoint

        :param scope: name of the BRM
        :returns all tasks
        """

        tasks_url = "{}/{}".format(
            self.get_backupmanager_url(scope, "v4"), "tasks")

        out = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                      "Get all tasks by {}"
                                      .format(scope),
                                      tasks_url,
                                      ca_cert=self.ca_cert_path)

        return out

    def v4_get_tasks_by_id(self, task_id, scope=DEFAULT_SCOPE):
        """
        Gets task  by id by using the BRO v4 endpoint

        :param scope: name of the BRM
        :param task_id: Id of the task
        :returns the last task
        """

        tasks_url = "{}/{}/{}".format(
            self.get_backupmanager_url(scope, "v4"),
            "tasks", task_id)

        out = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                      "Get task {} by ID"
                                      .format(scope),
                                      tasks_url,
                                      ca_cert=self.ca_cert_path)

        return out

    def post_request(self, action, action_url, payload,
                     raise_for_status=True, action_created=True):
        """
        Submits a POST request of an action to the Orchestrator
        and checks if it has been created
        :param action: BRO action name
        :param action_url: The REST endpoint url
        :param payload: the body of the POST request
        :return: the output of the request
        """
        output = rest_utils.rest_request(rest_utils.REQUESTS.post,
                                         "Create {} action with paylod {}".
                                         format(action, payload),
                                         action_url, body=payload,
                                         ca_cert=self.ca_cert_path,
                                         raise_for_status=raise_for_status)

        out = json.loads(output)
        # If an action request fails before the action id is generated,
        # it didn't return an id.
        if action_created:
            assert "id" in out.keys(), \
                log("An action ID is not found on the response "
                    "Action: {} - {} : {}".format(action,
                                                  out["statusCode"],
                                                  out["message"]))
        return out

    def check_for_specific_action_id(self, action_id, action_name,
                                     scope=DEFAULT_SCOPE, rest_version="v1"):
        """
        checks if a specific action ID is present on the orchestrator

        :param action_id: id of an action
        :param action_name: name of an action
        :param scope: The scope to be used for checking an action
        :param rest_version: the BRO REST API version
        """

        actions = self.get_actions(scope, rest_version)
        found_the_expected_action = False
        for action in actions:
            if action['id'] == action_id:
                assert action['name'] == action_name
                found_the_expected_action = True
        assert found_the_expected_action

    def get_action_result(self, action_id, action_type,
                          scope=DEFAULT_SCOPE, rest_version="v3"):
        """
        Returns the current action_id result
        :param action_id: id of an action
        :param action_type: type of action
        :param scope: The scope to be used for checking an action

        :return: Current status of an action_id
        """
        out = self.get_action(action_id, action_type, scope, rest_version)
        return out["result"]

    def get_action_additionalInfo(self, action_id, action_type,
                                  scope=DEFAULT_SCOPE, rest_version="v3"):
        """
        Returns the additionalInfo of an action_id
        :param action_id: id of an action
        :param action_type: type of action
        :param scope: The scope to be used for checking an action
        :param rest_version: the BRO REST API version

        :return: additionalInfo of an action_id
        """
        out = self.get_action(action_id, action_type, scope, rest_version)
        return out["additionalInfo"]

    def wait_for_action_complete(self, action_id, action_type,
                                 backup_name,
                                 scope=DEFAULT_SCOPE,
                                 expected_result="SUCCESS",
                                 failure_info=None,
                                 rest_version="v1"):
        """
        Waits till an action is FINISHED

        :param action_id: id of an action
        :param action_type: type of action
        :param backup_name: backup name
        :param scope: The scope to be used for checking the action
        :param expected_result: The expected result of the action
        :param failure_info: string or substring expected in the additionalInfo
                             of a failed action
        :param rest_version: the BRO REST API version
        """
        action_url = self.get_action_url(scope, rest_version)

        self.check_for_specific_action_id(
            action_id,
            action_type,
            scope,
            rest_version)

        action = "{}/{}".format(action_url, action_id)
        failed_progress_report = \
            "Progress Report for the action has unexpected values: {}"
        count = 0
        out = None
        does_percent_change = False
        while count < 1000:
            out = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                          "Get action progress",
                                          action,
                                          ca_cert=self.ca_cert_path)

            if out["state"] == "FINISHED" and out["result"] != "NOT_AVAILABLE":
                # Additional guard against the second thread
                # setting progress percentage
                if "completionTime" in out:
                    log("Action has finished")
                    break

            # Verify the running action parameters
            assert out["name"] == action_type, \
                failed_progress_report.format(out["name"])

            # Actions that have backupName:
            # CREATE_BACKUP, RESTORE, EXPORT, DELETE_BACKUP
            if action_type != "HOUSEKEEPING":
                if "backupName" in out["payload"].keys():
                    assert out["payload"]["backupName"] == backup_name, \
                        failed_progress_report.format(
                            out["payload"]["backupName"])

            # Verify action progress percentage doesnt jump from 0 to 1
            if 0.0 < out["progressPercentage"] < 1.0:
                does_percent_change = True

            count = count + 1
            time.sleep(1)

        # Verify the completed action parameters
        assert out["name"] == action_type, \
            failed_progress_report.format(out["name"])
        if expected_result is not None:
            assert out["result"] == expected_result, \
                failed_progress_report.format(out["result"])
        assert out["state"] == "FINISHED", \
            failed_progress_report.format(out["state"])

        if "backupName" in out["payload"].keys():
            assert out["payload"]["backupName"] == backup_name, \
                failed_progress_report.format(out["payload"]["backupName"])

        if expected_result == "SUCCESS":
            assert out["progressPercentage"] == 1.0, \
                failed_progress_report.format(out["progressPercentage"])
            if out["name"] == "EXPORT" and out["name"] == "IMPORT":
                assert does_percent_change is True, \
                    "Expected progress report to update incrementally \
                        but it did not"
        elif expected_result is None:
            log("Action result: {}".format(out["result"]))
        else:
            assert out["progressPercentage"] < 1.0, \
                failed_progress_report.format(out["progressPercentage"])

        if failure_info:
            assert failure_info in out["additionalInfo"], \
                "Expected {} but received {}".format(failure_info,
                                                     out["additionalInfo"])

        # Additional check to ensure Orchestrator is available until
        # implementation of ADPPRG-40797
        self.wait_for_orchestrator_to_be_available()

    def wait_for_action_start(self, action_id, action_type,
                              scope=DEFAULT_SCOPE,
                              rest_version="v1"):
        """
        Waits till an action is RUNNNING

        :param action_id: id of an action
        :param action_type: type of action
        :param scope: The scope to be used for checking the action
        :param rest_version: the BRO REST API version
        """

        self.check_for_specific_action_id(
            action_id,
            action_type,
            scope,
            rest_version)

        action_url = self.get_action_url(scope, rest_version)
        action = "{}/{}".format(action_url, action_id)
        count = 0
        out = None
        while count < 300:
            out = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                          "Get action progress",
                                          action,
                                          ca_cert=self.ca_cert_path)

            if out["state"] == "RUNNING":
                break
            else:
                count = count + 1

    def waits_until_action_reaches_progress_percentage(self,
                                                       action_id,
                                                       action_type,
                                                       progress,
                                                       scope=DEFAULT_SCOPE,
                                                       rest_version="v1"):
        """
        Checks for a particular progress percentage for an action
        :param action_id: id of an action
        :param action_type: type of action
        :param progress: the progress percentage of the action
        :param scope: the scope to be used for checking the action
        :param rest_version: the BRO REST API version
        """
        action_url = self.get_action_url(scope, rest_version)

        self.check_for_specific_action_id(
            action_id,
            action_type,
            scope,
            rest_version)

        action = "{}/{}".format(action_url, action_id)
        count = 0
        minutes_5 = 300
        while count < minutes_5:
            out = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                          "Get action progress",
                                          action,
                                          ca_cert=self.ca_cert_path)

            # Verify action progress percentage
            if out["progressPercentage"] >= progress:
                return
            count = count + 1
            time.sleep(1)
        log("Action did not reach {} in 5 minutes".format(progress))
        raise Exception("Action failed to reach the expected progress")

    def wait_for_brm_to_be_available(self, action, brm_id, backup_id=None,
                                     timeout=300):
        """
        Waits till backup manager becomes available to perform
        a specific action on a specified backup
        :param action: the name of the action
        :param brm_id: the name of the backup manager
        :param backup_id: the backup id for some actions
        :param timeout: raise exception if timeout is exceeded(unit=secs)
        """
        log(f"Checking if backup manager {brm_id} is available to "
            f"run action '{action}'")
        endpoint = f"/backup-managers/{brm_id}"
        action_task_mapping = {
            "EXPORT": {"task": "export", "requires_backup": True},
            "DELETE_BACKUP": {"task": "delete", "requires_backup": True},
            "RESTORE": {"task": "restore", "requires_backup": True},
            "CREATE_BACKUP": {"task": "create", "requires_backup": False},
            "IMPORT": {"task": "import", "requires_backup": False}
        }
        start_time = time.time()
        while time.time() - start_time < timeout:
            response = requests.get(f"{self.v4_base_url}{endpoint}")
            log(f"Got backup manager's endpoint  {brm_id},{response.text}")
            if response.status_code == 200:
                data = response.json()
                available_tasks = data["availableTasks"]

                # Check if the action is one of those
                # that does not require a backupid(CREATE,IMPORT)
                if action in action_task_mapping:
                    task_key = action_task_mapping[action]["task"]
                    requires_backup = action_task_mapping[action][
                        "requires_backup"
                    ]
                    for task in available_tasks:
                        if task_key in task:
                            if requires_backup:
                                task_info = task[task_key]
                                href = task_info["href"]
                                task_backup_id = re.findall(
                                    r"/backups/([^/]+)",
                                    href
                                )
                                if task_backup_id[0] == backup_id:
                                    log(
                                        f"Action '{action}' is available for "
                                        f"{brm_id}."
                                    )
                                return

                            else:
                                log("Action '{}' is available for {}"
                                    .format(action, brm_id))
                                return
                else:
                    raise ValueError("Invalid action provided")
            else:
                raise ValueError("Request failed with status code: "
                                 "{0}".format(response.status_code))
            time.sleep(1)
        raise TimeoutError(
            f"Timeout ({timeout} seconds) exceeded. "
            f"Backup manager not available for action '{action}'."
        )

    def wait_for_orchestrator_to_be_available(self, rest_version="v1"):
        """
        Waits till orchestrator becomes available
        :param rest_version: the BRO REST API version
        """

        count = 0
        while count < 300:
            if self.get_availability_status(rest_version) == "Available":
                break
            count = count + 1
            time.sleep(1)

        assert self.get_availability_status(rest_version) == "Available", \
            "Orchestrator is unavailable to perform an action"

    def expect_error_code(self, out, num_actions_before, error_code):
        """
        checks that a given error code occurs and action list
        does not increase in size
        :param out: the return from attempting to create an action
        :param num_actions_before: the size of the list of actions
                                   before attempting this action.
        :param error_code: the expected error code
        """

        assert out["statusCode"] == error_code, \
            "Expected the status code of the response to be {} but it was: " \
            "{}".format(error_code, out["statusCode"])
        # Verify an action was not created
        assert num_actions_before == len(self.get_actions()), \
            "The number of actions has changed. No new actions should have " \
            "been created."

    def get_orchestrator_metadata(self, rest_version="v3"):
        """
        Gets the metadata stored in Orchestrator for backup-managers,
        actions and backups.
        :param rest_version: the BRO REST API version

        :return: a list combining the query results.
        """
        # Get all backup-managers
        base_backup_manager_url = "/".join(
            self.get_backupmanager_url(
                DEFAULT_SCOPE,
                rest_version).split("/")[:-1])
        brms = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                       "Get all backup-managers",
                                       base_backup_manager_url,
                                       ca_cert=self.ca_cert_path)

        # Get all actions in the default backup-manager
        action_url = self.get_action_url(DEFAULT_SCOPE, rest_version)
        actions = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                          "Get default backup-manager actions",
                                          action_url,
                                          ca_cert=self.ca_cert_path)

        # List backups in the default backup manager
        backup_url = self.get_backup_url(DEFAULT_SCOPE, rest_version)
        backups = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                          "Get default backup-manager backups",
                                          backup_url,
                                          ca_cert=self.ca_cert_path)
        backups["backups"].sort(key=operator.itemgetter('name'))

        # Get housekeeping metadata
        # Hardcoded to v3 as v1 housekeeping is not implemented
        housekeeper_url = self.get_backupmanager_url(DEFAULT_SCOPE, "v3") + \
            "/housekeeping"

        housekeeping = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                               "Get housekeeping info",
                                               housekeeper_url,
                                               ca_cert=self.ca_cert_path)

        return [brms, actions, backups, housekeeping]

    def verify_backup_metadata(self, backup_name, agent_list,
                               scope=DEFAULT_SCOPE,
                               rest_version="v1"):
        """
        Verifies the metadata stored in Orchestrator
        for a successfully created backup.

        :param backup_name: name of the backup
        :param agent_list: list of agents in the backup.
                           Each agent is represented by a dictionary
        :param scope : the scope of the backup
        :param rest_version: the BRO REST API version
        """

        backup_url = self.get_backup_url(scope, rest_version)

        # Get metadata for the specific backup
        out = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                      "Get backup {}".format(backup_name),
                                      "{}/{}".format(backup_url, backup_name),
                                      ca_cert=self.ca_cert_path)

        assert {out["name"], out["creationType"],
                out["status"], out["id"]} == {backup_name, "MANUAL",
                                              "COMPLETE", backup_name}

        # Verify the expected agents are in the backup
        # included application software version
        expected_agents = ["APPLICATION_INFO"]
        for agent in agent_list:
            agent_id = agent["AgentId"]
            expected_agents.append(agent_id)

        backup_agents = []
        for product in out["softwareVersions"]:
            agent = product["agentId"]
            backup_agents.append(agent)

        # Verify that the backup contains all expected agents
        assert set(backup_agents) == set(expected_agents), \
            "Agents missing for backup metadata"

    def get_context(self, backup_manager_id,
                    backup_id=None, rest_version="v3"):
        """
        Get context for a backupManager or backup.
        :param backup_manager_id: id of the backupManager.
        :param backup_id: id of the backup.
        :param rest_version: the BRO REST API version
        """
        backup_manager_index = self.get_backup_manager_index(
            backup_manager_id,
            rest_version)
        context = "/ericsson-brm:brm/backup-manager/{}"
        context = context.format(backup_manager_index)

        if backup_id:
            backup_index = self.get_backup_index(
                backup_manager_id,
                backup_id,
                rest_version)
            context = "{}/backup/{}".format(context, backup_index)
        return context

    def export_backup(self, backup_name, payload,
                      scope=DEFAULT_SCOPE, expected_result="SUCCESS",
                      failure_info=None,
                      rest_version="v1",
                      wait_for_action_complete=True):
        """
        Exports a backup into a remote sftp or http location
        :param backup_name: name of backup to be exported
        :param payload: required - sftp server, including:
                                        backupName:  Backup to be exported
                                        uri: sftp URL definition including
                                             the sftp user and path
                                        password: sftp password
                                 - http server, including:
                                        backupName:  Backup to be exported
                                        uri: http URL definition including
                                             the path
        :param scope: name of the scope
        :param expected_result: indicates if export is expected to be
                                successful
        :param failure_info: string or substring expected in the additionalInfo
                             of a failed action
        :param rest_version: the BRO REST API version
        :returns action id of the backup action
        """
        log("Exporting backup {}".format(backup_name))

        out = self.create_action("EXPORT", payload, scope, rest_version)

        action_id = out["id"]
        # Follow the backup progress
        if wait_for_action_complete:
            self.wait_for_action_complete(action_id, "EXPORT",
                                          backup_name, scope, expected_result,
                                          failure_info,
                                          rest_version=rest_version)
        return action_id

    def v4_export_backup(self, backup_name, payload,
                         scope=DEFAULT_SCOPE, expected_result="SUCCESS",
                         failure_info=None, wait_for_action_complete=True):
        """
        Exports a backup into a remote sftp server using the BRO v4 endpoint
        :param backup_name: name of backup to be exported
        :param payload: required - sftp server, including:
                                        uri: sftp URL definition including
                                             the sftp user and path
                                        password: sftp password
        :param scope: name of the scope
        :param expected_result: indicates if export is expected to be
                                successful
        :param failure_info: string or substring expected in the additionalInfo
                             of a failed action
        :returns the export id
        """
        log("Exporting backup via v4 API {}".format(backup_name))
        action_url = "{}/{}".format(
            self.get_specific_backup_url(backup_name, scope, "v4"), "exports")
        out = self.post_request("V4_EXPORT", action_url, payload)

        # verify the export location of the backup
        tarball_name = self.get_backup_tarball_name(backup_name)
        export_path = payload["uri"] + scope + "/" + tarball_name
        log("Expected export path: {}".format(export_path))
        log("Actual export path: {}".format(out["uri"]))
        assert export_path == out["uri"]

        # Follow the export progress
        export_id = out["id"]

        if wait_for_action_complete:
            self.wait_for_action_complete(export_id, "EXPORT",
                                          backup_name, scope, expected_result,
                                          failure_info,
                                          rest_version="v3")
        return export_id

    def v4_get_exports(self, backup_name,
                       scope=DEFAULT_SCOPE):
        """
        Gets all exports using the BRO v4 endpoint

        :param scope: name of the scope
        :param backup_name: name of the backup
        :returns list of all exports
        """
        action_url = "{}/{}/{}/{}".format(
            self.get_backupmanager_url(scope, "v4"),
            "backups", backup_name, "exports")

        out = rest_utils.rest_request(
            rest_utils.REQUESTS.get,
            "Get all {} exports"
            .format(scope),
            action_url,
            ca_cert=self.ca_cert_path)

        return out

    def v4_get_exports_by_id(self, backup_name, export_id,
                             scope=DEFAULT_SCOPE):
        """
        Gets export by id by using the BRO v4 endpoint

        :param scope: name of the scope
        :param backup_name: name of the backup
        :param export_id: id of the export
        :returns the export
        """
        action_url = "{}/{}/{}/{}/{}".format(
            self.get_backupmanager_url(scope, "v4"),
            "backups", backup_name, "exports", export_id)

        out = rest_utils.rest_request(
            rest_utils.REQUESTS.get,
            "Get export for scope {} by ID"
            .format(scope),
            action_url,
            ca_cert=self.ca_cert_path)

        return out

    def get_backup_tarball_name(self, backup_name):
        """
        Gets the expected tarball name of a backup
        based on its id and creation time
        : backup_name: name of the backup
        returns the tarball name for a backup
        """
        backup = self.get_backup(backup_name)
        creation_time = backup["creationTime"]
        return "{}-{}.tar.gz".format(backup_name, creation_time)

    def import_backup(self, backup_name, payload,
                      scope=DEFAULT_SCOPE,
                      rest_version="v1",
                      wait_for_action_complete=True):
        """
        Imports a backup using from the remote location
        Imports the remote directory into the BRO backups directory
        :param backup_name: name of backup
        :param  payload: required - sft server, including
                                        uri: sftp URL definition including
                                             the sftp user and path
                                        password: sftp password
                                  - http server, including
                                        uri: http URL definition including path
        :param scope: name of the scope

        :returns action id of the import action
        """
        # Create an action for IMPORT
        out = self.create_action("IMPORT", payload, scope, rest_version)
        action_id = out["id"]

        # Follow the backup progress
        if wait_for_action_complete:
            self.wait_for_action_complete(action_id, "IMPORT",
                                          backup_name, scope,
                                          rest_version=rest_version)
        return action_id

    def import_backup_to_sftp(self, test_case, backup, sftp_path,
                              scope, sftp_server, namespace):
        """
        Imports backup from SFTP. Will check if backup exists
        in BRO and delete before import.
        """
        log("Test Case: {}".format(test_case))

        if self.backup_exists(backup):
            self.delete_backup(backup, namespace)
            self.wait_for_backup_to_be_removed(backup)

        log("Import {}".format(backup))

        # Creates the payload for import.
        # Import does not requires the "backupName" parameter
        path_remote = sftp_path + scope + "/" + backup
        sftp_payload = sftp_server.get_sftp_payload(path_remote)

        self.import_backup(
            backup_name=backup,
            payload=sftp_payload)

        assert self.backup_exists(backup), \
            "Backup {} has not been imported ".format(backup)

    def v4_import_backup(self, backup_name, payload,
                         scope=DEFAULT_SCOPE,
                         wait_for_action_complete=True):
        """
        Imports a backup from a remote SFTP server location using
        using the BRO v4 endpoint
        :param backup_name: name of backup
        :param  payload: required - sftp server, including
                                        uri: sftp URL definition including
                                             the sftp user and path
                                        password: sftp password
        :param scope: name of the scope
        :returns the import id
        """
        action_url = "{}/{}".format(self.get_backupmanager_url(scope, "v4"),
                                    "imports")
        out = self.post_request("V4_IMPORT", action_url, payload)
        # Follow the import progress
        import_id = out["id"]

        # to be done in a later V4 story, update for V4
        if wait_for_action_complete:
            self.wait_for_action_complete(import_id, "IMPORT",
                                          backup_name, scope,
                                          rest_version="v3")
        return import_id

    def v4_get_imports(self, scope=DEFAULT_SCOPE):
        """
        Gets all imports using the BRO v4 endpoint

        :param scope: name of the scope
        :returns list of all imports
        """
        action_url = "{}/{}".format(
            self.get_backupmanager_url(scope, "v4"),
            "imports")

        out = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                      "Get all {} imports"
                                      .format(scope),
                                      action_url,
                                      ca_cert=self.ca_cert_path)

        return out

    def v4_get_imports_by_id(self, import_id, scope=DEFAULT_SCOPE):
        """
        Gets import by id by using the BRO v4 endpoint

        :param scope: name of the scope
        :param import_id: id of the import
        :returns the import by id
        """
        action_url = "{}/{}/{}".format(
            self.get_backupmanager_url(scope, "v4"),
            "imports", import_id)

        out = rest_utils.rest_request(
            rest_utils.REQUESTS.get,
            "Get imports {} by ID"
            .format(scope),
            action_url,
            ca_cert=self.ca_cert_path)

        return out

    def import_backup_with_existing_backup_deletion(self, backup_name,
                                                    remote_backup_name,
                                                    namespace,
                                                    sftp_path,
                                                    sftp_server,
                                                    scope=DEFAULT_SCOPE,
                                                    rest_version="v1"):
        """
        This deletes an existing backup, then imports the same backup
        from the SFTP server, and verifies that the backup exists
        in the orchestrator.
        :param backup_name: name of backup
        :param remote_backup_name: the name of the legacy backup folder/tarball
                                   in the SFTP server
        :param namespace: namespace where orchestrator pod is deployed
        :param sftp_path: the base path of the sftp server
        :param sftp_server: the SFTP server
        :param scope: name of the scope
        :param rest_version: BRO's REST API version

        """
        log("Deletes backup {} if exist".format(backup_name))
        if self.backup_exists(backup_name, scope):
            self.delete_backup(backup_name,
                               namespace,
                               scope,
                               rest_version)

        log("Import {}".format(remote_backup_name))
        # Creates the payload for import.
        path_remote = sftp_path + scope + "/" + remote_backup_name
        sftp_payload = sftp_server.get_sftp_payload(path_remote)

        self.import_backup(backup_name, sftp_payload, scope)

        assert self.backup_exists(backup_name, scope), \
            "Backup {} has not been imported ".format(backup_name)

    def wait_for_backup_to_be_removed(self, backup_name,
                                      scope=DEFAULT_SCOPE, rest_version="v1"):
        """
        Waits until the backup has been removed from the backup manager

        :param backup_name: name of backup
        :param scope: name of the scope
        :param rest_version: the BRO REST API version
        """
        count = 0
        log("Waiting until backup {} is removed".format(backup_name))
        while count < 120:
            if self.backup_exists(backup_name, scope, rest_version):
                log("Backup {} is still present")
                count = count + 1
                time.sleep(1)
            else:
                break
        assert not self.backup_exists(backup_name, scope, rest_version), \
            "Timeout waiting for backup {} to be removed".format(backup_name)

    def wait_for_bro_agents_to_reconnect(self, agent_ids, rest_version="v1"):
        """
        Used when the channel between orchestrator and agent is broken.
        Checks whether the agents have re-registered.
        If not waits for GRPC retry to be successful.

        :param agent_ids: list of agent ids
        :param rest_version: the BRO REST API version
        """
        log("Waiting for agents to register: {}".format(agent_ids))

        count = 0

        while count < 12:
            status = self.get_registered_agents(rest_version)
            if set(status) != set(agent_ids):
                log("Not all agents have re-registered")
                count = count + 1
                time.sleep(10)
            else:
                break

        # Verify that test agents have successfully registered
        assert set(self.get_registered_agents(rest_version)) == \
            set(agent_ids), "Timeout waiting for Agents to re-register"

    def wait_for_agent_to_unregister(self, agent_id, rest_version="v1"):
        """
        Waits until an agent is no longer registered in the Orchestrator
        :param rest_version: the BRO REST API version
        """
        count = 0

        log("Waiting until agent <{}> is no longer registered"
            .format(agent_id))
        while count < 30:
            status = self.get_registered_agents(rest_version)
            if agent_id in set(status):
                log("Agent is still registered")
                count = count + 1
                time.sleep(5)
            else:
                log("Agent is not registered")
                break
        assert agent_id not in self.get_registered_agents(rest_version), \
            "Timeout waiting for agent <{}> to unregister." \
            .format(agent_id)

    def get_scheduler_config(self, scope=DEFAULT_SCOPE, rest_version='v3'):
        """
        Retrieve the scheduler config of a backup manager

        :return a dict representing the current scheduler configuration
        """
        if rest_version == 'v3':
            return requests.get(
                self.v3_base_url + "/backup-managers/" +
                scope + "/scheduler").json()
        if rest_version == 'v4':
            return requests.get(
                self.v4_base_url + "/backup-managers/" +
                scope + "/configuration/scheduling").json()
        raise Exception("Unknown rest version")

    def update_scheduler_config(self, config, scope=DEFAULT_SCOPE):
        """
        Update the scheduler config of a backup manager

        :return a dict representing the updated scheduler configuration
        """
        return requests.put(self.v3_base_url + "/backup-managers/" +
                            scope + "/scheduler/configuration",
                            json=config).json()

    def v4_put_scheduler_config(self, config, scope=DEFAULT_SCOPE):
        """
        Update the scheduler config of a backup manager using V4 REST endpoint
        """
        log("Updating the scheduler config of <{}> BRM".format(scope))
        requests.put(self.get_backupmanager_url(scope, "v4") +
                     "/configuration/scheduling",
                     json=config)

    def v4_patch_scheduler_config(self, config, scope=DEFAULT_SCOPE):
        """
        Update the scheduler config of a backup manager using V4 REST endpoint
        """
        log("Updating the scheduler config of <{}> BRM".format(scope))
        requests.patch(self.get_backupmanager_url(scope, "v4") +
                       "/configuration/scheduling",
                       json=config)

    def create_scheduled_periodic_event(
            self, event, scope=DEFAULT_SCOPE, rest_version='v3'):
        """
        Create a scheduler periodic event
        :return the id of the event created
        """
        if rest_version == 'v3':
            return requests.post(self.v3_base_url + "/backup-managers/" +
                                 scope + "/scheduler/periodic-events",
                                 json=event).json()["id"]
        if rest_version == 'v4':
            return requests.post(self.v4_base_url + "/backup-managers/" +
                                 scope + "/periodic-schedules",
                                 json=event).json()["id"]
        raise Exception("Unknown rest version")

    def delete_scheduled_periodic_event(
            self, event_id, scope=DEFAULT_SCOPE, rest_version='v3'):
        """
        Delete the scheduled periodic event with the given ID
        """
        if rest_version == 'v3':
            requests.delete(self.v3_base_url + "/backup-managers/" + scope +
                            "/scheduler/periodic-events/" + str(event_id))
            return
        if rest_version == 'v4':
            requests.delete(self.v4_base_url + "/backup-managers/" + scope +
                            "/periodic-schedules/" + str(event_id))
            return
        raise Exception("Unknown rest version")

    def get_scheduled_periodic_event(
            self, event_id, scope=DEFAULT_SCOPE, rest_version='v3'):
        """
        Retrieves the given scheduled periodic event, for the given scope

        :return a dict representing the periodic event
        """
        if rest_version == 'v3':
            return requests.get(
                self.v3_base_url + "/backup-managers/" + scope +
                "/scheduler/periodic-events/" +
                str(event_id)).json()
        if rest_version == 'v4':
            return requests.get(
                self.v4_base_url + "/backup-managers/" + scope +
                "/periodic-schedules/" +
                str(event_id)).json()
        raise Exception("Unknown rest version")

    def update_scheduled_periodic_event(self,
                                        event_id,
                                        update,
                                        scope=DEFAULT_SCOPE,
                                        rest_version='v3'):
        """
        Update the periodic event with the given event id.

        :param event_id - the id of the event to be updated
        :param update - the values to set on the event, e.g. {"hours": 1}
        :param scope - the backup manager the event belongs to
        :return the updated event values
        """
        if rest_version == 'v3':
            return requests.put(
                self.v3_base_url + "/backup-managers/" + scope +
                "/scheduler/periodic-events/" + str(event_id),
                json=update).json()
        if rest_version == 'v4':
            return requests.patch(
                self.v4_base_url + "/backup-managers/" + scope +
                "/periodic-schedules/" + str(event_id),
                json=update).json()
        raise Exception("Unknown rest version")

    def v4_replace_scheduled_periodic_event(self, event_id,
                                            update, scope=DEFAULT_SCOPE):
        """
        Update the periodic event with the given event id.

        :param event_id - the id of the event to be updated
        :param update - the values to set on the event, e.g. {"hours": 1}
        :param scope - the backup manager the event belongs to
        :return the updated event values
        """
        return requests.put(self.v4_base_url + "/backup-managers/" + scope +
                            "/periodic-schedules/" + str(event_id),
                            json=update).json()

    def delete_periodic_events(self, events, scope, rest_version='v3'):
        """
        Deletes periodic events

        :param events - list of events to delete
        :param scope - the backup manager the events belongs to
        """
        for event in events:
            log("Deleting event: " + str(event))
            self.delete_scheduled_periodic_event(
                event["id"], scope, rest_version)

    def create_scheduled_periodic_event_autoexport(
            self,
            sftp_server,
            sftp_path,
            sftp_password,
            scope,
            rest_version="v3",
            check_additional_info=True,
            check_timezone_in_rest=True,
            include_sec_when_zero=True,
            include_nanosec=True,
            delete_periodic_events=False,
            cleanup_events_after_test=True,
            timezone="Z"):
        """
        Configures the default backup manager's scheduler
        with auto export enabled and set up to export to
        the sftp server, then sets up an event to run every
        minute, sleeps long enough to let it run a couple
        of times, then checks both backup and export actions
        have been run and completed successfully. Cleans up
        the created event but leaves the scheduler configured
        to do auto export after a scheduled backup.

        This could conceivably be broken out such that the
        tests after this one run while waiting the two
        minutes, but this minimises room for hard to debug
        behaviour
        """
        # Wait for the SFTP server to come up
        sftp_server.wait_to_start()

        backup_name = self.configure_scheduler(sftp_server, sftp_path,
                                               sftp_password, scope,
                                               rest_version)

        # Create an event to start 2.5 hours from now,
        # with a period of 1 hour, and
        # check the "nextScheduledTime"
        date_format = ""
        now = time.time()
        nsec = repr(now).split('.')[1][:6]

        scheduled_time_in_utc = now + 60*(60+60+30)

        # Expected scheduled time in scheduler is scheduled_time_in_utc
        expected_scheduled_time_in_utc = time.localtime(scheduled_time_in_utc)
        start_time = calculate_time_in_timezone(timezone,
                                                scheduled_time_in_utc)

        if not include_sec_when_zero and \
                expected_scheduled_time_in_utc.tm_sec == 0:
            date_format = "%Y-%m-%dT%H:%M"
        else:
            date_format = "%Y-%m-%dT%H:%M:%S"

        if include_nanosec:
            date_format = "%Y-%m-%dT%H:%M:%S.{}" + timezone
            date_format = date_format.format(nsec)

        log("Start time after timezone update" +
            time.strftime(date_format, start_time))
        # We're not going to bother with a stop time, we'll delete the
        # event when we're done, then wait a bit and assert it didn't run
        # after being deleted
        event_config = {
            "hours": 1,
            "startTime": time.strftime(date_format, start_time)
        }

        returned_events = self.get_scheduled_periodic_events(
            rest_version, scope)
        log("Scheduled events: " + str(returned_events))

        # Delete any existing events
        if delete_periodic_events:
            log("Deleting Periodic Events")
            self.delete_periodic_events(returned_events, scope, rest_version)

        # Get existing actions to be used for identifying the actions
        # created by the periodic event
        actions_before = self.get_actions(scope)

        # Create the event, then get the event as created by the scheduler
        event_id = self.create_scheduled_periodic_event(event_config,
                                                        scope, rest_version)
        returned_event = self.get_scheduled_periodic_event(event_id,
                                                           scope, rest_version)

        log("Got event id: " + str(event_id))
        log("Scheduled event: " + str(returned_event))

        if check_timezone_in_rest:
            # Check periodic event REST matches regex
            # in EOI for startTime
            assert_time_matches_regex_eoi(returned_event["startTime"])

        # Check the start time is correct here
        assert_equal(
            remove_trailing_zeros(returned_event["startTime"]),
            remove_trailing_zeros(time.strftime(date_format, start_time)))

        # Now we're going to check it handled the date/time stuff properly,
        # by checking the scheduler's "nextScheduledTime" is equal to the
        # time we think it should be. This /should/ be exactly correct, because
        # we're setting a fixed startime, not relying on now()
        config = self.get_scheduler_config(scope, rest_version)
        log("Got scheduler config: " + str(config))

        # Expected next time with milliseconds trimmed off
        expected_next_scheduled_time = \
            time.strftime(date_format.split(".")[0],
                          expected_scheduled_time_in_utc)
        log("Expected next scheduled time: " + expected_next_scheduled_time)

        # Get the actual next scheduled time with milliseconds trimmed off
        actual_next_scheduled_time = \
            ".".join(config["nextScheduledTime"].split(".")[:-1])
        log("Actual next scheduled time: " + actual_next_scheduled_time)
        assert_equal(actual_next_scheduled_time, expected_next_scheduled_time)

        # Now we're going to update the event to happen every minute and set
        # the startTime to 2.5 hours ago, then
        # wait a couple of minutes to let it run once or twice (whether it
        # runs once or twice depends on timing which can vary between
        # test runs, but we know if should run /at least/ once)

        # Update to schedule 2.5 hours in the past
        expected_scheduled_time_in_utc = now - 60*(60+60+30)
        start_time = \
            calculate_time_in_timezone(timezone,
                                       expected_scheduled_time_in_utc)

        update = {
            "startTime": time.strftime(date_format, start_time),
            "hours": 0,
            "minutes": 1
        }
        updated_event = self.update_scheduled_periodic_event(event_id,
                                                             update,
                                                             scope,
                                                             rest_version)
        log("Got updated event: " + str(updated_event))
        # Let's check the start time is correct here,
        # just to be sure the update
        # didn't break anything
        actual_start_time = updated_event["startTime"]

        if not include_nanosec:
            actual_start_time = updated_event["startTime"].split(".")[0]

        log("Start time after updating the event: " + actual_start_time)

        expected_start_time = time.strftime(date_format, start_time)
        log("Expected start time: " + expected_start_time)

        assert_equal(remove_trailing_zeros(actual_start_time),
                     remove_trailing_zeros(expected_start_time))

        time.sleep(120)  # Wait a couple of minutes to let the event run

        # Get list of actions, assert at least one backup and export success
        self.wait_for_orchestrator_to_be_available()
        actions_after = self.get_actions(scope)

        # Get the actions ran in this test
        actions_ran_in_this_test = []
        for action in actions_after:
            if action not in actions_before:
                actions_ran_in_this_test.append(action)
        log("----------------------------------------")
        log(actions_ran_in_this_test)
        log("----------------------------------------")

        # Assert at least one CREATE_BACKUP and EXPORT event ran
        assert [x for x in actions_ran_in_this_test
                if x["name"] == "EXPORT"], \
            "No exports found"

        assert [x for x in actions_ran_in_this_test
                if x["name"] == "CREATE_BACKUP"], \
            "No backups found"

        # Assert all CREATE_BACKUP and EXPORT actions ran
        # in this test were successful
        for action in actions_ran_in_this_test:
            if action["name"] in ["CREATE_BACKUP", "EXPORT"]:
                assert action["result"] == "SUCCESS", \
                    "Action " + str(action) + " failed"

        # Assert all EXPORT actions has backup name in additionalInfo
        # except backup exported in 3.2.0+51
        if check_additional_info:
            for action in actions_ran_in_this_test:
                if action["name"] in ["EXPORT"]:
                    assert backup_name in self.get_action_additionalInfo(
                        action["id"], "EXPORT"), \
                        "additionalInfo not found in " + str(action)

        returned_events = self.get_scheduled_periodic_events(
            rest_version, scope)
        log("Scheduled events: " + str(returned_events))
        # Delete any existing periodic events after run
        if cleanup_events_after_test:
            log("Deleting Periodic Events")
            self.delete_periodic_events(returned_events, scope, rest_version)

        return event_id

    def get_scheduled_periodic_events(self, rest_version, scope):
        """
        Get the list of periodic schedules
        """
        if rest_version == 'v3':
            returned_events = \
                self.get_scheduled_periodic_event("",
                                                  scope,
                                                  rest_version)["events"]
        if rest_version == 'v4':
            returned_events = \
                self.get_scheduled_periodic_event("",
                                                  scope,
                                                  rest_version)["schedules"]
        return returned_events

    def configure_scheduler(self, sftp_server, sftp_path, sftp_password,
                            scope, rest_version="v3"):
        """
        Configures the default backup manager's scheduler with
        auto export enabled and set up to export to the sftp server
        """
        # Constants used
        backup_name = "TEST_SCHEDULED_BACKUP"

        # Configure the scheduler for SFTP export
        config = self.get_scheduler_config(scope)
        config["autoExport"] = "ENABLED"
        config["autoExportPassword"] = sftp_password
        config["autoExportUri"] = sftp_server.get_sftp_uri_for_path(sftp_path)
        config["scheduledBackupName"] = backup_name

        if rest_version == "v3":
            updated_config = self.update_scheduler_config(config, scope)
        else:
            self.v4_put_scheduler_config(config, scope)
            updated_config = self.get_scheduler_config(scope)

        # Check the returned auto export password is obfuscated
        assert updated_config["autoExportPassword"] and \
            updated_config["autoExportPassword"] != sftp_password
        return backup_name

    def create_calendar_based_event(self, event, scope=DEFAULT_SCOPE):
        """
        Create a calendar based event
        :return the id of the event created
        """
        event_id = requests.post(self.v4_base_url + "/backup-managers/" +
                                 scope + "/calendar-schedules",
                                 json=event).json()["id"]

        calendar_event = self.get_calendar_based_event(event_id)
        log("Got event id: " + str(event_id))
        log("Scheduled event: " + str(calendar_event))
        assert_time_matches_regex_eoi(calendar_event["startTime"])
        return event_id

    # Helper function: creates a calendar event to be updated
    def create_calendar_update(self, seconds_to_change=30,
                               month=1, day_of_month=31):
        """
        Creates a calendar event with 3 parameters to be used on
        put and patch operations.
        This calendar expected to be updated at least by seconds_to_change
        @param: seconds_to_change Number of seconds to be updated,
                must be in range (0-120). Default 30 seconds
        @param: month Month used to create the event
        @param: day_of_month Day used to create the event
        @return: The time and date schedule expected
                  Event Id generated
        """
        if seconds_to_change not in range(0, 120):
            log("Seconds to change not in range (1,120), using 30 instead ")
            seconds_to_change = 30
        now = datetime.now()
        first_runtime = now + timedelta(seconds=120)
        update_runtime = now + timedelta(seconds=seconds_to_change)
        next_minute = first_runtime.strftime(TIME_FORMAT)
        update_minute = update_runtime.strftime(TIME_FORMAT)

        event = {
            "time": next_minute,
            "month": month,
            "dayOfMonth": day_of_month
        }
        event_id = self.create_calendar_based_event(event)
        calendar_event_initial = self.get_calendar_based_event(event_id)
        config = self.get_scheduler_config(
            scope=DEFAULT_SCOPE, rest_version='v4')
        # Get the next scheduled time with milliseconds trimmed off
        next_scheduled_time = config["nextScheduledTime"][:-1]
        dt_obj = datetime.strptime(next_scheduled_time, DATE_FORMAT)
        update_runtime = dt_obj - timedelta(seconds=120 - seconds_to_change)
        update_expected_next_time = update_runtime.strftime(DATE_FORMAT)
        log("Got event id: " + str(event_id))
        log("Scheduled event: " + str(calendar_event_initial))
        log("Scheduled Initial time: " + str(next_minute))
        log("Scheduled initial run time: " + next_scheduled_time)
        log("Scheduled expected run time: " + update_expected_next_time)
        return update_minute, update_expected_next_time, event_id

    def get_calendar_based_event(self, event_id, scope=DEFAULT_SCOPE):
        """
        Gets the calendar based event with specified id
        """
        return requests.get(
            self.get_backupmanager_url(scope, "v4") +
            "/calendar-schedules/" +
            str(event_id)).json()

    def get_calendar_based_events(self, scope=DEFAULT_SCOPE):
        """
        Gets the calendar based events for specified scope
        """
        return self.get_calendar_based_event("",
                                             scope)["schedules"]

    def delete_calendar_based_event(
            self, event_id, scope=DEFAULT_SCOPE):
        """
        Delete the calendar based event with the given ID
        """
        return requests.delete(self.get_backupmanager_url(scope, "v4") +
                               "/calendar-schedules/" + str(event_id))

    def put_calendar_based_event(
            self, event_id, config, scope=DEFAULT_SCOPE):
        """
        Replace calendar based event with the given ID
        """
        return requests.put(self.get_backupmanager_url(scope, "v4") +
                            "/calendar-schedules/" + str(event_id),
                            json=config)

    def patch_calendar_based_event(
            self, event_id, config, scope=DEFAULT_SCOPE):
        """
        Update calendar based event with the given ID
        """
        return requests.patch(self.get_backupmanager_url(scope, "v4") +
                              "/calendar-schedules/" + str(event_id),
                              json=config)

    def generate_backup_tarball_name(self, backup_name, scope=DEFAULT_SCOPE):
        """
        Generates the tarball name of an existing backup
        in the Orchestrator.

        :param backup_name - name of the backup
        :param scope - the backup manager the backup belongs to
        """
        backup_info = self.get_backup(backup_name, scope)
        creation_time = backup_info["creationTime"]
        backup_tarball_name = \
            "{}-{}.tar.gz".format(backup_name, creation_time)
        return backup_tarball_name
