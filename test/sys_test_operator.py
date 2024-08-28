#!/usr/bin/env python3
"""
This module provides test cases to verify the Backup and Restore Orchestrator
features on in-house operator KVDB-RD (Redis Notification)
"""
import os
import time
import json
import utilprocs
from utilprocs import log
import k8sclient
import helm3procs
import bro_ctrl
import bro_utils
from bro_utils import assert_equal
import bro_cmyp_cli_client
import rest_utils
import bro_redis_utils
import sys_cm
from globals import (V4_FRAG_CM,
                     DDPG_AGENT)


# snapshot repo
INT_SNAP_REPO = os.environ.get("helm_repo")
# chart_version
INT_SNAP_VERSION = os.environ.get('baseline_chart_version')
INT_SNAP_REPO_NAME = "int_snap"

NAMESPACE = os.environ.get('kubernetes_namespace')

# Set BRO variables
INT_CHART_NAME = "eric-bro-int-operator"

# test service helm repo
TEST_REPO = os.environ.get("test_helm_repo")
TEST_CHART_NAME = "eric-test-service-basic"
TEST_REPO_NAME = "test_svc"
TEST_CHART_VERSION = ""

# scope
DEFAULT_SCOPE = "DEFAULT"
CONFIGURATION_SCOPE = "configuration-data"

# set integration chart deployment timeout
DEPLOY_TIMEOUT = 900

BRO_POD_NAME = "eric-ctrl-bro-0"

KUBE = k8sclient.KubernetesClient()
BRO_CTRL = bro_ctrl.BroCtrlClient()

AGENTS = [V4_FRAG_CM, DDPG_AGENT]

# Define Backup Name
BACKUPS = ["bu1", "bu2", "bu3", "bu4"]

# Redis details
REDIS_CLIENT = None
BRO_TOPIC = 'bro-notification'

# Define CMYP CLI data
CMYP_USER = "brocmyp"
CMYP_PSW = "TeamPlanBYang$1"
CMYP = bro_cmyp_cli_client.CMYPCliClient(CMYP_USER, CMYP_PSW, BRO_CTRL)


# Test Cases
def test_clear_any_failed_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    log("Test Case: test_clear_any_failed_resources")
    bro_utils.remove_namespace_resources(NAMESPACE, remove_redis_cluster=False,
                                         remove_kvdb_cluster=True)
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
                                    kvdb_new_operator=True,
                                    data=True)

    # Verify that the Orchestrator has all expected agents registered
    try:
        BRO_CTRL.wait_for_bro_agents_to_reconnect([DDPG_AGENT["AgentId"]])

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


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

    finally:
        get_all_pod_logs(test_case)


def test_create_backup_cmyp():
    """
     Creates a backup and verifies its presence
     in the Orchestrator and CMYP.
    """
    test_case = "test_create_backup_cmyp"
    log("Test Case: {}".format(test_case))

    # Create a Redis_Client Instance
    global REDIS_CLIENT
    REDIS_CLIENT = bro_redis_utils.RedisClient(BRO_TOPIC, NAMESPACE)

    try:
        # Populate CM with test data
        sys_cm.create_test_data()

        # Create backup
        progress_report_id = CMYP.create_backup(
            BACKUPS[0], backup_manager=CONFIGURATION_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id, "CREATE_BACKUP", scope=CONFIGURATION_SCOPE,
            backup_name=BACKUPS[0])

        REDIS_CLIENT.validate_notification(
            progress_report_id, CONFIGURATION_SCOPE, "CREATE_BACKUP")

        # Collect the logs after a successful backup
        get_all_pod_logs("post_{}".format(test_case))
    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_restore_backup_cmyp():
    """
    This test restores a backup through CMYP
    """
    test_case = "test_restore_backup_cmyp"
    log("Test Case: {}".format(test_case))

    # Update housekeeping config to set max-backups to 5
    # and auto-delete disabled
    CMYP.set_housekeeping_configuration("disabled", "5",
                                        backup_manager=CONFIGURATION_SCOPE)
    BRO_CTRL.wait_for_orchestrator_to_be_available()

    number_of_restores = 1
    for count in range(number_of_restores):
        try:
            # Restore backups
            log("Executing restore {} of {}"
                .format(count + 1, number_of_restores))
            progress_report_id = CMYP.restore_backup(
                BACKUPS[0], backup_manager=CONFIGURATION_SCOPE)

            wait_until_bro_cmyp_ready_after_action(
                progress_report_id,
                "RESTORE",
                backup_name=BACKUPS[0],
                scope=CONFIGURATION_SCOPE)

            REDIS_CLIENT.validate_notification(
                progress_report_id, CONFIGURATION_SCOPE, "RESTORE")

            get_all_pod_logs(test_case)

        except Exception:
            get_all_pod_logs(test_case)
            raise

        finally:
            get_all_pod_logs(test_case)


def test_failed_backup():
    """
    This test verifies a backup can be made after BRO restarts/recovers
    during a backup operation
    """
    test_case = "test_backup_failure_after_bro_restarts_during_backup"
    utilprocs.log("Test Case: {}".format(test_case))

    BRO_CTRL.wait_for_orchestrator_to_be_available()

    create_payload = {"backupName": BACKUPS[1]}
    action_id = BRO_CTRL.create_action(
        "CREATE_BACKUP", create_payload, DEFAULT_SCOPE)

    # Collect console logs before restart
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME,
                      "pre_restart_{}".format(test_case))

    bro_utils.restart_pod(BRO_POD_NAME, NAMESPACE)
    BRO_CTRL.wait_for_bro_agents_to_reconnect(
        [DDPG_AGENT["AgentId"], V4_FRAG_CM["AgentId"]])

    REDIS_CLIENT.validate_notification_failure(
        action_id["id"], DEFAULT_SCOPE, "CREATE_BACKUP")
    BRO_CTRL.wait_for_orchestrator_to_be_available()

    create_payload = {"backupName": BACKUPS[2]}
    action_id = BRO_CTRL.create_action(
        "CREATE_BACKUP", create_payload, DEFAULT_SCOPE)

    time.sleep(20)

    REDIS_CLIENT.validate_notification(
        action_id["id"], DEFAULT_SCOPE, "CREATE_BACKUP")


def test_failed_restore():
    """
    This test verifies a restore can be made after BRO restarts/recovers
    during a restore operation
    """
    test_case = "test_restore_failure_after_bro_restarts_during_restore"
    utilprocs.log("Test Case: {}".format(test_case))
    create_payload = {"backupName": BACKUPS[2]}
    action_id = BRO_CTRL.create_action(
        "RESTORE", create_payload, DEFAULT_SCOPE)

    # Collect console logs before restart
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME,
                      "pre_restart_{}".format(test_case))

    bro_utils.restart_pod(BRO_POD_NAME, NAMESPACE)
    BRO_CTRL.wait_for_bro_agents_to_reconnect(
        [DDPG_AGENT["AgentId"], V4_FRAG_CM["AgentId"]])

    REDIS_CLIENT.validate_notification_failure(
        action_id["id"], DEFAULT_SCOPE, "RESTORE")


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

    # Close Redis Client
    REDIS_CLIENT.close()

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
                                           backup_name=None):
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
        backup_name=backup_name
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
