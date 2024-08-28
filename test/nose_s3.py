#!/usr/bin/env python3
"""
This module runs OSMN tests
"""
import os
import time
import bro_ctrl
import bro_sftp
import bro_utils
import helm3procs
import utilprocs
from bro_s3 import BroS3
from globals import (V2_FRAG_ONLY, V2_FRAG_CM,
                     V3_FRAG_CM,
                     V3_FRAG_ONLY,
                     V4_FRAG_CM,
                     V4TLS_FRAG_ONLY,
                     V4_MULTI_FRAG_CM,
                     V4_NO_FRAG_OR_META,
                     V4_NO_FRAG_CM_ONLY)

NAMESPACE = os.environ.get("kubernetes_namespace")
BRO_REPO = os.environ.get("helm_repo")
BRO_NAME = "eric-ctrl-bro"
DEFAULT_SCOPE = "DEFAULT"
BRO_REPO_NAME = "bro_drop"
BRO_SNAP_VERSION = os.environ.get('baseline_chart_version')

# Set Test Service variables
TEST_CHART_NAME = "eric-test-service-basic"
TEST_REPO = os.environ.get("test_helm_repo")
TEST_REPO_NAME = "test_svc"
TEST_CHART_VERSION = ""

BRO_CTRL = bro_ctrl.BroCtrlClient()

# Base REST URL and Scopes
BASE_URL = "http://eric-ctrl-bro:7001/v1/backup-manager"

# SFTP Server USER and password
SFTP_USER = "brsftp"
SFTP_PASSWORD = "planbsftp"
SFTP_PATH = "bro_test/1/3/"
SFTP_SERVER = bro_sftp.SFTPServer(SFTP_USER, SFTP_PASSWORD)

BACKUP_LIST = ["bu1", "bu2"]
MAX_STORED_BACKUPS = 2

V2_AGENTS = [V2_FRAG_ONLY, V2_FRAG_CM]
V3_AGENTS = [V3_FRAG_CM, V3_FRAG_ONLY]
V4_AGENTS = [V4_FRAG_CM,
             V4TLS_FRAG_ONLY,
             V4_MULTI_FRAG_CM,
             V4_NO_FRAG_OR_META,
             V4_NO_FRAG_CM_ONLY]
TEST_AGENTS = V2_AGENTS + V3_AGENTS + V4_AGENTS

BACKUP_TARBALL_NAME = ""
BACKUP_ACTION_IDS = []
RESTORE_ACTION_IDS = []

BRO_URL = "http://eric-ctrl-bro:7001"
ACTIONS_URL = "/v3/backup-managers/DEFAULT/actions"

SEPARATOR = "{}\n".format("-" * 40)


# OSMN server settings
OSMN_CHART_NAME = 'eric-data-object-storage-mn'
OSMN_SVC = BroS3()

# Test Cases


def test_clear_any_failed_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    utilprocs.log("Test Case: test_clear_any_failed_resources")

    bro_utils.remove_namespace_resources(NAMESPACE)
    utilprocs.log("Finished removing Kubernetes resources")


def test_deploy_osmn_server():
    """
    Deploys the OSMN service
    """
    utilprocs.log("Test Case: test_deploy_osmn_server")
    OSMN_SVC.start_server()
    OSMN_SVC.start_client()


def test_deploy_bro_service_osmn():
    """
    Deploys the Orchestrator Service with OSMN enable.
    """
    utilprocs.log("Test Case: test_deploy_bro_service_osmn")

    utilprocs.log("Add the BRO Snapshot helm repo")
    helm3procs.add_helm_repo(BRO_REPO, BRO_REPO_NAME)

    start_time = time.perf_counter()
    # Install Orchestrator Chart
    bro_utils.install_service_chart(BRO_REPO_NAME, BRO_NAME,
                                    BRO_SNAP_VERSION, NAMESPACE,
                                    agent_discovery=True,
                                    enable_global_tls=False,
                                    osmn_enabled=True)
    pods = bro_utils.get_pods_by_prefix(NAMESPACE, BRO_NAME)
    for pod in pods:
        install_time = bro_utils.calc_time(start_time, pod, NAMESPACE)
    utilprocs.log("TEST REPORT: Install Time: {}s".format(install_time))
    # Starts the sftp server, doesn't wait for the service to be up and running
    SFTP_SERVER.start_sftp_server()


def test_deploy_bro_agent():
    """
    Deploys the Test Agents.
    V2: ECHO, TANGO
    V3: BRAVO, GOLF, DELTA
    """
    utilprocs.log(
        "Test Case: test_deploy_bro_agent")

    utilprocs.log("Add the test service helm repo")
    global TEST_CHART_VERSION

    # Get latest test service chart
    helm3procs.add_helm_repo(TEST_REPO, TEST_REPO_NAME)
    TEST_CHART_VERSION = \
        helm3procs.get_latest_chart_version(TEST_CHART_NAME,
                                            helm_repo_name=TEST_REPO_NAME,
                                            development_version=True)

    # Deploy the v2 test agents
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 '4.0.0-8',
                                 [agent["AgentId"] for agent in V2_AGENTS],
                                 NAMESPACE, enable_global_tls=False)

    # Deploy the v3 test agents
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION,
                                 [agent["AgentId"] for agent in V3_AGENTS],
                                 NAMESPACE, enable_global_tls=False)

    # Deploy the v4 test agents
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION,
                                 [agent["AgentId"] for agent in V4_AGENTS],
                                 NAMESPACE, enable_global_tls=False)

    # Verify that Orchestrator has the expected agent/s registered
    BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())


def test_create_backup():
    """
    This test create a backup.
    """
    utilprocs.log("Test Case: test_create_backup")
    auto_delete = "enabled"
    BRO_CTRL.update_housekeeping_config(auto_delete, MAX_STORED_BACKUPS,
                                        DEFAULT_SCOPE)
    # Create backup and check against all agents
    backup = BACKUP_LIST[0]
    create_payload = {"backupName": backup}
    out = BRO_CTRL.create_action("CREATE_BACKUP",
                                 create_payload,
                                 scope=DEFAULT_SCOPE)

    BRO_CTRL.wait_for_action_complete(out["id"], "CREATE_BACKUP",
                                      backup,
                                      expected_result="SUCCESS",
                                      scope=DEFAULT_SCOPE)

    result = OSMN_SVC.backup_exist(backupmanager=DEFAULT_SCOPE,
                                   backupname=backup)
    assert result
    utilprocs.log("BACKUP ID is {}".format(out["id"]))


def test_export_backup():
    """
    This test exports the content of multiple backups
    into an external SFTP server.
    """
    utilprocs.log("Test Case: test_export_backup")
    # Waits until the ftp server is up and running
    SFTP_SERVER.wait_to_start()

    # Creates the payload for export.
    # Export requires the "backupName" parameter
    sftp_payload = SFTP_SERVER.get_sftp_payload(SFTP_PATH)
    sftp_payload.update({"backupName": BACKUP_LIST[0]})
    utilprocs.log("Exporting {}".format(BACKUP_LIST[0]))
    action_id = BRO_CTRL.export_backup(
        backup_name=BACKUP_LIST[0],
        payload=sftp_payload)
    assert BRO_CTRL.get_action_result(action_id, "EXPORT") == "SUCCESS"
    assert (BACKUP_LIST[0] in BRO_CTRL.get_action_additionalInfo(
        action_id, "EXPORT"))


def test_import_backup():
    """
    This test imports multiple backups from a sftp server.
    """
    utilprocs.log("Test Case: test_import_backup")
    backup = BRO_CTRL.get_backup(BACKUP_LIST[0])

    global BACKUP_TARBALL_NAME
    creation_time = backup["creationTime"]
    utilprocs.log("creationTime: " + creation_time)
    BACKUP_TARBALL_NAME = \
        "{}-{}.tar.gz".format(BACKUP_LIST[0], creation_time)
    utilprocs.log("backupName: " + BACKUP_TARBALL_NAME)

    if BRO_CTRL.backup_exists(BACKUP_LIST[0]):
        BRO_CTRL.delete_backup(BACKUP_LIST[0], NAMESPACE,
                               verify_files=False)
        result = OSMN_SVC.backup_exist(backupmanager=DEFAULT_SCOPE,
                                       backupname=BACKUP_LIST[0])
        assert result is False

    utilprocs.log("Import {}".format(BACKUP_TARBALL_NAME))

    # Creates the payload for import.
    # Import does not requires the "backupName" parameter
    path_remote = SFTP_PATH + DEFAULT_SCOPE + "/" \
        + BACKUP_TARBALL_NAME
    sftp_payload = SFTP_SERVER.get_sftp_payload(path_remote)

    BRO_CTRL.import_backup(
        backup_name=BACKUP_LIST[0],
        payload=sftp_payload)

    assert BRO_CTRL.backup_exists(BACKUP_LIST[0]), \
        "Backup {} has not been imported ".format(BACKUP_LIST[0])
    result = OSMN_SVC.backup_exist(backupmanager=DEFAULT_SCOPE,
                                   backupname=BACKUP_LIST[0])
    assert result


def test_restore_backup():
    """
    This test restores multiple backups and stores the action IDs.
    """
    utilprocs.log("Test Case: test_restore_backup")

    out = BRO_CTRL.restore_backup(BACKUP_LIST[0], TEST_AGENTS,
                                  NAMESPACE, validate_checksum=False)
    result = OSMN_SVC.backup_exist(backupmanager=DEFAULT_SCOPE,
                                   backupname=BACKUP_LIST[0])
    assert result

    utilprocs.log("RESTORE ID  is {}".format(out))


def test_delete_bro_pod():
    """
    Deletes the BRO pod and ensures it comes back up running.

    The BRO Test Agent detects the channel is down and will
    continually try to re-establish the GRPC connection until successful.
    """
    test_case = "test_delete_bro_pod"
    utilprocs.log("Test Case: {}".format(test_case))
    start_time = time.perf_counter()
    bro_utils.restart_deployment(BRO_NAME, NAMESPACE)
    pods = bro_utils.get_pods_by_prefix(NAMESPACE, BRO_NAME)
    for pod in pods:
        restart_time = bro_utils.calc_time(start_time, pod, NAMESPACE)
    utilprocs.log("TEST REPORT: Restart Time: {}s".format(restart_time))
    # List status of helm release
    # Verify Orchestrator status and check registered agents
    BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())


def test_remove_k8s_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    utilprocs.log("Test Case: test_remove_k8s_resources")

    # stops sftp server
    SFTP_SERVER.stop_sftp_server()

    bro_utils.remove_namespace_resources(NAMESPACE)
    utilprocs.log("Finished removing Kubernetes resources")


# Helper function: get all test agent ids
def get_agent_ids():
    """
    Fetches the Agent Id of all test agents and
    returns this as a list.

    :return: a list of the agent Ids.
    """

    agents = []
    for agent in TEST_AGENTS:
        agents.append(agent["AgentId"])
    return agents
