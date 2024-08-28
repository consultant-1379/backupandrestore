#!/usr/bin/env python3
"""
This module is used for running BRO operations
for a given list of backups in performance tests
"""

from datetime import datetime
import pandas as pd

import utilprocs
import rest_utils
from bro_utils import get_pods_by_prefix

# the Backup and Restore Orchestrator URI
BRO_URL = "http://eric-ctrl-bro:7001"

# the root path for backup files in the sftp server
SFTP_PATH = "bro_test/1/3/"

# the relative path to backup-manager in the sftp server
BACKUPMANAGERS_PATH = "/v3/backup-managers/"

# the backup tarball name to be imported
BACKUP_TAR_NAME = ""
BRO_NAME = "eric-ctrl-bro"


def create_backups(backup_list, scope, bro_ctrl,
                   wait_for_action_complete=True):
    """
    Creates the backup for a given backup list and scope
    and records the action ids
    :param backup_list: the list of backups
    :param scope: the scope of the agents involved in the operation
    :bro_ctrl: the BRO rest client
    :return the action ids of all the backup operations
    """
    action_ids = []
    for backup in backup_list:
        create_payload = {"backupName": backup}
        out = bro_ctrl.create_action("CREATE_BACKUP", create_payload,
                                     scope=scope)
        action_ids.append(out["id"])
        utilprocs.log("BACKUP ACTION ID is {}".format(out["id"]))
        # Follow the create backup progress
        if wait_for_action_complete:
            bro_ctrl.wait_for_action_complete(out["id"], "CREATE_BACKUP",
                                              backup,
                                              expected_result="SUCCESS",
                                              scope=scope)
    return action_ids


def export_backups(backup_list, scope, bro_ctrl, sftp_server,
                   wait_for_action_complete=True):
    """
    Exports the backup for a given backup list and scope
    and records the action ids
    :param backup_list: the list of backups
    :param scope: the scope of the agents involved in the operation
    :param bro_ctrl: the BRO rest client
    :param sftp_server: the SFTP server
    :return the action ids of the export operations
    """
    # Creates the payload for export.
    # Export requires the "backupName" parameter
    sftp_payload = sftp_server.get_sftp_payload(SFTP_PATH)
    action_ids = []
    for backup in backup_list:
        sftp_payload.update({"backupName": backup})
        utilprocs.log("Exporting {}".format(backup))
        action_id = bro_ctrl.export_backup(
            backup_name=backup,
            payload=sftp_payload,
            scope=scope,
            wait_for_action_complete=wait_for_action_complete)
        action_ids.append(action_id)
        if wait_for_action_complete:
            assert bro_ctrl.get_action_result(action_id, "EXPORT",
                                              scope=scope) \
                == "SUCCESS"
    return action_ids


def import_backups(backup_list, scope, namespace, bro_ctrl, sftp_server,
                   wait_for_action_complete=True):
    """
    Imports the backup for a given backup list and scope
    and records the action ids
    :param backup_list: the list of backups
    :param scope: the scope of the agents involved in the operation
    :param namespace: namespace where orchestrator pod is deployed
    :param bro_ctrl: the BRO rest client
    :param sftp_server: the SFTP server
    :return the action ids of the import operations
    """
    action_ids = []
    for backup in backup_list:
        utilprocs.log(
            "Delete backup {} if exist".format(backup))

        backup_result = bro_ctrl.get_backup(backup,
                                            scope=scope)

        global BACKUP_TAR_NAME
        creation_time = backup_result["creationTime"]
        BACKUP_TAR_NAME = \
            "{}-{}.tar.gz".format(backup, creation_time)

        if bro_ctrl.backup_exists(backup,
                                  scope=scope):
            if get_pods_by_prefix(namespace, BRO_NAME)[0] == "eric-ctrl-bro-0":
                bro_ctrl.delete_backup(backup,
                                       namespace, scope=scope)
            else:
                bro_ctrl.delete_backup(backup,
                                       namespace, scope=scope,
                                       verify_files=False)
            bro_ctrl.wait_for_backup_to_be_removed(backup,
                                                   scope=scope)

        utilprocs.log("Import {}".format(BACKUP_TAR_NAME))
        path_remote = SFTP_PATH + scope + "/" + BACKUP_TAR_NAME
        sftp_payload = sftp_server.get_sftp_payload(path_remote)

        action_id = bro_ctrl.import_backup(
            backup_name=backup,
            payload=sftp_payload,
            scope=scope,
            wait_for_action_complete=wait_for_action_complete)

        action_ids.append(action_id)
        if wait_for_action_complete:
            assert bro_ctrl.backup_exists(backup,
                                          scope=scope), \
                "Backup {} has not been imported ".format(
                    backup)
    return action_ids


def restore_backups(backup_list, scope, bro_ctrl,
                    wait_for_action_complete=True):
    """
    Restores the backup for a given backup list and scope
    and records the action ids
    :param backup_list: the list of backups
    :param scope: the scope of the agents involved in the operation
    :param bro_ctrl: the BRO rest client
    :return the action ids of the restore operations
    """
    action_ids = []
    for backup in backup_list:
        restore_payload = {"backupName": backup}
        out = bro_ctrl.create_action("RESTORE", restore_payload,
                                     scope=scope)
        action_ids.append(out["id"])
        utilprocs.log("RESTORE ACTION ID is {}".format(out["id"]))
        # Follow the create restore progress
        if wait_for_action_complete:
            bro_ctrl.wait_for_action_complete(out["id"], "RESTORE",
                                              backup,
                                              expected_result="SUCCESS",
                                              scope=scope)
    return action_ids


def calculate_average_time(action_type, backup_list, action_ids, scope):
    """
    Calculate the average time for an action.
    :param action_type: the action type
    :param backup_list: the list of backups associated with the action_ids
    :param action_ids: the map of scope to action_ids
    :param scope: the scope of the agents
    :return the average time for the action type
    """
    header = "------Calculating Average Time For {}------"
    utilprocs.log(header.format(action_type))

    times = []
    actions_url = BACKUPMANAGERS_PATH + scope + "/actions"
    scope_actions = action_ids.get(scope)
    for count, action_id in enumerate(scope_actions):
        action = "{}{}/{}".format(BRO_URL, actions_url,
                                  action_id)
        out = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                      "Get action info",
                                      action)
        start_time = out["startTime"]
        completion_time = out["completionTime"]
        date_format = '%Y-%m-%dT%H:%M:%S.%fZ'
        times.append(datetime.strptime(completion_time,
                                       date_format) - datetime.strptime(
                                           start_time, date_format))
        utilprocs.log("{} {} took {}".format(action_type, backup_list[count],
                                             times[count]))
    average_time = pd.Series(pd.to_timedelta(times)).mean()
    utilprocs.log("AVERAGE {} TIME: {}".format(action_type, average_time))
    return average_time


def log_action_times(action_times, num_agents,
                     test_descr="1GB Agents RESULTS"):
    """
    Prints the average action time for each operation to the log
    :param action_times: map of operation to average action time
    :param num_agents: the number of agents involved in the operations
    :param test_descr: title of the test group
    """
    separator = "{}\n".format("-" * 40)
    utilprocs.log(separator)
    utilprocs.log("TEST REPORT: {} x {}".format(num_agents, test_descr))
    for operation in action_times.keys():
        utilprocs.log("AVERAGE {} TIME: {}".format(operation,
                                                   action_times[operation]))
    utilprocs.log(separator)


def add_actions_times_file(action_times, num_agents,
                           test_descr="1GB-Agents-RESULTS"):
    """
    Creates a csv file for each action time consisting of the action operation
    and the time
    :param action_times: map of operation to average action time
    :param num_agents: the number of agents involved in the operations
    :param test_descr: title of the test group
    """
    for operation in action_times.keys():
        new_file = open("plot/{}x{}-{}.csv".format(num_agents, test_descr,
                                                   operation), "w")
        new_file.write("AVERAGE {} TIME\n".format(operation))
        new_file.write(str(action_times[operation].total_seconds()))
        new_file.close()
