#!/usr/bin/env python3
"""
This module runs daily robustness tests for BRO with OSMN
"""
import os
import time
import bro_ctrl
import k8sclient
import bro_utils
import helm3procs
import utilprocs

NAMESPACE = os.environ.get("kubernetes_namespace")
INT_SNAP_REPO = os.environ.get("helm_repo")
DEFAULT_SCOPE = "DEFAULT"

INT_SNAP_VERSION = os.environ.get('baseline_chart_version')
INT_SNAP_REPO_NAME = "int_snap"
INT_CHART_NAME = "eric-bro-int"
DEPLOY_TIMEOUT = 900

# Set Test Service variables
TEST_CHART_NAME = "eric-test-service-basic"
TEST_REPO = os.environ.get("test_helm_repo")
TEST_REPO_NAME = "test_svc"
TEST_CHART_VERSION = ""

KUBE = k8sclient.KubernetesClient()
BRO_CTRL = bro_ctrl.BroCtrlClient()

BACKUPS = ["bu1", "bu2", "bu3", "bu4", "bu5"]

DDPG_AGENT = {"AgentId": "eric-data-document-database-pg",
              "pod_prefix": "eric-data-document-database-pg"}

LG_AGENT_A = {"AgentId": "agent-large-file-a",
              "pod_prefix": "agent-large-file-agent-a"}

LG_AGENT_B = {"AgentId": "agent-large-file-b",
              "pod_prefix": "agent-large-file-agent-b"}

AGENTS = [DDPG_AGENT, LG_AGENT_A, LG_AGENT_B]

OSMN_POD_LIST = ["eric-data-object-storage-mn-0",
                 "eric-data-object-storage-mn-1",
                 "eric-data-object-storage-mn-2",
                 "eric-data-object-storage-mn-3"]

BRO_NAME = "eric-ctrl-bro-"


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

    # Install Integration Chart
    bro_utils.install_service_chart(INT_SNAP_REPO_NAME, INT_CHART_NAME,
                                    INT_SNAP_VERSION, NAMESPACE,
                                    DEPLOY_TIMEOUT,
                                    agent_discovery=True,
                                    osmn_enabled=True,
                                    data=False, osmn_mode="distributed")

    # Verify that the Orchestrator has all expected agents registered
    # While real agents are disabled we do not expect any at this point
    BRO_CTRL.wait_for_bro_agents_to_reconnect([DDPG_AGENT["AgentId"]])


def test_deploy_bro_agent():
    """
    Deploys the Test Agent.
    """
    utilprocs.log("Test Case: test_deploy_bro_agent")

    utilprocs.log("Add the test service helm repo")
    global TEST_CHART_VERSION

    # Get latest test service chart
    helm3procs.add_helm_repo(TEST_REPO, TEST_REPO_NAME)
    TEST_CHART_VERSION = \
        helm3procs.get_latest_chart_version(TEST_CHART_NAME,
                                            helm_repo_name=TEST_REPO_NAME,
                                            development_version=True)

    # Deploy 2 agents with configuration-data scope
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION,
                                 [LG_AGENT_A["AgentId"],
                                  LG_AGENT_B["AgentId"]],
                                 NAMESPACE,
                                 generate_values_path=False)

    # Verify that Orchestrator has the expected agent/s registered
    BRO_CTRL.wait_for_bro_agents_to_reconnect(
        [DDPG_AGENT["AgentId"], LG_AGENT_A["AgentId"], LG_AGENT_B["AgentId"]])


def test_backup_after_osmn_restarts_during_backup():
    """
    This test verifies a backup can be made after osmn restarts/recovers
    during a backup operation
    """
    test_case = "test_backup_after_osmn_restarts_during_backup"
    utilprocs.log("Test Case: {}".format(test_case))
    # Update housekeeping config to set max-backups to 2
    # and auto-delete to enable
    BRO_CTRL.update_housekeeping_config("enabled", "2",
                                        backup_manager_name=DEFAULT_SCOPE)
    create_payload = {"backupName": BACKUPS[3]}
    out = BRO_CTRL.create_action("CREATE_BACKUP", create_payload,
                                 DEFAULT_SCOPE)
    action_id = out["id"]

    # Restart osmn pods
    BRO_CTRL.waits_until_action_reaches_progress_percentage(action_id,
                                                            "CREATE_BACKUP",
                                                            progress=0.30)
    for elem in OSMN_POD_LIST:
        KUBE.delete_pod(elem, NAMESPACE, wait_for_terminating=False)

    # sleep time to wait for osmn pods to restart
    time.sleep(120)
    for elem in OSMN_POD_LIST:
        KUBE.wait_for_pod_to_start(elem, NAMESPACE)

    # Due to OSMN being more robust since this test was created,
    # the backup action is now either a success or failure
    # after the OSMN pods have been restarted.
    # Test is changed to now wait for the action to be finished
    # and logs the result.
    # BRO_CTRL.wait_for_action_complete(action_id, "CREATE_BACKUP",
    #                                   BACKUPS[3], expected_result="FAILURE")
    # BRO_CTRL.wait_for_orchestrator_to_be_available()

    # verify if the previous create backup action is finished
    BRO_CTRL.wait_for_action_complete(action_id, "CREATE_BACKUP",
                                      BACKUPS[3], expected_result=None)
    BRO_CTRL.wait_for_orchestrator_to_be_available()

    # verify a successful backup can be performed
    BRO_CTRL.create_backup(BACKUPS[4], AGENTS, NAMESPACE,
                           test_agent=False, expected_result="SUCCESS")


def test_restore_after_osmn_restarts_during_backup():
    """
    This test verifies a restore can be made after osmn restarts/recovers
    during a backup operation
    """
    test_case = "test_restore_after_osmn_restarts_during_backup"
    utilprocs.log("Test Case: {}".format(test_case))
    create_payload = {"backupName": BACKUPS[2]}
    out = BRO_CTRL.create_action("CREATE_BACKUP", create_payload,
                                 DEFAULT_SCOPE)
    action_id = out["id"]

    # Restart osmn pods
    BRO_CTRL.waits_until_action_reaches_progress_percentage(action_id,
                                                            "CREATE_BACKUP",
                                                            progress=0.30)
    for elem in OSMN_POD_LIST:
        KUBE.delete_pod(elem, NAMESPACE, wait_for_terminating=False)

    # sleep time to wait for osmn pods to restart
    time.sleep(120)
    for elem in OSMN_POD_LIST:
        KUBE.wait_for_pod_to_start(elem, NAMESPACE)

    # Due to OSMN being more robust since this test was created,
    # the backup action is now either a success or failure
    # after the OSMN pods have been restarted.
    # Test is changed to now wait for the action to be finished
    # and logs the result.
    # BRO_CTRL.wait_for_action_complete(action_id, "CREATE_BACKUP",
    #                                   BACKUPS[2], expected_result="FAILURE")
    # BRO_CTRL.wait_for_orchestrator_to_be_available()

    # verify if the previous create backup action is finished
    BRO_CTRL.wait_for_action_complete(action_id, "CREATE_BACKUP",
                                      BACKUPS[2], expected_result=None)
    BRO_CTRL.wait_for_orchestrator_to_be_available()

    # verify a successful restore can be performed
    BRO_CTRL.restore_backup(BACKUPS[4], AGENTS, NAMESPACE,
                            test_agent=False)


"""
These 2 test cases will be addressed in a later story

def test_backup_after_osmn_restarts_during_restore():
     """
"""
    This test verifies a backup can be made after osmn restarts/recovers
    during a restore operation
     """
"""
    test_case = "test_backup_after_osmn_restarts_during_restore"
    utilprocs.log("Test Case: {}".format(test_case))
    create_payload = {"backupName": BACKUPS[1]}
    out = BRO_CTRL.create_action("RESTORE", create_payload,
                                 DEFAULT_SCOPE)
    action_id = out["id"]

    # Restart osmn pods
    for elem in OSMN_POD_LIST:
        utilprocs.log("Delete {} pod".format(elem))
        KUBE.delete_pod(elem, NAMESPACE, wait_for_terminating=False)
    for elem in OSMN_POD_LIST:
        KUBE.wait_for_pod_to_start(elem, NAMESPACE)
    if BRO_CTRL.check_for_progress_percentage(action_id,
                                              "RESTORE",
                                              progress=0.30):
        for elem in OSMN_POD_LIST:
            utilprocs.log("Delete {} pod".format(elem))
            KUBE.delete_pod(elem, NAMESPACE, wait_for_terminating=False)
        for elem in OSMN_POD_LIST:
            KUBE.wait_for_pod_to_start(elem, NAMESPACE)

    # verify if the previous restore action has failed
    BRO_CTRL.wait_for_action_complete(action_id, "RESTORE",
                                      BACKUPS[1], expected_result="FAILURE")
    BRO_CTRL.wait_for_orchestrator_to_be_available()

    # sleep time to ensure osmn pods are healthy, needs to be investigated
    time.sleep(70)
    BRO_CTRL.create_backup(BACKUPS[4], AGENTS, NAMESPACE,
                           test_agent=False, expected_result="SUCCESS")


def test_restore_after_osmn_restarts_during_restore():
     """
"""
    This test verifies a restore can be made after osmn restarts/recovers
    during a restore operation
     """
"""
    test_case = "test_restore_after_osmn_restarts_during_restore"
    utilprocs.log("Test Case: {}".format(test_case))
    create_payload = {"backupName": BACKUPS[4]}
    out = BRO_CTRL.create_action("RESTORE", create_payload,
                                 DEFAULT_SCOPE)
    action_id = out["id"]

    # Restart osmn pods
    for elem in OSMN_POD_LIST:
        utilprocs.log("Delete {} pod".format(elem))
        KUBE.delete_pod(elem, NAMESPACE, wait_for_terminating=False)
    for elem in OSMN_POD_LIST:
        KUBE.wait_for_pod_to_start(elem, NAMESPACE)
    if BRO_CTRL.check_for_progress_percentage(action_id,
                                              "RESTORE",
                                              progress=0.30):
        for elem in OSMN_POD_LIST:
            utilprocs.log("Delete {} pod".format(elem))
            KUBE.delete_pod(elem, NAMESPACE, wait_for_terminating=False)
        for elem in OSMN_POD_LIST:
            KUBE.wait_for_pod_to_start(elem, NAMESPACE)

    # verify if the previous restore action has failed
    BRO_CTRL.wait_for_action_complete(action_id, "RESTORE",
                                      BACKUPS[4], expected_result="FAILURE")
    BRO_CTRL.wait_for_orchestrator_to_be_available()

    # sleep time to ensure osmn pods are healthy, needs to be investigated
    time.sleep(70)

    # verify a successful backup can be performed
    BRO_CTRL.restore_backup(BACKUPS[4], AGENTS, NAMESPACE,
                            test_agent=False)

 """


def test_backup_after_bro_restarts_during_backup():
    """
    This test verifies a backup can be made after BRO restarts/recovers
    during a backup operation
    """
    test_case = "test_backup_after_bro_restarts_during_backup"
    utilprocs.log("Test Case: {}".format(test_case))
    expected_backup = BACKUPS[1]
    assert not BRO_CTRL.backup_exists(expected_backup)
    # Update housekeeping config to set max-backups to 2
    # and auto-delete to enabled
    BRO_CTRL.update_housekeeping_config("enabled", "2",
                                        backup_manager_name=DEFAULT_SCOPE)
    BRO_CTRL.wait_for_orchestrator_to_be_available()

    create_payload = {"backupName": BACKUPS[0]}
    BRO_CTRL.create_action("CREATE_BACKUP", create_payload, DEFAULT_SCOPE)

    pod_name = bro_utils.get_pods_by_prefix(NAMESPACE, BRO_NAME)[0]
    KUBE.get_pod_logs(NAMESPACE, pod_name,
                      "pre_restart_{}".format(test_case))

    # Restart BRO pod and get new pod name
    utilprocs.log("Delete {} pod".format(pod_name))
    KUBE.shoot_pod(pod_name, NAMESPACE)
    time.sleep(5)
    pod_name = bro_utils.get_pods_by_prefix(NAMESPACE, BRO_NAME)[0]
    utilprocs.log("Waiting for {} to restart".format(pod_name))
    KUBE.wait_for_pod_to_start(pod_name, NAMESPACE)

    BRO_CTRL.wait_for_bro_agents_to_reconnect(
        [DDPG_AGENT["AgentId"], LG_AGENT_A["AgentId"], LG_AGENT_B["AgentId"]])

    BRO_CTRL.create_backup(BACKUPS[1], AGENTS, NAMESPACE, test_agent=False)


def test_restore_after_bro_restarts_during_backup():
    """
    This test verifies a restore can be made after BRO restarts/recovers
    during a backup operation
    """
    test_case = "test_restore_after_bro_restarts_during_backup"
    utilprocs.log("Test Case: {}".format(test_case))
    create_payload = {"backupName": BACKUPS[2]}
    BRO_CTRL.create_action("CREATE_BACKUP", create_payload, DEFAULT_SCOPE)

    pod_name = bro_utils.get_pods_by_prefix(NAMESPACE, BRO_NAME)[0]

    # Restart BRO pod and get new pod name
    utilprocs.log("Delete {} pod".format(pod_name))
    KUBE.shoot_pod(pod_name, NAMESPACE)
    time.sleep(5)
    pod_name = bro_utils.get_pods_by_prefix(NAMESPACE, BRO_NAME)[0]
    utilprocs.log("Waiting for {} to restart".format(pod_name))
    KUBE.wait_for_pod_to_start(pod_name, NAMESPACE)

    BRO_CTRL.wait_for_bro_agents_to_reconnect(
        [DDPG_AGENT["AgentId"], LG_AGENT_A["AgentId"], LG_AGENT_B["AgentId"]])

    BRO_CTRL.restore_backup(BACKUPS[1], AGENTS, NAMESPACE, test_agent=False)


def test_backup_after_bro_restarts_during_restore():
    """
    This test verifies a backup can be made after BRO restarts/recovers
    during a restore operation
    """
    test_case = "test_backup_after_bro_restarts_during_restore"
    utilprocs.log("Test Case: {}".format(test_case))
    expected_backup = BACKUPS[4]
    if BRO_CTRL.backup_exists(expected_backup):
        BRO_CTRL.delete_backup(expected_backup, NAMESPACE, verify_files=False)
    create_payload = {"backupName": BACKUPS[1]}
    BRO_CTRL.create_action("RESTORE", create_payload, DEFAULT_SCOPE)

    pod_name = bro_utils.get_pods_by_prefix(NAMESPACE, BRO_NAME)[0]

    # Collect console logs before restart
    KUBE.get_pod_logs(NAMESPACE, pod_name,
                      "pre_restart_{}".format(test_case))

    # Restart BRO pod and get new pod name
    utilprocs.log("Delete {} pod".format(pod_name))
    KUBE.shoot_pod(pod_name, NAMESPACE)
    time.sleep(5)
    pod_name = bro_utils.get_pods_by_prefix(NAMESPACE, BRO_NAME)[0]
    utilprocs.log("Waiting for {} to restart".format(pod_name))
    KUBE.wait_for_pod_to_start(pod_name, NAMESPACE)

    BRO_CTRL.wait_for_bro_agents_to_reconnect(
        [DDPG_AGENT["AgentId"], LG_AGENT_A["AgentId"], LG_AGENT_B["AgentId"]])

    BRO_CTRL.create_backup(BACKUPS[4], AGENTS, NAMESPACE, test_agent=False)


def test_restore_after_bro_restarts_during_restore():
    """
    This test verifies a restore can be made after BRO restarts/recovers
    during a restore operation
    """
    test_case = "test_restore_after_bro_restarts_during_restore"
    utilprocs.log("Test Case: {}".format(test_case))
    create_payload = {"backupName": BACKUPS[4]}
    BRO_CTRL.create_action("RESTORE", create_payload, DEFAULT_SCOPE)

    pod_name = bro_utils.get_pods_by_prefix(NAMESPACE, BRO_NAME)[0]

    # Collect console logs before restart
    KUBE.get_pod_logs(NAMESPACE, pod_name,
                      "pre_restart_{}".format(test_case))

    # Restart BRO pod and get new pod name
    utilprocs.log("Delete {} pod".format(pod_name))
    KUBE.shoot_pod(pod_name, NAMESPACE)
    time.sleep(5)
    pod_name = bro_utils.get_pods_by_prefix(NAMESPACE, BRO_NAME)[0]
    utilprocs.log("Waiting for {} to restart".format(pod_name))
    KUBE.wait_for_pod_to_start(pod_name, NAMESPACE)

    BRO_CTRL.wait_for_bro_agents_to_reconnect(
        [DDPG_AGENT["AgentId"], LG_AGENT_A["AgentId"], LG_AGENT_B["AgentId"]])

    BRO_CTRL.restore_backup(BACKUPS[4], AGENTS, NAMESPACE, test_agent=False)


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


def get_all_pod_logs(log_file_suffix):
    """
    Gets the pod logs for all agents and the Orchestrator

    :param log_file_suffix: A string to append to the name of the log files
    """
    pod_name = bro_utils.get_pods_by_prefix(NAMESPACE, BRO_NAME)[0]
    KUBE.get_pod_logs(NAMESPACE, pod_name, log_file_suffix)
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


def test_remove_k8s_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    utilprocs.log("Test Case: test_remove_k8s_resources")

    bro_utils.remove_namespace_resources(NAMESPACE)
    utilprocs.log("Finished removing Kubernetes resources")
