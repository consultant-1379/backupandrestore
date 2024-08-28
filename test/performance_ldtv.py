#!/usr/bin/env python3
"""
This module runs performance tests with
large data agent which is passed from
 jenkins parameter.
"""
import os
import bro_ctrl
import bro_sftp
import bro_utils
import helm3procs
import utilprocs
import bro_perf_utils
from bro_span_utils import Span

NAMESPACE = os.environ.get("kubernetes_namespace")
BRO_REPO = os.environ.get("helm_repo")
BRO_NAME = "eric-ctrl-bro"

BRO_REPO_NAME = "bro_drop"

# Set Test Service variables
TEST_CHART_NAME = "eric-test-service-basic"
TEST_REPO = os.environ.get("test_helm_repo")
TEST_REPO_NAME = "test_svc"
TEST_CHART_VERSION = ""

BRO_CTRL = bro_ctrl.BroCtrlClient()

SUBSCRIBER_SCOPE = "subscriber"

# SFTP Server USER and password
SFTP_USER = "brsftp"
SFTP_PASSWORD = "planbsftp"
SFTP_SERVER = bro_sftp.SFTPServer(SFTP_USER, SFTP_PASSWORD)

SUBS_BACKUP_LIST = ["sub_bu1"]
MAX_STORED_BACKUPS = 1

LG_AGENT_A = {"Large_AgentId": "agent-large-file"}

AGENT_NAMES = {SUBSCRIBER_SCOPE: []}
BACKUP_ACTION_IDS = {SUBSCRIBER_SCOPE: []}
EXPORT_ACTION_IDS = {SUBSCRIBER_SCOPE: []}
IMPORT_ACTION_IDS = {SUBSCRIBER_SCOPE: []}
RESTORE_ACTION_IDS = {SUBSCRIBER_SCOPE: []}


# Test Cases
def test_clear_any_failed_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    utilprocs.log("Test Case: test_clear_any_failed_resources")

    bro_utils.remove_namespace_resources(NAMESPACE)
    utilprocs.log("Finished removing Kubernetes resources")


def test_deploy_bro_service():
    """
    Deploys the Orchestrator Service.
    """
    utilprocs.log("Test Case: test_deploy_bro_service")

    utilprocs.log("Add the BRO Snapshot helm repo")
    helm3procs.add_helm_repo(BRO_REPO, BRO_REPO_NAME)
    bro_chart_version = \
        helm3procs.get_latest_chart_version(BRO_NAME,
                                            helm_repo_name=BRO_REPO_NAME,
                                            development_version=True)
    utilprocs.log("Deploy BRO version: {}".format(bro_chart_version))
    desc = "Deploy BRO"
    deploy_span = Span("deploy-bro", {"phase": "deploy", "description": desc,
                                      "labels": ["LCM"]})
    # Install Orchestrator Chart
    bro_utils.install_service_chart(BRO_REPO_NAME, BRO_NAME,
                                    bro_chart_version, NAMESPACE,
                                    agent_discovery=True,
                                    enable_global_tls=False,
                                    pvc_size="45Gi")
    utilprocs.log(deploy_span.finish())
    # Starts the sftp server, doesn't wait for the service to be up and running
    SFTP_SERVER.start_sftp_server()


def test_deploy_bro_agent():
    """
    Deploys a specified number of agents with a large data
    to verify their registration with an Orchestrator.
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

    agent_name = LG_AGENT_A['Large_AgentId']
    AGENT_NAMES[SUBSCRIBER_SCOPE].append(agent_name)
    # Deploy 1 large agents with subscriber scope
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION,
                                 [LG_AGENT_A["Large_AgentId"]],
                                 NAMESPACE, enable_global_tls=False,
                                 values_path=LG_AGENT_A.get(
                                     'Large_AgentId')+"_values.yaml.txt",
                                 generate_values_path=False,
                                 backup_type_list="{subscriber}")

    # Verify that Orchestrator has the expected agent/s registered
    desc = "Deploy test agents"
    register_span = \
        Span("agent-register-after-deploy-1x",
             {"phase": "deploy", "description": desc})
    BRO_CTRL.wait_for_bro_agents_to_reconnect(AGENT_NAMES[SUBSCRIBER_SCOPE])
    utilprocs.log(register_span.finish())


def test_create_single_agent_backups():
    """
    This test creates 1 backup of 30GB
    and stores the action IDs.
    """
    utilprocs.log("Test Case: test_create_single_agent_backups")
    auto_delete = "enabled"

    BRO_CTRL.update_housekeeping_config(auto_delete, MAX_STORED_BACKUPS,
                                        SUBSCRIBER_SCOPE)
    desc = "Create 1 backups, with 30GB agent/s"
    create_span = Span("create-backup-30gb",
                       {"phase": "traffic", "description": desc})
    action_ids = bro_perf_utils.create_backups(SUBS_BACKUP_LIST,
                                               SUBSCRIBER_SCOPE,
                                               BRO_CTRL)
    utilprocs.log(create_span.finish())
    BACKUP_ACTION_IDS[SUBSCRIBER_SCOPE].extend(action_ids)
    utilprocs.log("BACKUP ACTION IDs are {}".format(BACKUP_ACTION_IDS))


def test_export_single_agent_backups():
    """
    This test exports the content of one 30gb backup
    into an external SFTP server.
    """
    utilprocs.log("Test Case: test_export_single_agent_backups")

    # Waits until the sftp server is up and running
    SFTP_SERVER.wait_to_start()
    desc = "Export 1 backups of 30GB Agent"
    export_span = \
        Span("export-backup-30gb",
             {"phase": "traffic", "description": desc})
    action_ids = bro_perf_utils.export_backups(SUBS_BACKUP_LIST,
                                               SUBSCRIBER_SCOPE,
                                               BRO_CTRL, SFTP_SERVER)
    utilprocs.log(export_span.finish())
    EXPORT_ACTION_IDS[SUBSCRIBER_SCOPE].extend(action_ids)
    utilprocs.log("EXPORT ACTION IDs are {}".format(EXPORT_ACTION_IDS))


def test_import_single_agent_backups():
    """
    This test imports one 30gb backup
    from a sftp server.
    """
    utilprocs.log("Test Case: test_import_single_agent_backups")
    desc = "Import 1 backups of 30GB Agent"
    import_span = Span("import-backup-30gb",
                       {"phase": "traffic", "description": desc})
    action_ids = bro_perf_utils.import_backups(SUBS_BACKUP_LIST,
                                               SUBSCRIBER_SCOPE,
                                               NAMESPACE,
                                               BRO_CTRL, SFTP_SERVER)
    utilprocs.log(import_span.finish())
    IMPORT_ACTION_IDS[SUBSCRIBER_SCOPE].extend(action_ids)
    utilprocs.log("IMPORT ACTION IDs are {}".format(IMPORT_ACTION_IDS))


def test_restore_single_agent_backups():
    """
    This test restores one 30x1G backup
    and stores the action IDs.
    """
    utilprocs.log("Test Case: test_restore_single_agent_backups")
    desc = "Restores 1 backups, with 30GB agents"
    restore_span = Span("restore-backup-30gb",
                        {"phase": "traffic", "description": desc})
    action_ids = bro_perf_utils.restore_backups(SUBS_BACKUP_LIST,
                                                SUBSCRIBER_SCOPE,
                                                BRO_CTRL)
    utilprocs.log(restore_span.finish())
    RESTORE_ACTION_IDS[SUBSCRIBER_SCOPE].extend(action_ids)
    utilprocs.log("RESTORE ACTION IDs are {}".format(RESTORE_ACTION_IDS))


def test_calculate_action_times():
    """
    Calculate average time for actions
    """
    utilprocs.log("Test Case: test_calculate_action_times")

    avg_single_agent_action_times = get_all_average_times(
        SUBS_BACKUP_LIST, SUBSCRIBER_SCOPE)

    os.mkdir("plot")
    bro_perf_utils.log_action_times(avg_single_agent_action_times, 1,
                                    "30GB Agent RESULTS")

    bro_perf_utils.add_actions_times_file(avg_single_agent_action_times,
                                          1, "30GB-Agent-RESULTS")


def test_remove_k8s_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    utilprocs.log("Test Case: test_remove_k8s_resources")

    # stops sftp server
    SFTP_SERVER.stop_sftp_server()

    bro_utils.remove_namespace_resources(NAMESPACE)
    utilprocs.log("Finished removing Kubernetes resources")


def get_all_average_times(backup_list, scope):
    """
    Calculates the average times for the backup, export, import
    and restore operations
    :param backup_list: the list of backups
    :param scope: the scope of the agents
    :return action_times: a map of operation to average action time
    """
    action_times = {}
    action_times["CREATE_BACKUP"] = \
        bro_perf_utils.calculate_average_time("CREATE_BACKUP",
                                              backup_list,
                                              BACKUP_ACTION_IDS,
                                              scope)
    action_times["RESTORE"] = \
        bro_perf_utils.calculate_average_time("RESTORE",
                                              backup_list,
                                              RESTORE_ACTION_IDS,
                                              scope)
    action_times["EXPORT"] = \
        bro_perf_utils.calculate_average_time("EXPORT",
                                              backup_list,
                                              EXPORT_ACTION_IDS,
                                              scope)
    action_times["IMPORT"] = \
        bro_perf_utils.calculate_average_time("IMPORT",
                                              backup_list,
                                              IMPORT_ACTION_IDS,
                                              scope)
    return action_times
