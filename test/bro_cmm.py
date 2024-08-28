#!/usr/bin/env python3
"""
This module provides test cases to verify the Backup and Restore Orchestrator
to CMM/CMYP interface and also verifies integration with real BR Agents
"""
import os
import time
import json
from utilprocs import log
import k8sclient
import helm3procs
import bro_ctrl
import bro_utils
from bro_utils import assert_equal
import bro_cmyp_cli_client
import bro_sftp
import rest_utils
import sys_cm
from globals import (V4_FRAG_CM,
                     V4_FRAG_CM_NAME,
                     TLS_AGENT,
                     DDPG_AGENT)
# snapshot repo
INT_SNAP_REPO = os.environ.get("helm_repo")
# chart_version
INT_SNAP_VERSION = os.environ.get('baseline_chart_version')

INT_SNAP_REPO_NAME = "int_snap"

NAMESPACE = os.environ.get('kubernetes_namespace')

# Set BRO variables
INT_CHART_NAME = "eric-bro-int"

# test service helm repo
TEST_REPO = os.environ.get("test_helm_repo")
TEST_CHART_NAME = "eric-test-service-basic"
TEST_REPO_NAME = "test_svc"
TEST_CHART_VERSION = ""

# scope
DEFAULT_SCOPE = "DEFAULT"
CONFIGURATION_SCOPE = "configuration-data"
# the vBRM used in vBRM tests

# set integration chart deployment timeout
DEPLOY_TIMEOUT = 900

BRO_POD_NAME = "eric-ctrl-bro-0"

KUBE = k8sclient.KubernetesClient()
BRO_CTRL = bro_ctrl.BroCtrlClient()

HELM_RELEASE_NAME = bro_utils.get_service_helm_release_name(
    INT_CHART_NAME, NAMESPACE)

VBRM_SCOPE = "DEFAULT-" + V4_FRAG_CM_NAME

# AGENTS = [DDPG_AGENT, KVDB_AGENT]
AGENTS = [V4_FRAG_CM, DDPG_AGENT, TLS_AGENT]

# Define Backup Name
BACKUP_NAME_LIST = ["backup_1", "backup_2"]
BACKUPS = ["bu1", "bu2", "bu3", "bu4", "bu5"]

# Define number of records to insert_delete on test
NUMBER_RECORDS = 10

# Define CMYP CLI data
CMYP_USER = "brocmyp"
CMYP_PSW = "TeamPlanBYang$1"
CMYP = bro_cmyp_cli_client.CMYPCliClient(CMYP_USER, CMYP_PSW, BRO_CTRL)

# SFTP Server USER and password
SFTP_USER = "brsftp"
SFTP_PASSWORD = "planbsftp"
SFTP_PATH = "bro_test/1/3/"

SFTP_SERVER = bro_sftp.SFTPServer(SFTP_USER, SFTP_PASSWORD)

TLS_TEST_CHART_VERSION = "2.5.0-0"

# Periodic event id
EVENT_ID = "id1"

EVENT_ID_2 = "id2"

EVENT_ID_3 = "id3"

SETTINGS_DICT = {"tags.data": "false",
                 "tags.pm": "false",
                 "eric-ctrl-bro.bro.enableNotifications": "false",
                 "eric-ctrl-bro.metrics.enabled": "false",
                 "eric-sec-access-mgmt.metrics.enabled": "false"}


# Test Cases
def test_clear_any_failed_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    log("Test Case: test_clear_any_failed_resources")
    bro_utils.remove_namespace_resources(NAMESPACE)
    log("Finished removing Kubernetes resources")


def test_deploy_integration_chart():
    """
    Deploys BRO Integration Helm Chart.
    """
    test_case = "test_deploy_integration_chart"
    log("Test Case: {}".format(test_case))

    log("Add the Int Snapshot helm repo {0} - {1}".format(INT_SNAP_REPO_NAME,
                                                          INT_SNAP_REPO))
    helm3procs.add_helm_repo(INT_SNAP_REPO, INT_SNAP_REPO_NAME)

    # Install Integration Chart
    bro_utils.install_service_chart(INT_SNAP_REPO_NAME, INT_CHART_NAME,
                                    INT_SNAP_VERSION, NAMESPACE,
                                    DEPLOY_TIMEOUT,
                                    settings=SETTINGS_DICT)
    # Starts the sftp server, doesn't wait for the service to be up and running
    SFTP_SERVER.start_sftp_server()

    # Verify that the Orchestrator has all expected agents registered
    # While real agents are disabled we do not expect any at this point
    try:
        BRO_CTRL.wait_for_bro_agents_to_reconnect([DDPG_AGENT["AgentId"]])

    except Exception:
        get_all_pod_logs(test_case)
        raise


def test_deploy_test_agent():
    """
    Deploys Test Agent Helm Chart.
    """
    test_case = "test_deploy_test_agent"
    log("Test Case: {}".format(test_case))

    log("Add the test service helm repo")
    global TEST_REPO
    global TEST_CHART_VERSION
    helm3procs.add_helm_repo(TEST_REPO, TEST_REPO_NAME)

    TEST_CHART_VERSION = \
        helm3procs.get_latest_chart_version(TEST_CHART_NAME,
                                            helm_repo_name=TEST_REPO_NAME,
                                            development_version=True)
    # Deploy latest test agent
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION,
                                 [V4_FRAG_CM["AgentId"]], NAMESPACE,
                                 agent_log_level="debug",
                                 root_log_level="debug")

    # Deploy a TLS test agent
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TLS_TEST_CHART_VERSION,
                                 [TLS_AGENT["AgentId"]], NAMESPACE,
                                 agent_log_level="debug",
                                 root_log_level="debug")
    try:
        # Verify that Orchestrator has all expected agents registered
        BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())

        # Verify the BRM configuration has been pushed to CM
        prepare_cmyp()
        time.sleep(10)
        CMYP.verify_brm_config()

        assert_equal(CMYP.get_housekeeping_configuration(
            backup_manager=CONFIGURATION_SCOPE), ("1", "enabled"))

        assert_equal(CMYP.get_scheduler_configuration(
            backup_manager=CONFIGURATION_SCOPE),
                     ("unlocked", "disabled", "SCHEDULED_BACKUP", ""))

        assert_equal(CMYP.get_housekeeping_configuration(
            backup_manager=DEFAULT_SCOPE), ("1", "enabled"))

        assert_equal(CMYP.get_scheduler_configuration(
            backup_manager=DEFAULT_SCOPE),
                     ("unlocked", "disabled", "SCHEDULED_BACKUP", ""))

    except Exception:
        get_all_pod_logs(test_case)
        raise


def test_restore_configuration():
    """
    In this test we will test restoring backup manager DEFAULT configuration by
    1. Creating a backup to capture the default configuration
    2. Perform updates to housekeeping, scheduling and create a periodic event
    3. Restore backup using vbrm DEFAULT-bro
    """
    test_case = "test_restore_configuration"
    log("Test Case: {}".format(test_case))

    try:
        backup_name = "for-restore"
        time.sleep(1)
        event_id = CMYP.create_backup(
            backup_name, backup_manager=DEFAULT_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            event_id, "CREATE_BACKUP", scope=DEFAULT_SCOPE,
            backup_name=backup_name, agents=AGENTS)

        # Check that the backup status is updated to "backup-complete" in vbrm
        get_attribute = CMYP.get_backup_status(backup_name,
                                               DEFAULT_SCOPE + "-bro")
        assert_equal(get_attribute, "backup-complete")

        # Update configuration
        CMYP.config_commands([CMYP.get_housekeeping_configuration_command(
            "enabled", "3")])
        BRO_CTRL.wait_for_orchestrator_to_be_available()

        dummy_sftp_uri = "sftp://test@192.168.153.116:22/bro_tests"
        CMYP.set_scheduler_configuration("SCHEDULED", admin_state="locked",
                                         auto_export="enabled",
                                         backup_manager=DEFAULT_SCOPE,
                                         auto_export_uri=dummy_sftp_uri,
                                         auto_export_password=SFTP_PASSWORD)

        CMYP.create_or_update_periodic_event(EVENT_ID, "30", hours="1",
                                             backup_manager=DEFAULT_SCOPE)
        # Adding sleep to make sure the restore
        # doesn't run before all the configurations have finished updating
        time.sleep(5)

        assert_equal(CMYP.get_housekeeping_configuration(
            backup_manager=DEFAULT_SCOPE), ("3", "enabled"))

        assert_equal(CMYP.get_scheduler_configuration(
            backup_manager=DEFAULT_SCOPE),
                     ("locked", "enabled", "SCHEDULED", dummy_sftp_uri))

        assert_equal(CMYP.get_periodic_event(
            EVENT_ID, backup_manager=DEFAULT_SCOPE), ("30", "1"))

        # Restore backup under DEFAULT-bro
        progress_report_id = CMYP.restore_backup(
            backup_name, backup_manager=DEFAULT_SCOPE + "-bro")

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id,
            "RESTORE",
            expected_result="SUCCESS",
            backup_name=backup_name,
            scope=DEFAULT_SCOPE + "-bro")

        # Check the configurations have been restored
        assert_equal(CMYP.get_housekeeping_configuration(
            backup_manager=DEFAULT_SCOPE), ("1", "enabled"))

        assert_equal(CMYP.get_scheduler_configuration(
            backup_manager=DEFAULT_SCOPE),
                     ("unlocked", "disabled", "SCHEDULED_BACKUP", ""))

        response = BRO_CTRL.get_scheduled_periodic_event(EVENT_ID)
        assert response["statusCode"] == 404, \
            "Periodic event " + event_id + " was not removed after restore"

        # After the test， delete the backup for-restore
        progress_report_id = CMYP.delete_backup(
            backup_name, backup_manager=DEFAULT_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id, "DELETE_BACKUP", scope=DEFAULT_SCOPE,
            backup_name=backup_name)
    except Exception:
        get_all_pod_logs(test_case)
        raise


def test_set_housekeeping_max_backups():
    """
    Changes the value of the max number of backups that can be stored under
    a backup manager and verifies the change was applied.
    """
    test_case = "test_set_housekeeping_max_backups"
    log("Test Case: {}".format(test_case))

    CMYP.verify_brm_config()

    try:
        commands = []
        commands.append(CMYP.get_housekeeping_configuration_command(
            "enabled", "3"))
        commands.append(CMYP.get_housekeeping_configuration_command(
            "enabled", "2", backup_manager=CONFIGURATION_SCOPE))
        # excute the above commands in a single commit
        CMYP.config_commands(commands)

        BRO_CTRL.wait_for_orchestrator_to_be_available()
        assert_equal(CMYP.get_housekeeping_configuration(), ("3", "enabled"))

        assert_equal(CMYP.get_housekeeping_configuration(
            backup_manager=CONFIGURATION_SCOPE), ("2", "enabled"))
    except Exception:
        get_all_pod_logs(test_case)
        raise


def test_create_backup_cmyp():
    """
     Creates a backup and verifies its presence
     in the Orchestrator and CMYP.
    """
    test_case = "test_create_backup_cmyp"
    log("Test Case: {}".format(test_case))

    try:
        # Populate CM with test data
        sys_cm.create_test_data()

        # Create backup
        progress_report_id = CMYP.create_backup(
            BACKUP_NAME_LIST[0], backup_manager=CONFIGURATION_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id, "CREATE_BACKUP", scope=CONFIGURATION_SCOPE,
            backup_name=BACKUP_NAME_LIST[0], agents=AGENTS)

        # Delete test data from CM
        sys_cm.remove_test_data()

        assert CMYP.backup_manager_has_backup(
            BACKUP_NAME_LIST[0], backup_manager=CONFIGURATION_SCOPE), \
            "Backup {} was not found in the backup list".format(
                BACKUP_NAME_LIST[0])

        assert_equal(CMYP.get_backup_status(
            BACKUP_NAME_LIST[0], backup_manager=CONFIGURATION_SCOPE
        ), "backup-complete")

        # Collect the logs after a successful backup
        get_all_pod_logs("post_{}".format(test_case))
    except Exception:
        get_all_pod_logs(test_case)
        raise


def test_export_backup_cmyp():
    """
    This test uses a single backup and export the
    content into an external SFTP server.
    """
    test_case = "test_export_backup_cmyp"
    log("Test Case: {}".format(test_case))

    try:
        # Waits until the ftp server is up and running
        SFTP_SERVER.wait_to_start()

        uri = SFTP_SERVER.get_sftp_uri_for_path(SFTP_PATH)

        log("Exporting {} from {}".format(BACKUP_NAME_LIST[0], uri))

        progress_report_id = CMYP.export_backup(
            BACKUP_NAME_LIST[0], uri, SFTP_PASSWORD,
            backup_manager=CONFIGURATION_SCOPE)

        # wait_until_bro_cmyp_ready_after_action(
        #     progress_report_id, "EXPORT", backup_name=BACKUP_NAME_LIST[0],
        #     scope=CONFIGURATION_SCOPE)
        # Would use wait_until_bro_cmyp_ready_after_action but need to carry
        # out the check for progress percetnage while the action is running
        CMYP.wait_until_progress_report_is_complete(
            progress_report_id, 'EXPORT', scope=CONFIGURATION_SCOPE,
            backup_name=BACKUP_NAME_LIST[0]
        )

    except Exception:
        get_all_pod_logs(test_case)
        raise


def test_create_second_backup_cmyp():
    """
     Creates a second backup and verifies its presence
     in the Orchestrator and CMYP.
     Updates the housekeeping configuration to set auto-delete to disabled
     and to set max-stored-manual-backups to 1, thus deleting oldest backup.
    """
    test_case = "test_create_second_backup_cmyp"
    log("Test Case: {}".format(test_case))

    try:

        # Create backup
        progress_report_id = CMYP.create_backup(
            BACKUP_NAME_LIST[1], backup_manager=CONFIGURATION_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id, "CREATE_BACKUP", scope=CONFIGURATION_SCOPE,
            backup_name=BACKUP_NAME_LIST[1], agents=AGENTS)

        assert CMYP.backup_manager_has_backup(
            BACKUP_NAME_LIST[1], backup_manager=CONFIGURATION_SCOPE), \
            "Backup {} was not found in the backup list".format(
                BACKUP_NAME_LIST[1])

        assert_equal(CMYP.get_backup_status(
            BACKUP_NAME_LIST[1], backup_manager=CONFIGURATION_SCOPE
        ), "backup-complete")

        # Update housekeeping config to set max-backups to 1
        # and auto-delete to disabled
        CMYP.set_housekeeping_configuration("disabled", "1",
                                            backup_manager=CONFIGURATION_SCOPE)
        BRO_CTRL.wait_for_orchestrator_to_be_available()
        assert_equal(CMYP.get_housekeeping_configuration(
            backup_manager=CONFIGURATION_SCOPE), ("1", "disabled"))

        # Collect the logs after a successful backup
        get_all_pod_logs("post_{}".format(test_case))
    except Exception:
        get_all_pod_logs(test_case)
        raise


def test_import_backup_cmyp():
    """
    Imports a backup from a sftp server.
    """
    test_case = "test_import_backup_cmyp"
    log("Test Case: {}".format(test_case))
    log("Import {}".format(BACKUP_NAME_LIST[0]))

    # Update housekeeping config to set max-backups to 5
    # and auto-delete disabled
    CMYP.set_housekeeping_configuration("disabled", "5",
                                        backup_manager=CONFIGURATION_SCOPE)
    BRO_CTRL.wait_for_orchestrator_to_be_available()

    try:
        path = "{}{}/{}".format(SFTP_PATH, CONFIGURATION_SCOPE,
                                BACKUP_NAME_LIST[0])
        uri = SFTP_SERVER.get_sftp_uri_for_path(path)

        log("Importing {} from {}".format(BACKUP_NAME_LIST[0], uri))

        progress_report_id = CMYP.import_backup(
            uri, SFTP_PASSWORD, backup_manager=CONFIGURATION_SCOPE)

        # wait_until_bro_cmyp_ready_after_action(
        #     progress_report_id, "IMPORT", scope=CONFIGURATION_SCOPE,
        #     backup_name=BACKUP_NAME_LIST[0])

        # Would use wait_until_bro_cmyp_ready_after_action but need to carry
        # out the check for progress percetnage while the action is running
        CMYP.wait_until_progress_report_is_complete(
            progress_report_id, 'IMPORT', scope=CONFIGURATION_SCOPE,
            backup_name=BACKUP_NAME_LIST[0]
        )

        assert CMYP.backup_manager_has_backup(
            BACKUP_NAME_LIST[0], backup_manager=CONFIGURATION_SCOPE), \
            "Backup {} has not been imported.".format(BACKUP_NAME_LIST[0])

    except Exception:
        get_all_pod_logs(test_case)
        raise


def test_restore_backup_cmyp():
    """
    This test restores a backup through CMYP 3 times
    """
    test_case = "test_restore_backup_cmyp"
    log("Test Case: {}".format(test_case))

    number_of_restores = 3
    for count in range(number_of_restores):
        try:
            # Restore backups
            log("Executing restore {} of {}"
                .format(count + 1, number_of_restores))
            progress_report_id = CMYP.restore_backup(
                BACKUP_NAME_LIST[0], backup_manager=CONFIGURATION_SCOPE)

            wait_until_bro_cmyp_ready_after_action(
                progress_report_id,
                "RESTORE",
                backup_name=BACKUP_NAME_LIST[0],
                scope=CONFIGURATION_SCOPE, agents=AGENTS)
            # Need to reconnect after the restore process
            # Verify the test data in CM
            sys_cm.verify_test_data()

            # Allow time for cmyp schema synchronizer container to come up
            time.sleep(30)

            # Check backup limit remains same after restore
            assert_equal(CMYP.get_housekeeping_configuration(
                backup_manager=CONFIGURATION_SCOPE), ("5", "disabled"))

            get_all_pod_logs(test_case)

        except Exception:
            get_all_pod_logs(test_case)
            raise

    get_all_pod_logs(test_case)


def test_delete_backup_cmyp():
    """
    This test verifies that a backup can be deleted.
    Uses v2 endpoint.
    """
    test_case = "test_delete_backup_cmyp"
    log("Test Case: {}".format(test_case))

    try:
        progress_report_id = CMYP.delete_backup(
            BACKUP_NAME_LIST[0], backup_manager=CONFIGURATION_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id, "DELETE_BACKUP", scope=CONFIGURATION_SCOPE,
            backup_name=BACKUP_NAME_LIST[0])

        assert CMYP.backup_manager_has_backup(
            BACKUP_NAME_LIST[0],
            backup_manager=CONFIGURATION_SCOPE) is False, \
            "Backup {} was found in the backup list.".format(
                BACKUP_NAME_LIST[0])

    except Exception:
        get_all_pod_logs(test_case)
        raise


def test_collect_logs():
    """
    Collects logs for archiving
    """
    log("Test Case: test_collect_logs")
    get_all_pod_logs("post_all_tests")


def test_remove_k8s_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    log("Test Case: test_remove_k8s_resources")
    # Close the CMYP connection
    CMYP.close()

    # stops sftp server
    SFTP_SERVER.stop_sftp_server()
    bro_utils.remove_namespace_resources(NAMESPACE)
    log("Finished removing Kubernetes resources")


# Helper functions
def get_agent_ids():
    """
    Fetches the Agent Id of all agents and
    returns this as a list.

    :return: a list of the agent Ids.
    """

    agents = []
    for agent in AGENTS:
        agents.append(agent["AgentId"])
    return agents


def get_all_pod_logs(log_file_suffix):
    """
    Gets the pod logs for all agents and the Orchestrator

    :param log_file_suffix: A string to append to the name of the log files
    """
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, log_file_suffix)
    get_agent_pod_logs(log_file_suffix)


def get_agent_pod_logs(log_file_suffix):
    """
    Gets the pod logs for all agents

    :param log_file_suffix : A string to append to the name of the log files
    """
    agent_pod_prefixes = get_agent_pod_prefixes()

    for prefix in agent_pod_prefixes:
        bro_utils.get_pod_logs(NAMESPACE, prefix, log_file_suffix)


def get_agent_pod_prefixes():
    """
    Fetches the pod_prefixes of all agents and
    returns this as a list.
    :return: a list of the agent pod_prefixes.
    """

    prefixes = []
    for agent in AGENTS:
        prefixes.append(agent["pod_prefix"])
    return prefixes


def wait_until_bro_cmyp_ready_after_action(progress_report_id,
                                           action_name,
                                           scope="DEFAULT",
                                           expected_result="SUCCESS",
                                           backup_name=None,
                                           agents=None):
    """
    wait until both BRO and CMYP is ready
    :param progress_report_id : the id of progress report
    :param action_name : the name of action
    :param scope : the BRM
    :param expected_result : the expected result
    :param backup_name : the name of backup
    """
    BRO_CTRL.wait_for_action_complete(
        action_id=progress_report_id,
        action_type=action_name,
        scope=scope,
        expected_result=expected_result,
        backup_name=backup_name
    )
    time.sleep(10)
    CMYP.wait_until_progress_report_is_complete(
        progress_report_id=progress_report_id,
        action_name=action_name,
        scope=scope,
        expected_result=expected_result,
        backup_name=backup_name,
        agents=agents
    )


def prepare_cmyp():
    """
    Prepare the cmyp user and connect to the cmyp server
    """
    # Create the cmyp user in LDAP via IAM
    token_query_url = ('https://eric-sec-access-mgmt-http:8443/'
                       'auth/realms/master/protocol/openid-connect/token')
    token_request_data = {'grant_type': 'password', 'username': 'adminplanb',
                          'password': 'kcpwb@Planb1', 'client_id': 'admin-cli'}
    token_request_headers = {'content-type':
                             'application/x-www-form-urlencoded'}
    # Query the endpoint for the token
    token_request_response = rest_utils.rest_request(
        rest_utils.REQUESTS.post, "Retrieve IAM token",
        token_query_url, token_request_data, headers=token_request_headers,
        content_type_json=False, ca_cert=False)
    token = json.loads(token_request_response)['access_token']
    user_creation_query_url = ('https://eric-sec-access-mgmt-http:8443/'
                               'auth/admin/realms/local-ldap3/users')
    user_creation_request_data = {
        "username": CMYP_USER,
        "enabled": "true",
        "credentials": [
            {
                "type": "password",
                "value": CMYP_PSW
            }
        ],
        "attributes": {
            "uidNumber": ["710"],
            "roles": ["system-admin"],
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
    CMYP.connect()


def check_log_is_present(retrieved_logs_dict, search_terms):
    """
    Checks if given message is present in the given logs dictionary argument.
    :param retrieved_logs_dict: retrieved logs dictionary from Search Engine
    :param search_terms: list of terms which is unique to the required log
    :return: a boolean whether the logs were found or not
    """
    # for each "hit" in the list of hits check the log message contents
    for hit in retrieved_logs_dict["hits"]["hits"]:
        if all(term in hit["_source"]["message"] for term in search_terms):
            return True

    return False
