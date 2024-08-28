#!/usr/bin/env python3
"""
This module runs daily performance tests with parallel actions enabled
"""
import os
import bro_ctrl
import bro_sftp
import bro_utils
import helm3procs
import utilprocs
import bro_perf_utils
from bro_span_utils import Span
import rest_utils

NAMESPACE = os.environ.get("kubernetes_namespace")
BRO_REPO = os.environ.get("helm_repo")
BRO_NAME = "eric-ctrl-bro"
BRO_POD_NAME = "eric-ctrl-bro-0"
BRO_REPO_NAME = "bro_drop"

BRO_PRA_REPO = "https://arm.sero.gic.ericsson.se/artifactory/" \
               "proj-adp-eric-ctrl-bro-released-helm"

BRO_PRA_REPO_NAME = "bro_pra"

BRO_HELM_RELEASE_NAME = bro_utils.get_service_helm_release_name(
    BRO_NAME, NAMESPACE)

# Set Test Service variables
TEST_CHART_NAME = "eric-test-service-basic"
TEST_REPO = os.environ.get("test_helm_repo")
TEST_REPO_NAME = "test_svc"
TEST_CHART_VERSION = ""

BRO_CTRL = bro_ctrl.BroCtrlClient()

BACKUP_LOCATION = "/bro/backups"

# Base REST URL and Scopes
BASE_URL = "http://eric-ctrl-bro:7001/v1/backup-manager"

# Configuration scope is used for 5x1G test cases
CONFIGURATION_SCOPE = "configuration-data"

# Subscriber scope is used for 1x1G test cases
SUBSCRIBER_SCOPE = "subscriber"

# Fragment scope is used for 1x1G test cases
FRAGMENT_SCOPE = "fragment"

# SFTP Server USER and password
SFTP_USER = "brsftp"
SFTP_PASSWORD = "planbsftp"
SFTP_SERVER = bro_sftp.SFTPServer(SFTP_USER, SFTP_PASSWORD)

HTTP_PATH = ""

CONFIG_BACKUP_LIST = ["con_bu1", "con_bu2"]
FRAGMENT_BACKUP_LIST = ["con_frag_bu1"]
SUBS_BACKUP_LIST = ["sub_bu1", "sub_bu2"]
MAX_STORED_BACKUPS = 6

AGENT_LARGE_FILE = {"AgentId": "agent-perf1gb",
                    "pod_prefix": "agent-perf1gb-agent",
                    "fragments": [{"fragmentId": "agent-perf1gb_1",
                                   "customData": False},
                                  {"fragmentId": "agent-perf1gb_2",
                                   "customData": False}]}

AGENT_FRAG = {"AgentId": "agent-fragment",
              "pod_prefix": "agent-fragment-agent"}

NUM_AGENTS = 5
AGENT_NAMES = {CONFIGURATION_SCOPE: [], SUBSCRIBER_SCOPE: [],
               FRAGMENT_SCOPE: []}
BACKUP_ACTION_IDS = {CONFIGURATION_SCOPE: [], SUBSCRIBER_SCOPE: [],
                     FRAGMENT_SCOPE: []}
EXPORT_ACTION_IDS = {CONFIGURATION_SCOPE: [], SUBSCRIBER_SCOPE: [],
                     FRAGMENT_SCOPE: []}
IMPORT_ACTION_IDS = {CONFIGURATION_SCOPE: [], SUBSCRIBER_SCOPE: [],
                     FRAGMENT_SCOPE: []}
RESTORE_ACTION_IDS = {CONFIGURATION_SCOPE: [], SUBSCRIBER_SCOPE: [],
                      FRAGMENT_SCOPE: []}

NUM_FRAGMENTS = 1000


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
                                    pvc_size="25Gi", parallel_actions=True)
    utilprocs.log(deploy_span.finish())
    # Starts the sftp server, doesn't wait for the service to be up and running
    SFTP_SERVER.start_sftp_server()


def test_delete_bro_pod():
    """
    Deletes the BRO pod and ensures it comes back up running.
    """
    test_case = "test_delete_bro_pod"
    utilprocs.log("Test Case: {}".format(test_case))

    desc = "Restart BRO pod"
    restart_span = Span("restart-bro",
                        {"phase": "restart",
                         "description": desc,
                         "labels": ["LCM"]})
    bro_utils.restart_pod(BRO_POD_NAME, NAMESPACE)
    utilprocs.log(restart_span.finish())


def test_rollback_via_helm_upgrade():
    """
    Performs a rollback using the helm upgrade command.
    """
    utilprocs.log("Test Case: test_rollback_via_helm_upgrade")

    utilprocs.log("Add the BRO PRA Repo")
    helm3procs.add_helm_repo(BRO_PRA_REPO, BRO_PRA_REPO_NAME)

    bro_pra_chart_version = \
        helm3procs.get_latest_chart_version(BRO_NAME,
                                            helm_repo_name=BRO_PRA_REPO_NAME,
                                            development_version=False)

    desc = "Rollback BRO to PRA"
    rollback_span = Span("rollback-bro",
                         {"phase": "rollback", "description": desc,
                          "labels": ["LCM"]})
    # Rollback via upgrade Orchestrator Chart
    helm3procs.helm_upgrade_with_chart_repo_with_dict(
        BRO_HELM_RELEASE_NAME,
        BRO_NAME,
        NAMESPACE,
        BRO_PRA_REPO_NAME,
        bro_pra_chart_version,
        settings_dict={"bro.enableLimitedParallelActions": "true",
                       "global.pullSecret": "armdocker",
                       "global.security.tls.enabled": "false",
                       "persistence.persistentVolumeClaim.size": "25Gi",
                       "bro.enableAgentDiscovery": "true",
                       "bro.vBRMAutoCreate": "DEFAULT"},
        timeout=120)

    utilprocs.log(rollback_span.finish())


def test_upgrade_from_pra_to_latest_snap():
    """
    Performs an upgrade from PRA to latest snap.
    """
    utilprocs.log("Test Case: test_upgrade_from_pra_to_latest_snap")

    utilprocs.log("Add the BRO Snapshot helm repo")
    helm3procs.add_helm_repo(BRO_REPO, BRO_REPO_NAME)
    bro_chart_version = \
        helm3procs.get_latest_chart_version(BRO_NAME,
                                            helm_repo_name=BRO_REPO_NAME,
                                            development_version=True)

    utilprocs.log("Upgrade to BRO version: {}".format(bro_chart_version))
    desc = "Upgrade BRO from PRA to latest snap"
    upgrade_span = Span("upgrade-bro",
                        {"phase": "upgrade", "description": desc,
                         "labels": ["LCM"]})
    # Upgrade Orchestrator Chart
    helm3procs.helm_upgrade_with_chart_repo_with_dict(
        BRO_HELM_RELEASE_NAME,
        BRO_NAME,
        NAMESPACE,
        BRO_REPO_NAME,
        bro_chart_version,
        settings_dict={"bro.enableLimitedParallelActions": "true",
                       "global.pullSecret": "armdocker",
                       "global.security.tls.enabled": "false",
                       "persistence.persistentVolumeClaim.size": "25Gi",
                       "bro.enableAgentDiscovery": "true",
                       "bro.vBRMAutoCreate": "DEFAULT"},
        timeout=120)

    utilprocs.log(upgrade_span.finish())


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
    for count in range(NUM_AGENTS):
        agent_name = AGENT_LARGE_FILE['AgentId'] + "-" + str(count)
        AGENT_NAMES[CONFIGURATION_SCOPE].append(agent_name)
    # Add the last agent of configuration scope to subscriber scope agent names
    AGENT_NAMES[SUBSCRIBER_SCOPE].append(AGENT_NAMES[CONFIGURATION_SCOPE][4])

    # Deploy 4 agents with configuration-data scope
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION,
                                 AGENT_NAMES[CONFIGURATION_SCOPE][0:4],
                                 NAMESPACE, enable_global_tls=False,
                                 generate_values_path=False,
                                 values_path=AGENT_LARGE_FILE.get(
                                     'AgentId')+"_values.yaml.txt",
                                 backup_type_list="{configuration-data}")

    # Deploy 1 agent with configuration-data and subscriber scope
    last_agent_scope = "{configuration-data,subscriber}"
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION,
                                 AGENT_NAMES[SUBSCRIBER_SCOPE],
                                 NAMESPACE, enable_global_tls=False,
                                 generate_values_path=False,
                                 values_path=AGENT_LARGE_FILE.get(
                                     'AgentId')+"_values.yaml.txt",
                                 backup_type_list=last_agent_scope)
    multifrag_agent_name = AGENT_FRAG['AgentId']
    AGENT_NAMES[CONFIGURATION_SCOPE].append(multifrag_agent_name)
    AGENT_NAMES[FRAGMENT_SCOPE].append(multifrag_agent_name)
    # Deploy 1 agent with multi fragment scope
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION,
                                 AGENT_NAMES[FRAGMENT_SCOPE],
                                 NAMESPACE, enable_global_tls=False,
                                 generate_values_path=False,
                                 values_path="{}_values.yaml.txt".
                                 format(multifrag_agent_name),
                                 backup_type_list="{fragment}")

    # Verify that Orchestrator has the expected agent/s registered
    desc = "Deploy 6 test agents"
    register_span = \
        Span("agent-register-after-deploy-5x",
             {"phase": "deploy", "description": desc})
    BRO_CTRL.wait_for_bro_agents_to_reconnect(AGENT_NAMES[CONFIGURATION_SCOPE])
    utilprocs.log(register_span.finish())


def test_parallel_create_and_export_5x1g_backup():
    """
    This test exports a backup to an sftp sever
    and creates a backup in parallel.
    """
    utilprocs.log("Test Case: test_parallel_create_and_export_5x1g_backup")

    # Setup for Export
    auto_delete = "enabled"
    BRO_CTRL.update_housekeeping_config(auto_delete,
                                        MAX_STORED_BACKUPS,
                                        CONFIGURATION_SCOPE)

    bro_perf_utils.create_backups([CONFIG_BACKUP_LIST[0]],
                                  CONFIGURATION_SCOPE,
                                  BRO_CTRL)

    SFTP_SERVER.wait_to_start()

    # Export Backup
    desc = "Export a backup of 5GB"
    export_span = \
        Span("export-backup-5gb",
             {"phase": "traffic", "description": desc})
    export_id = bro_perf_utils.export_backups([CONFIG_BACKUP_LIST[0]],
                                              CONFIGURATION_SCOPE,
                                              BRO_CTRL, SFTP_SERVER,
                                              wait_for_action_complete=False)

    # Create Backup in parallel to Export
    desc = "Create a backup in parallel with export, with 5 x 1GB agent/s"
    create_span = Span("create-backup-5x-1gb",
                       {"phase": "traffic", "description": desc})
    backup_id = bro_perf_utils.create_backups([CONFIG_BACKUP_LIST[1]],
                                              CONFIGURATION_SCOPE,
                                              BRO_CTRL,
                                              wait_for_action_complete=False)

    wait_for_parallel_actions_to_complete(CONFIGURATION_SCOPE,
                                          export_id[0], backup_id[0],
                                          export_span, create_span)

    EXPORT_ACTION_IDS[CONFIGURATION_SCOPE].extend(export_id)
    utilprocs.log("EXPORT ACTION ID is {}".format(EXPORT_ACTION_IDS))
    BACKUP_ACTION_IDS[CONFIGURATION_SCOPE].extend(backup_id)
    utilprocs.log("BACKUP ACTION ID is {}".format(BACKUP_ACTION_IDS))

    backups = BRO_CTRL.get_backups(scope=CONFIGURATION_SCOPE)
    backup_names = [elem.get("name") for elem in backups]
    assert CONFIG_BACKUP_LIST[0] in backup_names,\
        "Backup {} does not exist  ".format(CONFIG_BACKUP_LIST[0])
    assert CONFIG_BACKUP_LIST[1] in backup_names,\
        "Backup {} does not exist  ".format(CONFIG_BACKUP_LIST[1])


def test_parallel_import_and_restore_5gb_backups():
    """
    This test imports a 5gb backup from a sftp server and
    runs a restore in parallel
    """
    utilprocs.log("Test Case: test_parallel_import_and_restore_5gb_backups")
    desc = "Import a backup, of 5GB"
    import_span = Span("import-backup-5gb",
                       {"phase": "traffic", "description": desc})

    import_id = bro_perf_utils.import_backups([CONFIG_BACKUP_LIST[0]],
                                              CONFIGURATION_SCOPE, NAMESPACE,
                                              BRO_CTRL, SFTP_SERVER,
                                              wait_for_action_complete=False)

    desc = "Restores a backup, with 5 x 1GB agents"
    restore_span = Span("restore-backup-5x1gb",
                        {"phase": "traffic", "description": desc})

    restore_id = bro_perf_utils.restore_backups([CONFIG_BACKUP_LIST[1]],
                                                CONFIGURATION_SCOPE,
                                                BRO_CTRL,
                                                wait_for_action_complete=False)

    wait_for_parallel_actions_to_complete(CONFIGURATION_SCOPE,
                                          import_id[0],
                                          restore_id[0],
                                          import_span, restore_span)

    IMPORT_ACTION_IDS[CONFIGURATION_SCOPE].extend(import_id)
    utilprocs.log("IMPORT ACTION ID is {}".format(IMPORT_ACTION_IDS))
    RESTORE_ACTION_IDS[CONFIGURATION_SCOPE].extend(restore_id)
    utilprocs.log("RESTORE ACTION ID is {}".format(RESTORE_ACTION_IDS))

    backups = BRO_CTRL.get_backups(scope=CONFIGURATION_SCOPE)
    backup_names = [elem.get("name") for elem in backups]
    assert CONFIG_BACKUP_LIST[0] in backup_names,\
        "Backup {} does not exist  ".format(CONFIG_BACKUP_LIST[0])
    assert CONFIG_BACKUP_LIST[1] in backup_names,\
        "Backup {} does not exist  ".format(CONFIG_BACKUP_LIST[1])


def test_parallel_create_and_export_1x1g_backup():
    """
    This test exports a backup with 1x1G agents
    and creates a backup in parallel.
    """
    utilprocs.log("Test Case: test_parallel_create_and_export_1x1g_backup")

    # Setup for Export
    auto_delete = "enabled"
    BRO_CTRL.update_housekeeping_config(auto_delete, MAX_STORED_BACKUPS,
                                        SUBSCRIBER_SCOPE)
    bro_perf_utils.create_backups([SUBS_BACKUP_LIST[0]],
                                  SUBSCRIBER_SCOPE,
                                  BRO_CTRL)

    # Export
    desc = "Export a backup of 1GB"
    export_span = \
        Span("export-backup-1gb",
             {"phase": "traffic", "description": desc})
    export_id = bro_perf_utils.export_backups([SUBS_BACKUP_LIST[0]],
                                              SUBSCRIBER_SCOPE,
                                              BRO_CTRL, SFTP_SERVER,
                                              wait_for_action_complete=False)

    # Create backup in parallel to export
    desc = "Create a backup, with 1 x 1GB agent"
    create_span = \
        Span("create-backup-1x-1gb",
             {"phase": "traffic", "description": desc})
    create_id = bro_perf_utils.create_backups([SUBS_BACKUP_LIST[1]],
                                              SUBSCRIBER_SCOPE,
                                              BRO_CTRL,
                                              wait_for_action_complete=False)

    wait_for_parallel_actions_to_complete(SUBSCRIBER_SCOPE,
                                          export_id[0],
                                          create_id[0],
                                          export_span, create_span)

    EXPORT_ACTION_IDS[SUBSCRIBER_SCOPE].extend(export_id)
    utilprocs.log("EXPORT ACTION IDs are {}".format(EXPORT_ACTION_IDS))
    BACKUP_ACTION_IDS[SUBSCRIBER_SCOPE].extend(create_id)
    utilprocs.log("BACKUP ACTION IDs are {}".format(BACKUP_ACTION_IDS))

    backups = BRO_CTRL.get_backups(scope=SUBSCRIBER_SCOPE)
    backup_names = [elem.get("name") for elem in backups]
    assert SUBS_BACKUP_LIST[0] in backup_names,\
        "Backup {} does not exist  ".format(SUBS_BACKUP_LIST[0])
    assert SUBS_BACKUP_LIST[1] in backup_names,\
        "Backup {} does not exist  ".format(SUBS_BACKUP_LIST[1])


def test_parallel_import_and_restore_1gb_backups():
    """
    This test imports a 1gb backup from a sftp server
    and runs a restore in parallel
    """
    utilprocs.log("Test Case: test_parallel_import_and_restore_1gb_backups")
    desc = "Import a backup of 1GB"
    import_span = Span("import-backup-1gb",
                       {"phase": "traffic", "description": desc})
    import_id = bro_perf_utils.import_backups([SUBS_BACKUP_LIST[0]],
                                              SUBSCRIBER_SCOPE, NAMESPACE,
                                              BRO_CTRL, SFTP_SERVER,
                                              wait_for_action_complete=False)

    desc = "Restores a backup, with 1 x 1GB agents"
    restore_span = Span("restore-backup-1x1gb",
                        {"phase": "traffic", "description": desc})
    restore_id = bro_perf_utils.restore_backups([SUBS_BACKUP_LIST[1]],
                                                SUBSCRIBER_SCOPE,
                                                BRO_CTRL,
                                                wait_for_action_complete=False)

    wait_for_parallel_actions_to_complete(SUBSCRIBER_SCOPE,
                                          import_id[0],
                                          restore_id[0],
                                          import_span, restore_span)

    IMPORT_ACTION_IDS[SUBSCRIBER_SCOPE].extend(import_id)
    utilprocs.log("IMPORT ACTION IDs are {}".format(IMPORT_ACTION_IDS))
    RESTORE_ACTION_IDS[SUBSCRIBER_SCOPE].extend(restore_id)
    utilprocs.log("RESTORE ACTION IDs are {}".format(RESTORE_ACTION_IDS))

    backups = BRO_CTRL.get_backups(scope=SUBSCRIBER_SCOPE)
    backup_names = [elem.get("name") for elem in backups]
    assert SUBS_BACKUP_LIST[0] in backup_names,\
        "Backup {} does not exist  ".format(SUBS_BACKUP_LIST[0])
    assert SUBS_BACKUP_LIST[1] in backup_names,\
        "Backup {} does not exist  ".format(SUBS_BACKUP_LIST[1])


def test_create_1000_fragments_backup():
    """
    This test creates 1 backup with 1000 fragments
    and stores the action IDs.
    """
    utilprocs.log("Test Case: test_create_1000_fragments_backup")
    desc = "Create {} backups, with {} fragments" \
        .format(len(FRAGMENT_BACKUP_LIST), NUM_FRAGMENTS)
    create_span = Span("create-backup-{}-fragments"
                       .format(NUM_FRAGMENTS),
                       {"phase": "traffic", "description": desc})
    action_ids = bro_perf_utils.create_backups(FRAGMENT_BACKUP_LIST,
                                               FRAGMENT_SCOPE,
                                               BRO_CTRL)
    utilprocs.log(create_span.finish())
    BACKUP_ACTION_IDS[FRAGMENT_SCOPE].extend(action_ids)
    utilprocs.log("BACKUP ACTION IDs are {}".format(BACKUP_ACTION_IDS))


def test_restore_1000_fragments_backup():
    """
    This test restores 1 backup with 1000 fragments
    and stores the action IDs.
    """
    utilprocs.log("Test Case: test_restore_1000_fragments_backup")
    desc = "Restores {} backup, with {} fragments" \
        .format(len(FRAGMENT_BACKUP_LIST), NUM_FRAGMENTS)
    restore_span = Span("restore-backup-{}-fragments"
                        .format(NUM_FRAGMENTS),
                        {"phase": "traffic", "description": desc})
    action_ids = bro_perf_utils.restore_backups(FRAGMENT_BACKUP_LIST,
                                                FRAGMENT_SCOPE,
                                                BRO_CTRL)
    utilprocs.log(restore_span.finish())
    RESTORE_ACTION_IDS[FRAGMENT_SCOPE].extend(action_ids)
    utilprocs.log("RESTORE ACTION IDs are {}".format(RESTORE_ACTION_IDS))


def test_calculate_action_times():
    """
    Calculate average time for actions
    """
    utilprocs.log("Test Case: test_calculate_action_times")

    average_5x1g_action_times = get_all_average_times(CONFIG_BACKUP_LIST,
                                                      CONFIGURATION_SCOPE)
    average_1x1g_action_times = get_all_average_times(SUBS_BACKUP_LIST,
                                                      SUBSCRIBER_SCOPE)
    average_fragment_times = get_all_average_times(FRAGMENT_BACKUP_LIST,
                                                   FRAGMENT_SCOPE)

    os.mkdir("plot")
    bro_perf_utils.log_action_times(average_5x1g_action_times, 5)
    bro_perf_utils.log_action_times(average_1x1g_action_times, 1)
    bro_perf_utils.log_action_times(average_fragment_times, 1,
                                    "1000 fragments results")

    bro_perf_utils.add_actions_times_file(average_5x1g_action_times, 5)
    bro_perf_utils.add_actions_times_file(average_1x1g_action_times, 1)
    bro_perf_utils.add_actions_times_file(average_fragment_times, 1,
                                          "1000-fragments-results")


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


def wait_for_parallel_actions_to_complete(scope, action_one,
                                          action_two, span_one,
                                          span_two):
    """
    Waits for both actions to be a success
    Once an action is a success the span is closed
    :param scope: the scope to be used for checking the action
    :param action_one: the action to be checked
    :param action_two: the action to be checked
    :param span_one: the span to be closed
    :param span_two: the span to be closed
    """
    action_url = BRO_CTRL.get_action_url(scope=scope)
    first_action = "{}/{}".format(action_url, action_one)
    first_result = None
    second_action = "{}/{}".format(action_url, action_two)
    second_result = None

    while not (first_result and second_result):

        first_status = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                               "Get action progress",
                                               first_action,
                                               ca_cert=BRO_CTRL.ca_cert_path)

        if first_status["result"] == "SUCCESS" and first_result is None:
            first_result = first_status["result"]
            utilprocs.log(span_one.finish())

        if first_status["result"] == "FAILURE":
            raise Exception("Action {} has failed".format(action_one))

        second_status = rest_utils.rest_request(rest_utils.REQUESTS.get,
                                                "Get action progress",
                                                second_action,
                                                ca_cert=BRO_CTRL.ca_cert_path)

        if second_status["result"] == "SUCCESS" and second_result is None:
            second_result = second_status["result"]
            utilprocs.log(span_two.finish())

        if second_status["result"] == "FAILURE":
            raise Exception("Action {} has failed".format(action_two))
