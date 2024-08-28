#!/usr/bin/env python3
"""
This module provides robustness test cases for the Backup and Restore
Orchestrator
"""
import json
import os
import time
import utilprocs
import k8sclient
import helm3procs
import bro_ctrl
import bro_utils
import bro_cmyp_cli_client
import bro_sftp
import rest_utils
import bro_redis_utils


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
TEST_HELM_RELEASE = "agent-large-file-{}".format(NAMESPACE)
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

HELM_RELEASE_NAME = bro_utils.get_service_helm_release_name(
    INT_CHART_NAME, NAMESPACE)

DDPG_AGENT = {"AgentId": "eric-data-document-database-pg",
              "pod_prefix": "eric-data-document-database-pg"}

LG_AGENT = {"AgentId": "agent-large-file",
            "pod_prefix": "agent-large-file-agent"}

AGENTS = [DDPG_AGENT, LG_AGENT]

# Define Backup Name
BACKUP_NAME_LIST = ["backup_1", "backup_2"]
BACKUPS = ["bu1", "bu2", "bu3", "bu4", "bu5"]

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

# Redis details
REDIS_CLIENT = None
BRO_TOPIC = 'bro-notification'


# Test Cases
def test_clear_any_failed_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    utilprocs.log("Test Case: test_clear_any_failed_resources")

    bro_utils.remove_namespace_resources(NAMESPACE)
    utilprocs.log("Finished removing Kubernetes resources")


def test_deploy_integration_chart():
    """
    Deploys BRO Integration Helm Chart.
    """

    utilprocs.log("Test Case: test_deploy_integration_chart")

    utilprocs.log(
        "Add the Int Snapshot helm repo {0} - {1}".format(INT_SNAP_REPO_NAME,
                                                          INT_SNAP_REPO))
    helm3procs.add_helm_repo(INT_SNAP_REPO, INT_SNAP_REPO_NAME)
    pm_service = "eric-pm-server.service.endpoints"
    pm_server = "eric-pm-server.server.service"
    kafka = "eric-data-message-bus-kf.service.endpoints.messagebuskf"
    kvdb = "eric-data-key-value-database-rd.service.endpoints.kvdbClients"
    tls_cert = "tls.verifyClientCertificate"
    settings = {
        "{}.scrapeTargets.tls.enforced".format(pm_service): "required",
        "{}.reverseproxy.tls.enforced".format(pm_service): "required",
        "{}.reverseproxy.{}".format(pm_service, tls_cert): "required",
        "{}.scrapeTargets.tls.enforced".format(pm_server): "required",
        "{}.reverseproxy.{}".format(pm_server, tls_cert): "required",
        "{}.tls.enforced".format(kafka): "required",
        "{}.tls.verifyClientCertificate".format(kafka): "required",
        "{}.tls.enforced".format(kvdb): "required"
    }
    # Install Integration Chart
    bro_utils.install_service_chart(INT_SNAP_REPO_NAME, INT_CHART_NAME,
                                    INT_SNAP_VERSION, NAMESPACE,
                                    DEPLOY_TIMEOUT,
                                    kvdb_rd_format="ASAJSONSTRING",
                                    settings=settings)

    # Verify that the Orchestrator has all expected agents registered
    # While real agents are disabled we do not expect any at this point
    BRO_CTRL.wait_for_bro_agents_to_reconnect([DDPG_AGENT["AgentId"]])

    # Starts the sftp server, doesn't wait for the service to be up and running
    SFTP_SERVER.start_sftp_server()

    time.sleep(200)

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
                "type":  "password",
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


def test_backup_after_bro_restarts_during_backup():
    """
    This test verifies a backup can be made after BRO restarts/recovers
    during a backup operation
    """
    test_case = "test_backup_after_bro_restarts_during_backup"
    utilprocs.log("Test Case: {}".format(test_case))

    utilprocs.log("Add the test service helm repo")
    global TEST_REPO
    global TEST_CHART_VERSION
    helm3procs.add_helm_repo(TEST_REPO, TEST_REPO_NAME)

    TEST_CHART_VERSION = \
        helm3procs.get_latest_chart_version(TEST_CHART_NAME,
                                            helm_repo_name=TEST_REPO_NAME,
                                            development_version=True)

    bro_utils.install_bro_agents(
        TEST_REPO_NAME,
        TEST_CHART_NAME,
        TEST_CHART_VERSION,
        [LG_AGENT["AgentId"]],
        NAMESPACE
    )
    bro_utils.wait_for_resources_to_deploy(
        TEST_HELM_RELEASE, NAMESPACE)
    BRO_CTRL.wait_for_bro_agents_to_reconnect(
        [LG_AGENT["AgentId"], DDPG_AGENT["AgentId"]])

    # Update housekeeping config to set max-backups to 2
    # and auto-delete to enabled
    CMYP.set_housekeeping_configuration("enabled", "2",
                                        backup_manager=DEFAULT_SCOPE)
    BRO_CTRL.wait_for_orchestrator_to_be_available()
    assert CMYP.get_housekeeping_configuration(
        backup_manager=DEFAULT_SCOPE) == ("2", "enabled"), \
        "Unexpected values in housekeeping configuration"

    # Start the REDIS CLIENT
    global REDIS_CLIENT
    REDIS_CLIENT = bro_redis_utils.RedisClient(BRO_TOPIC, NAMESPACE)

    create_payload = {"backupName": BACKUPS[0]}
    action_id = BRO_CTRL.create_action(
        "CREATE_BACKUP", create_payload, DEFAULT_SCOPE)

    # Collect console logs before restart
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME,
                      "pre_restart_{}".format(test_case))

    bro_utils.restart_pod(BRO_POD_NAME, NAMESPACE)
    BRO_CTRL.wait_for_bro_agents_to_reconnect(
        [LG_AGENT["AgentId"], DDPG_AGENT["AgentId"]])

    latest_message = REDIS_CLIENT.get_latest_notification()
    bro_redis_utils.validate_json_notification(
        latest_message, action_id["id"], DEFAULT_SCOPE,
        "CREATE_BACKUP", "FAILED")

    action_id = BRO_CTRL.create_backup(
        BACKUPS[1], AGENTS, NAMESPACE, test_agent=False)

    bro_redis_utils.validate_json_notifications(
        REDIS_CLIENT, action_id, DEFAULT_SCOPE, "CREATE_BACKUP", "COMPLETED")


def test_restore_after_bro_restarts_during_backup():
    """
    This test verifies a restore can be made after BRO restarts/recovers
    during a backup operation
    """
    test_case = "test_restore_after_bro_restarts_during_backup"
    utilprocs.log("Test Case: {}".format(test_case))
    create_payload = {"backupName": BACKUPS[2]}
    BRO_CTRL.create_action("CREATE_BACKUP", create_payload, DEFAULT_SCOPE)

    # Collect console logs before restart
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME,
                      "pre_restart_{}".format(test_case))

    bro_utils.restart_pod(BRO_POD_NAME, NAMESPACE)
    BRO_CTRL.wait_for_bro_agents_to_reconnect(
        [LG_AGENT["AgentId"], DDPG_AGENT["AgentId"]])

    BRO_CTRL.restore_backup(BACKUPS[1], AGENTS, NAMESPACE, test_agent=False)


def test_backup_after_bro_restarts_during_restore():
    """
    This test verifies a backup can be made after BRO restarts/recovers
    during a restore operation
    """
    test_case = "test_backup_after_bro_restarts_during_restore"
    utilprocs.log("Test Case: {}".format(test_case))
    create_payload = {"backupName": BACKUPS[1]}
    BRO_CTRL.create_action("RESTORE", create_payload, DEFAULT_SCOPE)

    # Collect console logs before restart
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME,
                      "pre_restart_{}".format(test_case))

    bro_utils.restart_pod(BRO_POD_NAME, NAMESPACE)
    BRO_CTRL.wait_for_bro_agents_to_reconnect(
        [LG_AGENT["AgentId"], DDPG_AGENT["AgentId"]])

    BRO_CTRL.create_backup(BACKUPS[4], AGENTS, NAMESPACE, test_agent=False)


def test_restore_after_bro_restarts_during_restore():
    """
    This test verifies a restore can be made after BRO restarts/recovers
    during a restore operation
    """
    test_case = "test_restore_after_bro_restarts_during_restore"
    utilprocs.log("Test Case: {}".format(test_case))
    create_payload = {"backupName": BACKUPS[4]}
    action_id = BRO_CTRL.create_action(
        "RESTORE", create_payload, DEFAULT_SCOPE)

    # Collect console logs before restart
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME,
                      "pre_restart_{}".format(test_case))

    bro_utils.restart_pod(BRO_POD_NAME, NAMESPACE)
    BRO_CTRL.wait_for_bro_agents_to_reconnect(
        [LG_AGENT["AgentId"], DDPG_AGENT["AgentId"]])

    bro_redis_utils.validate_json_notifications(
        REDIS_CLIENT, action_id["id"], DEFAULT_SCOPE, "RESTORE", "FAILED")

    action_id = BRO_CTRL.restore_backup(
        BACKUPS[4], AGENTS, NAMESPACE, test_agent=False)

    bro_redis_utils.validate_json_notifications(
        REDIS_CLIENT, action_id, DEFAULT_SCOPE, "RESTORE", "COMPLETED")


def test_collect_logs():
    """
    Collects logs for archiving
    """
    utilprocs.log("Test Case: test_collect_logs")
    try:
        get_all_pod_logs("post_all_tests")
        utilprocs.log("Sleeping...for 3 min")
        time.sleep(180)
        utilprocs.log("Finished Sleeping")

    except Exception as error:
        utilprocs.log("Error in collect logs")
        utilprocs.log(str(error))
        raise


def test_remove_k8s_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    utilprocs.log("Test Case: test_remove_k8s_resources")
    try:
        # Close the CMYP connection
        CMYP.close()

        # stops sftp server
        SFTP_SERVER.stop_sftp_server()
        bro_utils.remove_namespace_resources(NAMESPACE)
        utilprocs.log("Finished removing Kubernetes resources")

    except Exception as error:
        utilprocs.log("Error in collect logs in test_remove_k8s_resources")
        utilprocs.log(str(error))
        raise


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
